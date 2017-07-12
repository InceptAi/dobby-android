package com.inceptai.wifimonitoringservice.monitors;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.Nullable;

import com.inceptai.actionlibrary.utils.ActionLog;
import com.inceptai.wifimonitoringservice.utils.WifiStateData;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.inceptai.wifimonitoringservice.utils.WifiStateData.WifiProblemMode.ERROR_AUTHENTICATING;
import static com.inceptai.wifimonitoringservice.utils.WifiStateData.WifiProblemMode.FREQUENT_DISCONNECTIONS;
import static com.inceptai.wifimonitoringservice.utils.WifiStateData.WifiProblemMode.HANGING_ON_AUTHENTICATING;
import static com.inceptai.wifimonitoringservice.utils.WifiStateData.WifiProblemMode.HANGING_ON_CONNECTING;
import static com.inceptai.wifimonitoringservice.utils.WifiStateData.WifiProblemMode.HANGING_ON_DHCP;
import static com.inceptai.wifimonitoringservice.utils.WifiStateData.WifiProblemMode.HANGING_ON_SCANNING;
import static com.inceptai.wifimonitoringservice.utils.WifiStateData.WifiProblemMode.PROBLEMATIC_SUPPLICANT_PATTERN;

/**
 * Created by vivek on 7/9/17.
 */

public class WifiStateMonitor {
    //TODO: Use weak reference for context -- look at WFMonitor from Wifi Fixer and here:
    // https://medium.com/google-developer-experts/finally-understanding-how-references-work-in-android-and-java-26a0d9c92f83
    private Context context;
    private WifiIntentReceiver wifiIntentReceiver;
    private AtomicBoolean wifiReceiverRegistered;
    private WifiStateCallback wifiStateCallback;
    private WifiStateData wifiStateData;

    public interface WifiStateCallback {
        void wifiStateEnabled();
        void wifiStateDisabled();
        void wifiStateDisconnected();
        void wifiStateConnected();
        void wifiPrimaryAPSignalLow();
        void wifiStateHangingOnScanning();
        void wifiStateHangingOnObtainingIPAddress();
        void wifiStateHangingOnAuthenticating();
        void wifiStateFrequentDropOff();
        void wifiStateErrorAuthenticating();
        void wifiStateProblematicSupplicantPattern();
        void wifiNetworkConnected();
        void wifiNetworkDisconnected();
        void wifiNetworkInvalidOrInactiveOrDormant();
        void wifiNetworkDisconnectedUnexpectedly();
    }

    public WifiStateMonitor(Context context) {
        this.context = context.getApplicationContext();
        wifiStateData = new WifiStateData();
        wifiIntentReceiver = new WifiIntentReceiver();
        wifiReceiverRegistered = new AtomicBoolean(false);
    }

    public void registerCallback(WifiStateCallback wifiStateCallback) {
        this.wifiStateCallback = wifiStateCallback;
        if (wifiReceiverRegistered.compareAndSet(false, true)) {
            registerReceiver();
        }
    }

    public void unregisterCallback() {
        this.wifiStateCallback = null;
        if (wifiReceiverRegistered.compareAndSet(true, false)) {
            unregisterReceiver();
        }
    }

    //Discover the public methods -- do we need any
    private void registerReceiver() {
        IntentFilter intentFilter =  getIntentFilter();
        context.registerReceiver(wifiIntentReceiver, intentFilter);

    }

    private void unregisterReceiver() {
        try {
            context.unregisterReceiver(wifiIntentReceiver);
        } catch (IllegalArgumentException e) {
            ActionLog.v("Exception while un-registering wifi receiver: " + e);
        }
    }

