package com.inceptai.dobby.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.eventbus.DobbyEvent;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.model.DobbyWifiInfo;
import com.inceptai.dobby.utils.DobbyLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Class contains logic for scanning, computing contention and any other wifi-related diagnostics.
 */

public class WifiAnalyzer {

    private static final int WIFI_RECEIVER_UNREGISTERED = 0;
    private static final int WIFI_RECEIVER_REGISTERED = 1;
    private static final boolean TRIGGER_WIFI_SCAN_ON_RSSI_CHANGE = false;
    private static final int MIN_WIFI_SCANS_NEEDED = 1;

    // Store application context to prevent leaks and crashes from an activity going out of scope.
    protected Context context;
    protected WifiReceiver wifiScanReceiver = new WifiReceiver();
    protected WifiReceiver wifiStateReceiver = new WifiReceiver();
    protected int wifiReceiverState = WIFI_RECEIVER_UNREGISTERED;
    protected WifiManager wifiManager;
    protected DobbyThreadpool threadpool;
    protected WifiState wifiState;
    protected boolean wifiConnected;
    private boolean publishedWifiState;
    protected boolean wifiEnabled;
    protected SettableFuture<List<ScanResult>> wifiScanFuture;
    protected DobbyEventBus eventBus;
    protected List<ScanResult> combinedScanResult;
    protected long lastScanCompletionTimestampMs;


