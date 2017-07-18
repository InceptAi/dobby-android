package com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.wifi;

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
import android.os.Build;
import android.support.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.inceptai.wifimonitoringservice.actionlibrary.utils.ActionLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Class contains logic for scanning, computing contention and any other wifi-related diagnostics.
 */
public class WifiController {

    private static final int WIFI_RECEIVER_UNREGISTERED = 0;
    private static final int WIFI_RECEIVER_REGISTERED = 1;
    private static final boolean TRIGGER_WIFI_SCAN_ON_RSSI_CHANGE = false;
    private static final int MIN_WIFI_SCANS_NEEDED = 1;

    // Store application context to prevent leaks and crashes from an activity going out of scope.
    private Context context;
    private Executor executor;

    private WifiReceiver wifiScanReceiver = new WifiReceiver();
    private WifiReceiver wifiStateReceiver = new WifiReceiver();
    private int wifiReceiverState = WIFI_RECEIVER_UNREGISTERED;
    private WifiManager wifiManager;
    private List<ScanResult> combinedScanResult;
    private boolean wifiConnected;
    private NetworkInfo.DetailedState lastDetailedState;

    //All the future variables live here
    private SettableFuture<List<ScanResult>> wifiScanFuture;
    private SettableFuture<Boolean> turnWifiOffFuture;
    private SettableFuture<Boolean> turnWifiOnFuture;
    private SettableFuture<Boolean> reAssociateCurrentWifiFuture;
    private SettableFuture<Boolean> disconnectCurrentWifiFuture;
    private SettableFuture<Boolean> forgetWifiFuture;
    private SettableFuture<Boolean> connectWithWifiFuture;
    private SettableFuture<List<WifiConfiguration>> wifiConfigurationFuture;


