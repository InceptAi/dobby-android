package com.inceptai.dobby.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.base.Preconditions;

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
    private WifiReceiver wifiReceiver = new WifiReceiver();
    private int wifiReceiverState = WIFI_RECEIVER_UNREGISTERED;
    private WifiManager wifiManager;

    private WifiAnalyzer(Context context, WifiManager wifiManager) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(wifiManager);
        this.context = context.getApplicationContext();
        this.wifiManager = wifiManager;
    }

    /**
     * Factory constructor to create an instance
     * @param context Application context.
     * @return Instance of WifiAnalyzer or null on error.
     */
    @Nullable
    public static WifiAnalyzer create(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            return new WifiAnalyzer(context.getApplicationContext(), wifiManager);
        }
        return null;
    }

    public boolean   startWifiScan() {
        if (wifiReceiverState != WIFI_RECEIVER_REGISTERED) {
            registerScanReceiver();
        }
        return wifiManager.startScan();
    }

    private void registerScanReceiver() {
        context.registerReceiver(wifiReceiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiReceiverState = WIFI_RECEIVER_REGISTERED;
    }

    private void unregisterScanReceiver() {
        context.unregisterReceiver(wifiReceiver);
        wifiReceiverState = WIFI_RECEIVER_UNREGISTERED;
    }

    private class WifiReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context c, Intent intent) {
            StringBuilder sb = new StringBuilder();
            List<ScanResult> wifiList = wifiManager.getScanResults();
            for(int i = 0; i < wifiList.size(); i++){
                sb.append(new Integer(i+1).toString() + ".");
                sb.append((wifiList.get(i)).toString());
                sb.append("\\n");
            }
            Log.i(TAG, "Wifi scan result: " + sb.toString());
        }
    }
}