    protected WifiAnalyzer(Context context, WifiManager wifiManager, DobbyThreadpool threadpool, DobbyEventBus eventBus) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(wifiManager);
        this.context = context.getApplicationContext();
        this.wifiManager = wifiManager;
        this.threadpool = threadpool;
        this.eventBus = eventBus;
        wifiConnected = false;
        publishedWifiState = false;
        wifiEnabled = false;
        wifiState = new WifiState();
        combinedScanResult = new ArrayList<>();
        lastScanCompletionTimestampMs = 0;
        initializeWifiState();
    }

    /**
     * Factory constructor to create an instance
     *
     * @param context Application context.
     * @return Instance of WifiAnalyzer or null on error.
     */
    @Nullable
    public static WifiAnalyzer create(Context context, DobbyThreadpool threadpool, DobbyEventBus eventBus) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            return new WifiAnalyzer(context.getApplicationContext(), wifiManager, threadpool, eventBus);
        }
        return null;
    }

    private void initializeWifiState() {
        Preconditions.checkNotNull(wifiManager);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        //Switch thread and do this.
        wifiState.updateWifiStats(new DobbyWifiInfo(wifiInfo), null);
        registerWifiStateReceiver();

        //Publish detailed connection state and wifi state on the bus
        updateWifiStatsDetailedState(WifiInfo.getDetailedStateOf(wifiInfo.getSupplicantState()));
        processWifiStateChanged(wifiManager.getWifiState());
    }

    public void clearWifiScanCache() {
        combinedScanResult.clear();
        lastScanCompletionTimestampMs = 0;
    }

    public List<ScanResult> getLatestWifiScan() {
        return combinedScanResult;
    }

    /**
     * @return An instance of a {@link ListenableFuture<List<ScanResult>>} or null on immediate failure.
     */
    public ListenableFuture<List<ScanResult>> startWifiScan(int maxAgeForRetriggeringScan) {
        if (wifiScanFuture != null && !wifiScanFuture.isDone()) {
            //WifiScan in progress, just return the current future.
            return wifiScanFuture;
        }
        //Check if the last scan result is fresh enough
        if (System.currentTimeMillis() - lastScanCompletionTimestampMs > maxAgeForRetriggeringScan) {
            //Clearing out previous scan results
            combinedScanResult.clear();

            if (wifiReceiverState != WIFI_RECEIVER_REGISTERED) {
                registerScanReceiver();
            }
            if (wifiManager.startScan()) {
                wifiScanFuture = SettableFuture.create();
                eventBus.postEvent(DobbyEvent.EventType.WIFI_SCAN_STARTING);
                return wifiScanFuture;
            }
        } else {
            //Return the last result
            wifiScanFuture = SettableFuture.create();
            setWifiScanFuture(combinedScanResult);
            return wifiScanFuture;
        }
        return null;
    }

    public DhcpInfo getDhcpInfo() {
        return wifiManager.getDhcpInfo();
    }

    public List<WifiConfiguration> getWifiConfiguration() {
        return wifiManager.getConfiguredNetworks();
    }

    public WifiState getWifiState() {
        return wifiState;
    }

    public DobbyWifiInfo getLinkInfo() { return wifiState.getLinkInfo(); }

    public HashMap<Integer, WifiState.ChannelInfo> getChannelStats() {
        return wifiState.getChannelInfoMap();
    }

    // Called in order to cleanup any held resources.
    public void cleanup() {
        unregisterScanReceiver();
        unregisterWifiStateReceiver();
    }

    private void registerScanReceiver() {
        context.registerReceiver(wifiScanReceiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiReceiverState = WIFI_RECEIVER_REGISTERED;
    }

    private void unregisterScanReceiver() {
        try {
            context.unregisterReceiver(wifiScanReceiver);
        } catch (IllegalArgumentException e) {
            DobbyLog.v("Exception while unregistering wifi receiver: " + e);
        }
        wifiReceiverState = WIFI_RECEIVER_UNREGISTERED;
    }

    private class WifiReceiver extends BroadcastReceiver {
        private int currentScans = 0;

        public WifiReceiver() {
        }

        private void resetCurrentScans() {
            currentScans = 0;
        }

        synchronized public void onScanReceive() {
            currentScans++;
            final boolean doScanAgain = (currentScans < MIN_WIFI_SCANS_NEEDED);
            threadpool.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        List<ScanResult> wifiList = wifiManager.getScanResults();
                        combinedScanResult.addAll(wifiList);
                        printScanResults(wifiList);
                    } catch (SecurityException e) {
                        DobbyLog.e("Security exception while getting scan results: " + e);
                        setWifiScanFuture(combinedScanResult);
                        unregisterScanReceiver();
                        resetCurrentScans();
                        return;
                    }
                    //We got some results back from getScanResults -- so presumably we have location
                    if (doScanAgain && wifiManager.startScan()) {
                        DobbyLog.v("Starting Wifi Scan again, currentScans: " + currentScans);
                    } else {
                        updateWifiScanResults();
                        DobbyLog.v("Un-registering Scan Receiver");
                        unregisterScanReceiver();
                        resetCurrentScans();
                    }
                }
            });
        }

        public void onWifiStateChange(Intent intent) {
            final Intent intentToProcess = intent;
            threadpool.submit(new Runnable() {
                @Override
                public void run() {
                    processWifiStateRelatedIntents(intentToProcess);
                }
            });
        }


        @Override
        public void onReceive(Context c, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                onScanReceive();
            } else if (action.equals(WifiManager.RSSI_CHANGED_ACTION)
                    || action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)
                    || action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                onWifiStateChange(intent);
            }
        }
    }

    private void printScanResults(List<ScanResult> scanResultList) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < scanResultList.size(); i++) {
            sb.append(new Integer(i + 1).toString() + ".");
            sb.append((scanResultList.get(i)).toString());
            sb.append("\\n");
        }
        DobbyLog.i("Wifi scan result: " + sb.toString());
    }

    private void setWifiScanFuture(List<ScanResult> scanResultList) {
        if (wifiScanFuture != null) {
            boolean setResult = wifiScanFuture.set(scanResultList);
            DobbyLog.v("Setting wifi scan future: return value: " + setResult);
        }
    }

    private void updateWifiScanResults() {
        StringBuilder sb = new StringBuilder();
        wifiState.updateWifiStats(null, combinedScanResult);
        setWifiScanFuture(combinedScanResult);
        lastScanCompletionTimestampMs = System.currentTimeMillis();
        /*
        if (wifiList.size() == 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                DobbyLog.i("isScanAlwaysAvailable: " + String.valueOf(wifiManager.isScanAlwaysAvailable()));
                DobbyLog.i("WifiInfo:" + wifiManager.getConnectionInfo().toString());
                DobbyLog.i("WifiState:" + wifiManager.getWifiState());
                wifiManager.reconnect();
                wifiManager.startScan();
            }
        }
        */
    }

    private void registerWifiStateReceiver() {
        IntentFilter wifiStateIntentFilter = new IntentFilter();
        wifiStateIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        wifiStateIntentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        wifiStateIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        context.registerReceiver(wifiStateReceiver, wifiStateIntentFilter);
    }

    private void unregisterWifiStateReceiver() {
        try {
            context.unregisterReceiver(wifiStateReceiver);
        } catch (IllegalArgumentException e) {
            DobbyLog.v("Exception while unregistering wifi state receiver: " + e);
        }
    }

    protected void updateWifiStatsWithWifiInfo(WifiInfo info) {
        if (info == null) {
            info = wifiManager.getConnectionInfo();
        }
        //Convert to DobbyWifiInfo
        DobbyWifiInfo dobbyWifiInfo = new DobbyWifiInfo(info);;
        wifiState.updateWifiStats(dobbyWifiInfo, null);
    }

    @DobbyEvent.EventType
    protected int convertWifiStateProblemToDobbyEventType(@WifiState.WifiLinkMode int problemMode) {
        @DobbyEvent.EventType int eventTypeToBroadcast;
        switch (problemMode) {
            case WifiState.WifiLinkMode.HANGING_ON_DHCP:
                eventTypeToBroadcast = DobbyEvent.EventType.HANGING_ON_DHCP;
                break;
            case WifiState.WifiLinkMode.HANGING_ON_AUTHENTICATING:
                eventTypeToBroadcast = DobbyEvent.EventType.HANGING_ON_AUTHENTICATING;
                break;
            case WifiState.WifiLinkMode.HANGING_ON_SCANNING:
                eventTypeToBroadcast = DobbyEvent.EventType.HANGING_ON_SCANNING;
                break;
            case WifiState.WifiLinkMode.FREQUENT_DISCONNECTIONS:
                eventTypeToBroadcast = DobbyEvent.EventType.FREQUENT_DISCONNECTIONS;
                break;
            default:
                eventTypeToBroadcast = DobbyEvent.EventType.WIFI_STATE_UNKNOWN;
        }
        return eventTypeToBroadcast;
    }

    protected void updateWifiStatsDetailedState(NetworkInfo.DetailedState detailedState) {
        @WifiState.WifiLinkMode int oldProblemMode = wifiState.getWifiProblemMode();
        @WifiState.WifiLinkMode int newProblemMode = wifiState.updateDetailedWifiStateInfo(detailedState, System.currentTimeMillis());
        if (oldProblemMode != newProblemMode) {
            @DobbyEvent.EventType int eventTypeToBroadcast = convertWifiStateProblemToDobbyEventType(newProblemMode);
            if (eventTypeToBroadcast != DobbyEvent.EventType.WIFI_STATE_UNKNOWN) {
                eventBus.postEvent(new DobbyEvent(eventTypeToBroadcast));
            }
        }
        //Utils.PercentileStats stats = wifiState.getStatsForDetailedState(detailedState, GAP_FOR_GETTING_DETAILED_NETWORK_STATE_STATS_MS);
        //DobbyLog.v("updateDetailedWifiStateInfo State: " + detailedState.name() + " stats: " + stats.toString());
    }

    private void processNetworkStateChangedIntent(Intent intent) {
        final NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        if (networkInfo != null) {
            NetworkInfo.DetailedState detailedWifiState = networkInfo.getDetailedState();
            updateWifiStatsDetailedState(detailedWifiState);
        }
        boolean wasConnected = wifiConnected;
        wifiConnected = networkInfo != null && networkInfo.isConnected();
        //If no longer connected, clear the connection info
        if (!wifiConnected) {
            wifiState.clearWifiConnectionInfo();
        }
        // If we just connected, grab the initial signal strength and SSID
        if (wifiConnected && !wasConnected) {
            postToEventBus(DobbyEvent.EventType.WIFI_CONNECTED);
            // try getting it out of the intent first
            WifiInfo info = (WifiInfo) intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
            updateWifiStatsWithWifiInfo(info);

            DhcpInfo dhcpInfo = getDhcpInfo();
            if (dhcpInfo != null && dhcpInfo.ipAddress != 0) {
                eventBus.postEvent(new DobbyEvent(DobbyEvent.EventType.DHCP_INFO_AVAILABLE));
            }
        } else if (!wifiConnected && wasConnected) {
            postToEventBus(DobbyEvent.EventType.WIFI_NOT_CONNECTED);
        } else {
            if (wifiConnected) {
                if (!publishedWifiState) {
                    postToEventBus(DobbyEvent.EventType.WIFI_CONNECTED);
                }
                DobbyLog.v("No change in wifi state -- we were connected and are connected");
            } else {
                //So that we publish this event at least once
                if (!publishedWifiState) {
                    postToEventBus(DobbyEvent.EventType.WIFI_NOT_CONNECTED);
                }
                DobbyLog.v("No change in wifi state -- we were NOT connected and are still NOT connected");
            }
        }
    }

    private void postToEventBus(@DobbyEvent.EventType int eventType) {
        eventBus.postEvent(new DobbyEvent(eventType));
        publishedWifiState = true;
    }

    private void processWifiStateChangedIntent(Intent intent) {
        int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
        processWifiStateChanged(wifiState);
    }

    private void processWifiStateChanged(int wifiState) {
        @DobbyEvent.EventType int eventToBroadcast = DobbyEvent.EventType.NO_EVENT_RECEIVED;
        switch (wifiState) {
            case WifiManager.WIFI_STATE_ENABLED:
                eventToBroadcast = DobbyEvent.EventType.WIFI_STATE_ENABLED;
                wifiEnabled = true;
                break;
            case WifiManager.WIFI_STATE_DISABLED:
                eventToBroadcast = DobbyEvent.EventType.WIFI_STATE_DISABLED;
                wifiEnabled = false;
                break;
            case WifiManager.WIFI_STATE_ENABLING:
                eventToBroadcast = DobbyEvent.EventType.WIFI_STATE_ENABLING;
                wifiEnabled = false;
                break;
            case WifiManager.WIFI_STATE_DISABLING:
                eventToBroadcast = DobbyEvent.EventType.WIFI_STATE_DISABLING;
                wifiEnabled = false;
                break;
            case WifiManager.WIFI_STATE_UNKNOWN:
                eventToBroadcast = DobbyEvent.EventType.WIFI_STATE_UNKNOWN;
                wifiEnabled = false;
                break;
        }
        if (eventToBroadcast != DobbyEvent.EventType.NO_EVENT_RECEIVED) {
            eventBus.postEvent(new DobbyEvent(eventToBroadcast));
        }
    }


    //Listening for WiFi intents
    synchronized private void processWifiStateRelatedIntents(Intent intent) {
        final String action = intent.getAction();
        int wifiState;
        switch (action) {
            case WifiManager.WIFI_STATE_CHANGED_ACTION:
                processWifiStateChangedIntent(intent);
                break;
            case WifiManager.NETWORK_STATE_CHANGED_ACTION:
                processNetworkStateChangedIntent(intent);
                break;
            case WifiManager.RSSI_CHANGED_ACTION:
                eventBus.postEvent(new DobbyEvent(DobbyEvent.EventType.WIFI_RSSI_CHANGED));
                int updatedSignal = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, 0);
                if (TRIGGER_WIFI_SCAN_ON_RSSI_CHANGE && this.wifiState.updateSignal(updatedSignal)) {
                    //Reissue wifi scan to correctly compute contention since the signal has changed significantly
                    //Force a wifi scan
                    startWifiScan(0);
                }
                break;
        }
    }

}
