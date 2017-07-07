package com.inceptai.actionlibrary.NetworkLayer.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.inceptai.actionlibrary.ActionThreadPool;
import com.inceptai.actionlibrary.utils.ActionLog;

import java.util.ArrayList;
import java.util.List;

/**
 * Class contains logic for scanning, computing contention and any other wifi-related diagnostics.
 */
public class WifiController {

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
    protected ActionThreadPool threadpool;
    protected List<ScanResult> combinedScanResult;

    //All the future variables live here
    protected SettableFuture<List<ScanResult>> wifiScanFuture;
    protected SettableFuture<Boolean> turnWifiOffFuture;
    protected SettableFuture<Boolean> turnWifiOnFuture;

    private WifiController(Context context, ActionThreadPool threadpool, WifiManager wifiManager) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(wifiManager);
        this.context = context;
        this.wifiManager = wifiManager;
        this.threadpool = threadpool;
        combinedScanResult = new ArrayList<>();
        initializeWifiState();
    }

    /**
     * Factory constructor to create an instance
     *
     * @param context Application context.
     * @return Instance of WifiAnalyzer or null on error.
     */
    @Nullable
    public static WifiController create(Context context, ActionThreadPool threadpool) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            return new WifiController(context.getApplicationContext(), threadpool, wifiManager);
        }
        return null;
    }


    /**
     * @return An instance of a {@link ListenableFuture<Boolean>} or null on immediate failure.
     */
    public ListenableFuture<Boolean> turnWifiOff() {
        if (turnWifiOffFuture != null && !turnWifiOffFuture.isDone()) {
            return turnWifiOffFuture;
        }
        turnWifiOffFuture = SettableFuture.create();
        if (wifiManager.isWifiEnabled()) {
            //Turn off wifi
            wifiManager.setWifiEnabled(false);
            //Set the result when state changes
        } else {
            //Wifi is already off, so return true
            turnWifiOffFuture.set(true);
        }
        return turnWifiOffFuture;
    }


    /**
     * @return An instance of a {@link ListenableFuture<Boolean>} or null on immediate failure.
     */
    public ListenableFuture<Boolean> turnWifiOn() {
        if (turnWifiOnFuture != null && !turnWifiOnFuture.isDone()) {
            return turnWifiOnFuture;
        }
        turnWifiOnFuture = SettableFuture.create();
        if (!wifiManager.isWifiEnabled()) {
            //Turn on wifi
            wifiManager.setWifiEnabled(true);
            //Set the result when state changes
        } else {
            //Wifi is already on, so return true
            turnWifiOnFuture.set(true);
        }
        return turnWifiOnFuture;
    }


    /**
         * @return An instance of a {@link ListenableFuture<List<ScanResult>>} or null on immediate failure.
     */
    public ListenableFuture<List<ScanResult>> startWifiScan() {
        if (wifiScanFuture != null && !wifiScanFuture.isDone()) {
            //WifiScan in progress, just return the current future.
            return wifiScanFuture;
        }

        combinedScanResult.clear();

        if (wifiReceiverState != WIFI_RECEIVER_REGISTERED) {
            registerScanReceiver();
        }
        //Catch security exception here
        try {
            if (wifiManager.startScan()) {
                wifiScanFuture = SettableFuture.create();
                return wifiScanFuture;
            }
        } catch (SecurityException e) {
            ActionLog.e("Security Exception while scanning");
        }
        return null;
    }

    public DhcpInfo getDhcpInfo() {
        return wifiManager.getDhcpInfo();
    }

    public List<WifiConfiguration> getWifiConfiguration() {
        List<WifiConfiguration> wifiConfigurationList = wifiManager.getConfiguredNetworks();
        if (wifiConfigurationList == null) {
            return new ArrayList<>();
        }
        return wifiManager.getConfiguredNetworks();
    }

    public void cleanup() {
        unregisterScanReceiver();
        unregisterWifiStateReceiver();
    }


    //Private calls
    private void initializeWifiState() {
        Preconditions.checkNotNull(wifiManager);
        registerWifiStateReceiver();
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
            ActionLog.v("Exception while unregistering wifi receiver: " + e);
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
                        ActionLog.e("Security exception while getting scan results: " + e);
                        setWifiScanFuture(combinedScanResult);
                        unregisterScanReceiver();
                        resetCurrentScans();
                        return;
                    }
                    //We got some results back from getScanResults -- so presumably we have location
                    if (doScanAgain && wifiManager.startScan()) {
                        ActionLog.v("Starting Wifi Scan again, currentScans: " + currentScans);
                    } else {
                        updateWifiScanResults();
                        ActionLog.v("Un-registering Scan Receiver");
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
        ActionLog.i("Wifi scan result: " + sb.toString());
    }

    private void setWifiScanFuture(List<ScanResult> scanResultList) {
        if (wifiScanFuture != null) {
            boolean setResult = wifiScanFuture.set(scanResultList);
            ActionLog.v("Setting wifi scan future: return value: " + setResult);
        }
    }

    private void updateWifiScanResults() {
        StringBuilder sb = new StringBuilder();
        setWifiScanFuture(combinedScanResult);
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
            ActionLog.v("Exception while unregistering wifi state receiver: " + e);
        }
    }


    //Listening for WiFi intents
    synchronized private void processWifiStateRelatedIntents(Intent intent) {
        final String action = intent.getAction();
        switch (action) {
            case WifiManager.WIFI_STATE_CHANGED_ACTION:
                processWifiStateChanged(intent);
                break;
            case WifiManager.NETWORK_STATE_CHANGED_ACTION:
                break;
            case WifiManager.RSSI_CHANGED_ACTION:
                int updatedSignal = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, 0);
                if (TRIGGER_WIFI_SCAN_ON_RSSI_CHANGE) {
                    //Reissue wifi scan to correctly compute contention since the signal has changed significantly
                    //Force a wifi scan
                    startWifiScan();
                }
                break;
        }
    }


    private void processWifiStateChanged(Intent intent) {
        int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
        switch (wifiState) {
            case WifiManager.WIFI_STATE_ENABLED:
                if (turnWifiOnFuture != null) {
                    turnWifiOnFuture.set(true);
                }
                break;
            case WifiManager.WIFI_STATE_DISABLED:
                if (turnWifiOffFuture != null) {
                    turnWifiOffFuture.set(true);
                }
                break;
            case WifiManager.WIFI_STATE_ENABLING:
                break;
            case WifiManager.WIFI_STATE_DISABLING:
                break;
            case WifiManager.WIFI_STATE_UNKNOWN:
                break;
        }
    }
}
