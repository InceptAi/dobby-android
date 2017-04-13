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
import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.eventbus.DobbyEvent;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.fake.FakeWifiAnalyzer;

import java.util.List;

import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * Class contains logic for scanning, computing contention and any other wifi-related diagnostics.
 */

public class WifiAnalyzer {

    private static final int WIFI_RECEIVER_UNREGISTERED = 0;
    private static final int WIFI_RECEIVER_REGISTERED = 1;
    private static final String UNINITIALIZED_IP_ADDRESS = "0.0.0.0";

    // Store application context to prevent leaks and crashes from an activity going out of scope.
    private Context context;
    private WifiReceiver wifiScanReceiver = new WifiReceiver();
    private WifiReceiver wifiStateReceiver = new WifiReceiver();
    private int wifiReceiverState = WIFI_RECEIVER_UNREGISTERED;
    private WifiManager wifiManager;
    private DobbyThreadpool threadpool;
    private WifiStats wifiStats;
    private boolean wifiConnected;
    private boolean wifiEnabled;
    private SettableFuture<List<ScanResult>> wifiScanFuture;
    private DobbyEventBus eventBus;
    private FakeWifiAnalyzer fakeWifiAnalyzer;


    private WifiAnalyzer(Context context, WifiManager wifiManager, DobbyThreadpool threadpool, DobbyEventBus eventBus) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(wifiManager);
        this.context = context.getApplicationContext();
        this.wifiManager = wifiManager;
        this.threadpool = threadpool;
        this.eventBus = eventBus;
        wifiConnected = false;
        wifiEnabled = false;
        wifiStats = new WifiStats();
        wifiStats.updateWifiStats(wifiManager.getConnectionInfo(), null);
        registerWifiStateReceiver();
        fakeWifiAnalyzer = new FakeWifiAnalyzer(threadpool);
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
        if (DobbyApplication.USE_FAKES.get()) {
            return fakeWifiAnalyzer.startWifiScan();
        }
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

    public WifiStats getWifiStats() {
        if (DobbyApplication.USE_FAKES.get()) {
            return fakeWifiAnalyzer.getWifiStats();
        }
        return wifiStats;
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
        context.unregisterReceiver(wifiScanReceiver);
        wifiReceiverState = WIFI_RECEIVER_UNREGISTERED;
    }

    public DhcpInfo getDhcpInfo() {
        return wifiManager.getDhcpInfo();
    }

    private class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context c, Intent intent) {
            final Intent intentToProcess = intent;
            threadpool.submit(new Runnable() {
                @Override
                public void run() {
                    processWifiIntents(intentToProcess);
                }
            });
        }
    }

    private void processWifiIntents(Intent intent) {
        final String action = intent.getAction();
        if (action.equals(WifiManager.RSSI_CHANGED_ACTION)
                || action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)
                || action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            updateWifiState(intent);
        } else if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
            eventBus.postEvent(new DobbyEvent(DobbyEvent.EventType.WIFI_SCAN_AVAILABLE));
            updateWifiScanResults();
            unregisterScanReceiver();
        }
    }

    private void updateWifiScanResults() {
        StringBuilder sb = new StringBuilder();
        List<ScanResult> wifiList = wifiManager.getScanResults();
        wifiStats.updateWifiStats(null, wifiList);
        for (int i = 0; i < wifiList.size(); i++) {
            sb.append(new Integer(i + 1).toString() + ".");
            sb.append((wifiList.get(i)).toString());
            sb.append("\\n");
        }
        Log.i(TAG, "Wifi scan result: " + sb.toString());
        if (wifiScanFuture != null) {
            wifiScanFuture.set(wifiList);
        }
    }

    private void registerWifiStateReceiver() {
        IntentFilter wifiStateIntentFilter = new IntentFilter();
        wifiStateIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        wifiStateIntentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        wifiStateIntentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        context.registerReceiver(wifiStateReceiver, wifiStateIntentFilter);
    }

    private void unregisterWifiStateReceiver() {
        context.unregisterReceiver(wifiStateReceiver);
    }

    //Listening for WiFi intents
    synchronized private void updateWifiState(Intent intent) {
        final String action = intent.getAction();
        int wifiState;
        if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
            eventBus.postEvent(new DobbyEvent(DobbyEvent.EventType.WIFI_STATE_CHANGED));
            wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
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
        } else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
            final NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            boolean wasConnected = wifiConnected;
            wifiConnected = networkInfo != null && networkInfo.isConnected();
            // If we just connected, grab the initial signal strength and SSID
            if (wifiConnected && !wasConnected) {
                // try getting it out of the intent first
                WifiInfo info = (WifiInfo) intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);

                if (info == null) {
                    info = wifiManager.getConnectionInfo();
                }
                wifiStats.updateWifiStats(info, null);

                DhcpInfo dhcpInfo = getDhcpInfo();
                if (dhcpInfo != null && dhcpInfo.ipAddress != 0) {
                    eventBus.postEvent(new DobbyEvent(DobbyEvent.EventType.DHCP_INFO_AVAILABLE));
                }

                eventBus.postEvent(new DobbyEvent(DobbyEvent.EventType.WIFI_CONNECTED));
            } else if (!wifiConnected && wasConnected) {
                wifiStats.clearWifiConnectionInfo();
                eventBus.postEvent(new DobbyEvent(DobbyEvent.EventType.WIFI_NOT_CONNECTED));
            } else {

            }
        } else if (action.equals(WifiManager.RSSI_CHANGED_ACTION)) {
            eventBus.postEvent(new DobbyEvent(DobbyEvent.EventType.WIFI_RSSI_CHANGED));
            int updatedSignal = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, 0);
            wifiStats.updateSignal(updatedSignal);
        }
    }

    private class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context c, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.RSSI_CHANGED_ACTION)
                    || action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)
                    || action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                updateWifiState(intent);
            } else if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                updateWifiScanResults();
                unregisterScanReceiver();
            }
        }
    }

}