    private WifiController(Context context, Executor executor, WifiManager wifiManager) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(wifiManager);
        this.context = context;
        this.executor = executor;
        this.wifiManager = wifiManager;
        combinedScanResult = new ArrayList<>();
        wifiConnected = false;
        registerWifiStateReceiver();
        lastDetailedState = NetworkInfo.DetailedState.IDLE;
    }

    /**
     * Factory constructor to create an instance
     *
     * @param context Application context.
     * @return Instance of WifiAnalyzer or null on error.
     */
    @Nullable
    public static WifiController create(Context context, Executor executor) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            return new WifiController(context.getApplicationContext(), executor, wifiManager);
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
     * @return An instance of a {@link ListenableFuture<Boolean>} or null on immediate failure.
     */
    public ListenableFuture<Boolean> reAssociateWithCurrentWifi() {
        if (reAssociateCurrentWifiFuture != null && !reAssociateCurrentWifiFuture.isDone()) {
            return reAssociateCurrentWifiFuture;
        }
        reAssociateCurrentWifiFuture = SettableFuture.create();
        if (wifiConnected) {
            wifiManager.disconnect();
        }
        wifiManager.reconnect();
        return reAssociateCurrentWifiFuture;
    }

    /**
     * @return An instance of a {@link ListenableFuture<Boolean>} or null on immediate failure.
     */
    public ListenableFuture<Boolean> forgetNetwork(int networkId) {
        if (forgetWifiFuture != null && !forgetWifiFuture.isDone()) {
            //WifiScan in progress, just return the current future.
            return forgetWifiFuture;
        }
        forgetWifiFuture = SettableFuture.create();
        forgetWifiFuture.set(wifiManager.removeNetwork(networkId));
        return forgetWifiFuture;
    }

    /**
     * @return An instance of a {@link ListenableFuture<Boolean>} or null on immediate failure.
     */
    public ListenableFuture<Boolean> disconnectFromCurrentWifi() {
        if (disconnectCurrentWifiFuture != null && !disconnectCurrentWifiFuture.isDone()) {
            return disconnectCurrentWifiFuture;
        }
        disconnectCurrentWifiFuture = SettableFuture.create();
        if (wifiConnected) {
            //Disconnected
            wifiManager.disconnect();
            //Set the result when state changes
        } else {
            //Wifi is already disconnected, so return true
            disconnectCurrentWifiFuture.set(true);
        }
        return disconnectCurrentWifiFuture;
    }


    /**
     * @return An instance of a {@link ListenableFuture<Boolean>} or null on immediate failure.
     */
    public ListenableFuture<Boolean> connectWithWifiNetwork(int networkId) {
        if (connectWithWifiFuture != null && !connectWithWifiFuture.isDone()) {
            return connectWithWifiFuture;
        }
        connectWithWifiFuture = SettableFuture.create();
        if (wifiConnected) {
            wifiManager.disconnect();
        }
        wifiManager.enableNetwork(networkId, true);
        return connectWithWifiFuture;
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


    public ListenableFuture<List<WifiConfiguration>> getWifiConfiguration() {
        if (wifiConfigurationFuture != null && !wifiConfigurationFuture.isDone()) {
            //WifiScan in progress, just return the current future.
            return wifiConfigurationFuture;
        }
        wifiConfigurationFuture = SettableFuture.create();
        wifiConfigurationFuture.set(wifiManager.getConfiguredNetworks());
        return wifiConfigurationFuture;
    }

    public ListenableFuture<DhcpInfo> getDhcpInfo() {
        SettableFuture<DhcpInfo> dhcpInfoSettableFuture = SettableFuture.create();
        dhcpInfoSettableFuture.set(wifiManager.getDhcpInfo());
        return dhcpInfoSettableFuture;
    }

    public ListenableFuture<WifiInfo> getWifiInfo() {
        SettableFuture<WifiInfo> wifiInfoSettableFuture = SettableFuture.create();
        wifiInfoSettableFuture.set(wifiManager.getConnectionInfo());
        return wifiInfoSettableFuture;
    }

    public ListenableFuture<Boolean> check5GHzSupported() {
        SettableFuture<Boolean> check5GHzSettableFuture = SettableFuture.create();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            check5GHzSettableFuture.set(wifiManager.is5GHzBandSupported());
        } else {
            check5GHzSettableFuture.set(null);
        }
        return check5GHzSettableFuture;
    }


    public void cleanup() {
        unregisterScanReceiver();
        unregisterWifiStateReceiver();
    }

    public boolean isWifiConnected() {
        return isWifiEnabled() && lastDetailedState == NetworkInfo.DetailedState.CONNECTED;
    }

    //Private calls
    private void registerScanReceiver() {
        context.registerReceiver(wifiScanReceiver,
                new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        wifiReceiverState = WIFI_RECEIVER_REGISTERED;
    }

    private void unregisterScanReceiver() {
        try {
            context.unregisterReceiver(wifiScanReceiver);
        } catch (IllegalArgumentException e) {
            ActionLog.v("Exception while un-registering wifi receiver: " + e);
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

        public void onScanReceive() {
            currentScans++;
            final boolean doScanAgain = (currentScans < MIN_WIFI_SCANS_NEEDED);
            executor.execute(new Runnable() {
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
            executor.execute(new Runnable() {
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
            sb.append(Integer.toString(i + 1));
            sb.append(".");
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

    private boolean isWifiEnabled() {
        return wifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED;
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
    private void processWifiStateRelatedIntents(Intent intent) {
        final String action = intent.getAction();
        switch (action) {
            case WifiManager.WIFI_STATE_CHANGED_ACTION:
                processWifiStateChanged(intent);
                break;
            case WifiManager.NETWORK_STATE_CHANGED_ACTION:
                processNetworkStateChangedIntent(intent);
                break;
            case WifiManager.RSSI_CHANGED_ACTION:
                //int updatedSignal = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, 0);
                if (TRIGGER_WIFI_SCAN_ON_RSSI_CHANGE) {
                    //Reissue wifi scan to correctly compute contention since the signal has changed significantly
                    //Force a wifi scan
                    startWifiScan();
                }
                break;
        }
    }


    private void processNetworkStateChangedIntent(Intent intent) {
        final NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        if (networkInfo != null) {
            lastDetailedState = networkInfo.getDetailedState();
            boolean wasConnected = wifiConnected;
            wifiConnected = networkInfo.isConnected();
            //If no longer connected, clear the connection info
            if (!wifiConnected) {
                //Set disconnect future
                if (disconnectCurrentWifiFuture != null) {
                    disconnectCurrentWifiFuture.set(true);
                }
            }
            if (wifiConnected) {
                //Re-associate does not trigger a disconnect -- do a disconnect and then reconnect.
                if (reAssociateCurrentWifiFuture != null) {
                    reAssociateCurrentWifiFuture.set(true);
                }
                //Set reconnect / re-associate / connect future
                if (connectWithWifiFuture != null) {
                    connectWithWifiFuture.set(true);
                }
            }
            if (wifiConnected && !wasConnected) {
                WifiInfo info = (WifiInfo) intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
            }
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
