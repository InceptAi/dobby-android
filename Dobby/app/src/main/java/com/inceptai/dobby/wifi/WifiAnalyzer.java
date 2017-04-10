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

import java.util.List;

import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * Class contains logic for scanning, computing contention and any other wifi-related diagnostics.
 */

public class WifiAnalyzer {

    private static final int WIFI_RECEIVER_UNREGISTERED = 0;
    private static final int WIFI_RECEIVER_REGISTERED = 1;

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


    private WifiAnalyzer(Context context, WifiManager wifiManager, DobbyThreadpool threadpool) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(wifiManager);
        this.context = context.getApplicationContext();
        this.wifiManager = wifiManager;
        this.threadpool = threadpool;
        wifiConnected = false;
        wifiEnabled = false;
        wifiStats = new WifiStats();
        wifiStats.updateWifiStats(wifiManager.getConnectionInfo(), null);
        registerWifiStateReceiver();
    }

    /**
     * Factory constructor to create an instance
     * @param context Application context.
     * @return Instance of WifiAnalyzer or null on error.
     */
    @Nullable
    public static WifiAnalyzer create(Context context, DobbyThreadpool threadpool) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            return new WifiAnalyzer(context.getApplicationContext(), wifiManager, threadpool);
        }
        return null;
    }

    /**
     *
     * @return An instance of a {@link ListenableFuture<List<ScanResult>>} or null on immediate failure.
     */
    public ListenableFuture<List<ScanResult>> startWifiScan() {
        if (wifiReceiverState != WIFI_RECEIVER_REGISTERED) {
            registerScanReceiver();
        }
        if (wifiManager.startScan()){
            wifiScanFuture = SettableFuture.create();
            return wifiScanFuture;
        }
        return null;
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

    private void updateWifiScanResults()  {
        StringBuilder sb = new StringBuilder();
        List<ScanResult> wifiList = wifiManager.getScanResults();
        wifiStats.updateWifiStats(null, wifiList);
        for(int i = 0; i < wifiList.size(); i++){
            sb.append(new Integer(i+1).toString() + ".");
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
    private void updateWifiState(Intent intent) {
        final String action = intent.getAction();
        int wifiState;
        if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
            wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
            if (wifiState == WifiManager.WIFI_STATE_ENABLED) {
                wifiEnabled = true;
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
            } else if (!wifiConnected) {
                wifiStats.clearWifiConnectionInfo();
            }
        } else if (action.equals(WifiManager.RSSI_CHANGED_ACTION)) {
            int updatedSignal = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, 0);
            wifiStats.updateSignal(updatedSignal);
        }
    }

    public WifiStats getWifiStats() {
        return wifiStats;
    }

}
