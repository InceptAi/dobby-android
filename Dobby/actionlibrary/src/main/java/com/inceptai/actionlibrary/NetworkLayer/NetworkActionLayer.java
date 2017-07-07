package com.inceptai.actionlibrary.NetworkLayer;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.actionlibrary.ActionThreadPool;
import com.inceptai.actionlibrary.NetworkLayer.wifi.WifiController;

import java.util.List;

/**
 * This class abstracts out the implementation details of all things 'network' related. The UI and
 * local bots would interact with this class to run tests, diagnostics etc. Also
 */

public class NetworkActionLayer {
    private WifiController wifiController;

    // Use Dagger to get a singleton instance of this class.
    public NetworkActionLayer(Context context, ActionThreadPool threadpool) {
        this.wifiController = WifiController.create(context, threadpool);
    }

    public ListenableFuture<List<ScanResult>> getNearbyWifiNetworks() {
        if (wifiController != null) {
            return wifiController.startWifiScan();
        }
        return null;
    }

    public ListenableFuture<Boolean> checkIf5GHzSupported() {
        if (wifiController != null) {
            return wifiController.check5GHzSupported();
        }
        return null;
    }

    public ListenableFuture<Boolean> turnWifiOn() {
        if (wifiController != null) {
            return wifiController.turnWifiOn();
        }
        return null;
    }

    public ListenableFuture<Boolean> turnWifiOff() {
        if (wifiController != null) {
            return wifiController.turnWifiOff();
        }
        return null;
    }

    public ListenableFuture<List<WifiConfiguration>> getConfiguredWifiNetworks() {
        if (wifiController != null) {
            return wifiController.getWifiConfiguration();
        }
        return null;
    }

    public ListenableFuture<DhcpInfo> getDhcpInfo() {
        if (wifiController != null) {
            return wifiController.getDhcpInfo();
        }
        return null;
    }

    public ListenableFuture<WifiInfo> getWifiInfo() {
        if (wifiController != null) {
            return wifiController.getWifiInfo();
        }
        return null;
    }

    public ListenableFuture<Boolean> resetConnectionToActiveWifi() {
        if (wifiController != null) {
            return wifiController.reAssociateWithCurrentWifi();
        }
        return null;
    }

    public ListenableFuture<Boolean> disconnectFromCurrentWifi() {
        if (wifiController != null) {
            return wifiController.disconnectFromCurrentWifi();
        }
        return null;
    }

    public ListenableFuture<Boolean> forgetWifiNetwork(int networkId) {
        if (wifiController != null) {
            return wifiController.forgetNetwork(networkId);
        }
        return null;
    }


    public ListenableFuture<Boolean> connectWifiNetwork(int networkId) {
        if (wifiController != null) {
            return wifiController.connectWithWifiNetwork(networkId);
        }
        return null;
    }

    public void cleanup() {}
}
