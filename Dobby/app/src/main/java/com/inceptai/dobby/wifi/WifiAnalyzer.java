package com.inceptai.dobby.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.eventbus.DobbyEvent;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.model.DobbyWifiInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * Class contains logic for scanning, computing contention and any other wifi-related diagnostics.
 */

public class WifiAnalyzer {

    private static final int WIFI_RECEIVER_UNREGISTERED = 0;
    private static final int WIFI_RECEIVER_REGISTERED = 1;
    private static final boolean TRIGGER_WIFI_SCAN_ON_RSSI_CHANGE = false;
    private static final int MIN_WIFI_SCANS_NEEDED = 3;

    // Store application context to prevent leaks and crashes from an activity going out of scope.
    protected Context context;
    protected WifiReceiver wifiScanReceiver = new WifiReceiver();
    protected WifiReceiver wifiStateReceiver = new WifiReceiver();
    protected int wifiReceiverState = WIFI_RECEIVER_UNREGISTERED;
    protected WifiManager wifiManager;
    protected DobbyThreadpool threadpool;
    protected WifiState wifiState;
    protected boolean wifiConnected;
    protected boolean wifiEnabled;
    protected SettableFuture<List<ScanResult>> wifiScanFuture;
    protected DobbyEventBus eventBus;
    @WifiState.WifiLinkMode
    protected int wifiStateProblemMode;
    protected List<ScanResult> combinedScanResult;


    protected WifiAnalyzer(Context context, WifiManager wifiManager, DobbyThreadpool threadpool, DobbyEventBus eventBus) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(wifiManager);
        this.context = context.getApplicationContext();
        this.wifiManager = wifiManager;
        this.threadpool = threadpool;
        this.eventBus = eventBus;
        wifiConnected = false;
        wifiEnabled = false;
        wifiState = new WifiState();
        wifiState.updateWifiStats(new DobbyWifiInfo(wifiManager.getConnectionInfo()), null);
        registerWifiStateReceiver();
        wifiStateProblemMode = WifiState.WifiLinkMode.NO_PROBLEM_DEFAULT_STATE;
        combinedScanResult = new ArrayList<>();
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

    /**
     * @return An instance of a {@link ListenableFuture<List<ScanResult>>} or null on immediate failure.
     */
    public ListenableFuture<List<ScanResult>> startWifiScan() {
        //Clearing out previous scan results
        combinedScanResult.clear();

        if (wifiReceiverState != WIFI_RECEIVER_REGISTERED) {
            registerScanReceiver();
        }
        if (wifiManager.startScan()) {
            wifiScanFuture = SettableFuture.create();
            return wifiScanFuture;
        }
        return null;
    }

