package com.inceptai.actionlibrary.NetworkLayer;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.actionlibrary.ActionThreadPool;
import com.inceptai.actionlibrary.NetworkLayer.wifi.WifiController;

import java.util.List;

/**
 * This class abstracts out the implementation details of all things 'network' related. The UI and
 * local bots would interact with this class to run tests, diagnostics etc. Also
 */

public class NetworkLayer {
    private Context context;
    private ActionThreadPool threadpool;
    private WifiController wifiController;
    private static NetworkLayer NETWORK_LAYER_INSTANCE = null;

    // Use Dagger to get a singleton instance of this class.
    private NetworkLayer(Context context, ActionThreadPool threadpool) {
        this.context = context;
        this.threadpool = threadpool;
        this.wifiController = WifiController.create(context, threadpool);
    }

    /**
     * Factory constructor to create an instance
     *
     * @param context Application context.
     * @return Instance of WifiAnalyzer or null on error.
     */
    @Nullable
    public static NetworkLayer getInstance(Context context, ActionThreadPool threadpool) {
        if (NETWORK_LAYER_INSTANCE == null) {
            NETWORK_LAYER_INSTANCE = new NetworkLayer(context, threadpool);
        }
        return NETWORK_LAYER_INSTANCE;
    }

    public ListenableFuture<List<ScanResult>> wifiScan() {
        if (wifiController != null) {
            return wifiController.startWifiScan();
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


    public void cleanup() {}
}