    //Declare a broadcast receiver and listen for all the interesting intents.
    // Process the state here and provide meaningful callbacks to WifiMonitor to act.
    private IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION);
        // Supplicant State filter
        filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        // Network State filter
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        // wifi scan results available callback
        filter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        //Changes in network state
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
        return filter;
    }

    private class WifiIntentReceiver extends BroadcastReceiver {
        private int currentScans = 0;

        public WifiIntentReceiver() {
        }

        public void onScanReceived() {}

        public void onWifiSignalChanged(int updatedSignal) {
            //TODO: Change thread before posting the callback
            @WifiStateData.WifiProblemMode int wifiLinkMode = wifiStateData.updateSignal(updatedSignal);
            if (wifiLinkMode == WifiStateData.WifiProblemMode.LOW_SNR) {
                //We have a link with low snr. give callback
                if (wifiStateCallback != null) {
                    wifiStateCallback.wifiPrimaryAPSignalLow();
                }
            }
        }

        @SuppressLint("SwitchIntDef")
        public void onDetailedStateChanged(NetworkInfo.DetailedState detailedState, @Nullable WifiInfo wifiInfo) {
            if (wifiInfo != null) {
                wifiStateData.updatePrimaryLinkInfo(wifiInfo);
            }
            @WifiStateData.WifiProblemMode int newProblem = wifiStateData.updateWifiDetailedState(detailedState);
            if (wifiStateCallback != null) {
                if (detailedState == NetworkInfo.DetailedState.CONNECTED) {
                    wifiStateCallback.wifiStateConnected();
                } else if (detailedState == NetworkInfo.DetailedState.DISCONNECTED) {
                    wifiStateCallback.wifiStateDisconnected();
                }
                switch (newProblem) {
                    case HANGING_ON_DHCP:
                        wifiStateCallback.wifiStateHangingOnObtainingIPAddress();
                        break;
                    case HANGING_ON_AUTHENTICATING:
                        wifiStateCallback.wifiStateHangingOnAuthenticating();
                        break;
                    case HANGING_ON_SCANNING:
                        wifiStateCallback.wifiStateHangingOnScanning();
                        break;
                    case FREQUENT_DISCONNECTIONS:
                        wifiStateCallback.wifiStateFrequentDropOff();
                        break;
                    case HANGING_ON_CONNECTING:
                        //Not implemented right now
                        //wifiStateCallback.wifiStateHangingOnConnecting();
                        break;
                    default:
                        break;
                }
            }
        }

        public void onSupplicantStateChanged(SupplicantState supplicantState, int supplicantError) {
            @WifiStateData.WifiProblemMode int wifiProblem = wifiStateData.updateSupplicantState(supplicantState, supplicantError);
            if (wifiStateCallback != null) {
                if (wifiProblem == PROBLEMATIC_SUPPLICANT_PATTERN) {
                    wifiStateCallback.wifiStateProblematicSupplicantPattern();
                } else if (wifiProblem == ERROR_AUTHENTICATING) {
                    wifiStateCallback.wifiStateErrorAuthenticating();
                }
            }
        }

        public void onWifiStateChanged(int wifiState) {
            wifiStateData.updateLastWifiEnabledState(wifiState);
            if (wifiStateCallback != null) {
                switch (wifiState) {
                    case WifiManager.WIFI_STATE_ENABLED:
                        wifiStateCallback.wifiStateEnabled();
                        break;
                    case WifiManager.WIFI_STATE_DISABLED:
                        wifiStateCallback.wifiStateDisabled();
                        break;
                }
            }
        }

        public void onConnectivityStateChanged(NetworkInfo networkInfo) {
            /*
             * This action means network connectivity has changed but, we only want
             * to run this code for wifi
             */
            if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                wifiStateData.updateWifiConnectionState(networkInfo.getState());
                if (wifiStateCallback != null) {
                    if (networkInfo.getState().equals(NetworkInfo.State.CONNECTED)) {
                        wifiStateCallback.wifiNetworkConnected();
                    } else if (networkInfo.getState().equals(NetworkInfo.State.DISCONNECTED)) {
                        wifiStateCallback.wifiNetworkDisconnected();
                    }
                }
            }
        }

        @Override
        public void onReceive(Context c, Intent intent) {
            final String action = intent.getAction();
            NetworkInfo.DetailedState detailedState;
            SupplicantState supplicantState;
            NetworkInfo networkInfo;
            WifiInfo wifiInfo = null;
            int supplicantError;
            switch (action) {
                case WifiManager.SCAN_RESULTS_AVAILABLE_ACTION:
                    onScanReceived();
                    break;
                case WifiManager.RSSI_CHANGED_ACTION:
                    int updatedSignal = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, WifiStateData.ANDROID_INVALID_RSSI);
                    onWifiSignalChanged(updatedSignal);
                    break;
                case WifiManager.WIFI_STATE_CHANGED_ACTION:
                    int newWifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
                    onWifiStateChanged(newWifiState);
                    break;
                case WifiManager.NETWORK_STATE_CHANGED_ACTION:
                    detailedState = ((NetworkInfo)intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO)).getDetailedState();
                    if (detailedState == NetworkInfo.DetailedState.CONNECTED) {
                        wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
                    }
                    onDetailedStateChanged(detailedState, wifiInfo);
                    break;
                case ConnectivityManager.CONNECTIVITY_ACTION:
                    networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    onConnectivityStateChanged(networkInfo);
                    break;
                case WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION:
                    detailedState = WifiInfo.getDetailedStateOf((SupplicantState)
                            intent.getParcelableExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED));
                    onDetailedStateChanged(detailedState, null);
                    break;
                case WifiManager.SUPPLICANT_STATE_CHANGED_ACTION:
                    supplicantState = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
                    supplicantError = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, 0);
                    onSupplicantStateChanged(supplicantState, supplicantError);
                    break;
            }
        }
    }


}