    public DhcpInfo getDhcpInfo() {
        return wifiManager.getDhcpInfo();
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
            Log.v(TAG, "Exception while unregistering wifi receiver: " + e);
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

        synchronized public void onScanReceive(Intent intent) {
            final Intent intentToProcess = intent;
            currentScans++;
            final boolean doScanAgain = (currentScans < MIN_WIFI_SCANS_NEEDED);
            threadpool.submit(new Runnable() {
                @Override
                public void run() {
                    List<ScanResult> wifiList = wifiManager.getScanResults();
                    combinedScanResult.addAll(wifiList);
                    printScanResults(wifiList);

                    if (doScanAgain && wifiManager.startScan()) {
                        Log.v(TAG, "Starting Wifi Scan again, currentScans: " + currentScans);
                    } else {
                        eventBus.postEvent(new DobbyEvent(DobbyEvent.EventType.WIFI_SCAN_AVAILABLE));
                        updateWifiScanResults();
                        Log.v(TAG, "Unregistering Scan Receiver");
                        unregisterScanReceiver();
                        resetCurrentScans();
                    }
                }
            });
        }

        public void onWifiStateChange(Intent intent) {
            final Intent intentToProcess = intent;
            final String action = intent.getAction();
            threadpool.submit(new Runnable() {
                @Override
                public void run() {
                    processWifiStateRelatedIntents(intentToProcess);
                }
            });
        }


        @Override
        public void onReceive(Context c, Intent intent) {
            final Intent intentToProcess = intent;
            final String action = intent.getAction();
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                onScanReceive(intent);
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
        Log.i(TAG, "Wifi scan result: " + sb.toString());
    }

    private void updateWifiScanResults() {
        StringBuilder sb = new StringBuilder();
        wifiState.updateWifiStats(null, combinedScanResult);
        if (wifiScanFuture != null) {
            boolean setResult = wifiScanFuture.set(combinedScanResult);
            Log.v(TAG, "Setting wifi scan future: return value: " + setResult);
        }
        /*
        if (wifiList.size() == 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                Log.i(TAG, "isScanAlwaysAvailable: " + String.valueOf(wifiManager.isScanAlwaysAvailable()));
                Log.i(TAG, "WifiInfo:" + wifiManager.getConnectionInfo().toString());
                Log.i(TAG, "WifiState:" + wifiManager.getWifiState());
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
            Log.v(TAG, "Exception while unregistering wifi state receiver: " + e);
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
        switch (wifiStateProblemMode) {
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
        @WifiState.WifiLinkMode int problemMode = wifiState.updateDetailedWifiStateInfo(detailedState, System.currentTimeMillis());
        if (wifiStateProblemMode != problemMode) {
            wifiStateProblemMode = problemMode;
            @DobbyEvent.EventType int eventTypeToBroadcast = convertWifiStateProblemToDobbyEventType(wifiStateProblemMode);
            if (eventTypeToBroadcast != DobbyEvent.EventType.WIFI_STATE_UNKNOWN) {
                eventBus.postEvent(new DobbyEvent(eventTypeToBroadcast));
            }
        }
        //Utils.PercentileStats stats = wifiState.getStatsForDetailedState(detailedState, GAP_FOR_GETTING_DETAILED_NETWORK_STATE_STATS_MS);
        //Log.v(TAG, "updateDetailedWifiStateInfo State: " + detailedState.name() + " stats: " + stats.toString());
    }

    private void processNetworkStateChangedIntent(Intent intent) {
        final NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        if (networkInfo != null) {
            NetworkInfo.DetailedState detailedWifiState = networkInfo.getDetailedState();
            updateWifiStatsDetailedState(detailedWifiState);
        }

        boolean wasConnected = wifiConnected;
        wifiConnected = networkInfo != null && networkInfo.isConnected();
        // If we just connected, grab the initial signal strength and SSID
        if (wifiConnected && !wasConnected) {
            eventBus.postEvent(new DobbyEvent(DobbyEvent.EventType.WIFI_CONNECTED));
            // try getting it out of the intent first
            WifiInfo info = (WifiInfo) intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
            updateWifiStatsWithWifiInfo(info);

            DhcpInfo dhcpInfo = getDhcpInfo();
            if (dhcpInfo != null && dhcpInfo.ipAddress != 0) {
                eventBus.postEvent(new DobbyEvent(DobbyEvent.EventType.DHCP_INFO_AVAILABLE));
            }
        } else if (!wifiConnected && wasConnected) {
            wifiState.clearWifiConnectionInfo();
            eventBus.postEvent(new DobbyEvent(DobbyEvent.EventType.WIFI_NOT_CONNECTED));
        } else {
            if (wifiConnected) {
                Log.v(TAG, "No change in wifi state -- we were connected and are connected");
            } else {
                Log.v(TAG, "No change in wifi state -- we were NOT connected and are still NOT connected");
            }
        }
    }

    private void processWifiStateChangedIntent(Intent intent) {
        int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
        if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
            eventBus.postEvent(new DobbyEvent(DobbyEvent.EventType.WIFI_STATE_ENABLED));
            wifiEnabled = true;
        } else if (wifiState == WifiManager.WIFI_STATE_DISABLED) {
            eventBus.postEvent(new DobbyEvent(DobbyEvent.EventType.WIFI_STATE_DISABLED));
            wifiEnabled = false;
        } else if (wifiState == WifiManager.WIFI_STATE_ENABLING) {
            eventBus.postEvent(new DobbyEvent(DobbyEvent.EventType.WIFI_STATE_ENABLING));
            wifiEnabled = false;
        } else if (wifiState == WifiManager.WIFI_STATE_DISABLING) {
            eventBus.postEvent(new DobbyEvent(DobbyEvent.EventType.WIFI_STATE_DISABLING));
            wifiEnabled = false;
        } else if (wifiState == WifiManager.WIFI_STATE_UNKNOWN) {
            eventBus.postEvent(new DobbyEvent(DobbyEvent.EventType.WIFI_STATE_UNKNOWN));
            wifiEnabled = false;
        }
    }
    //Listening for WiFi intents
    synchronized private void processWifiStateRelatedIntents(Intent intent) {
        final String action = intent.getAction();
        int wifiState;
        if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
            processWifiStateChangedIntent(intent);
        } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            processNetworkStateChangedIntent(intent);
        } else if (action.equals(WifiManager.RSSI_CHANGED_ACTION)) {
            eventBus.postEvent(new DobbyEvent(DobbyEvent.EventType.WIFI_RSSI_CHANGED));
            int updatedSignal = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, 0);
            if (TRIGGER_WIFI_SCAN_ON_RSSI_CHANGE && this.wifiState.updateSignal(updatedSignal)){
                //Reissue wifi scan to correctly compute contention since the signal has changed significantly
                startWifiScan();
            }
        }
    }

}
