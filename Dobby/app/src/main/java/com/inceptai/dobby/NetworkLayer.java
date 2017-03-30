package com.inceptai.dobby;

import android.content.Context;

import com.inceptai.dobby.wifi.WifiAnalyzer;

/**
 * This class abstracts out the implementation details of all things 'network' related. The UI and
 * local bots would interact with this class to run tests, diagnostics etc. Also
 */

public class NetworkLayer {
    private Context context;
    private DobbyThreadpool threadpool;
    private WifiAnalyzer wifiAnalyzer;

    public NetworkLayer(Context context, DobbyThreadpool threadpool) {
        this.context = context;
        this.threadpool = threadpool;
    }

    void initialize() {
        wifiAnalyzer = WifiAnalyzer.create(context);
    }

    void runWifiAnalysis() {
        wifiAnalyzer.startWifiScan();
    }
}
