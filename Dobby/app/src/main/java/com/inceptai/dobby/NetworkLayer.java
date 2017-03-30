package com.inceptai.dobby;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.util.Log;

import com.inceptai.dobby.wifi.WifiAnalyzer;

import java.util.List;

import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * This class abstracts out the implementation details of all things 'network' related. The UI and
 * local bots would interact with this class to run tests, diagnostics etc. Also
 */

public class NetworkLayer implements  WifiAnalyzer.ResultsCallback{
    private Context context;
    private DobbyThreadpool threadpool;
    private WifiAnalyzer wifiAnalyzer;

    public NetworkLayer(Context context, DobbyThreadpool threadpool) {
        this.context = context;
        this.threadpool = threadpool;
    }

    void initialize() {
        wifiAnalyzer = WifiAnalyzer.create(context, this);
    }

    void runWifiAnalysis() {
        wifiAnalyzer.startWifiScan();
    }

    @Override
    public void onWifiScan(List<ScanResult> scanResults) {
        for (ScanResult result : scanResults) {
            Log.i(TAG, "Wifi scan results:" + result.toString());
        }

        // Thread switch logic.
        threadpool.submit(new Runnable() {
            @Override
            public void run() {
                // Post scan results to caller if needed.
            }
        });
    }
}
