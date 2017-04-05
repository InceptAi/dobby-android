package com.inceptai.dobby;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.support.annotation.Nullable;
import android.util.Log;

import com.inceptai.dobby.speedtest.BandwidthAnalyzer;
import com.inceptai.dobby.speedtest.BandwithTestCodes.BandwidthTestErrorCodes;
import com.inceptai.dobby.speedtest.BandwithTestCodes.BandwidthTestMode;
import com.inceptai.dobby.speedtest.PingAnalyzer;
import com.inceptai.dobby.speedtest.ServerInformation;
import com.inceptai.dobby.speedtest.SpeedTestConfig;
import com.inceptai.dobby.wifi.WifiAnalyzer;

import java.util.List;

import fr.bmartel.speedtest.SpeedTestReport;

import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * This class abstracts out the implementation details of all things 'network' related. The UI and
 * local bots would interact with this class to run tests, diagnostics etc. Also
 */

public class NetworkLayer implements  WifiAnalyzer.ResultsCallback, BandwidthAnalyzer.ResultsCallback, PingAnalyzer.ResultsCallback {
    private Context context;
    private DobbyThreadpool threadpool;
    private WifiAnalyzer wifiAnalyzer;
    private BandwidthAnalyzer bandwidthAnalyzer;
    private PingAnalyzer pingAnalyzer;

    /*
    PingAnalyzer pingAnalyzer = new PingAnalyzer(null);
    PingAnalyzer.PingStats routerPingStats = pingAnalyzer.pingAndReturnStats("192.168.1.1");
    */

    public NetworkLayer(Context context, DobbyThreadpool threadpool) {
        this.context = context;
        this.threadpool = threadpool;
    }

    void initialize() {
        wifiAnalyzer = WifiAnalyzer.create(context, this);
        bandwidthAnalyzer = BandwidthAnalyzer.create(this);
        pingAnalyzer = PingAnalyzer.create(this);
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


    //Error callback
    @Override
    public void onBandwidthTestError(@BandwidthTestMode int testMode,
                              @BandwidthTestErrorCodes int errorCode,
                              @Nullable String errorMessage) {}

    @Override
    public void onConfigFetch(SpeedTestConfig config) {

    }

    @Override
    public void onServerInformationFetch(ServerInformation serverInformation) {

    }

    @Override
    public void onTestRepeatIntervalReport(@BandwidthTestMode int testMode, SpeedTestReport report) {

    }

    @Override
    public void onTestProgress(@BandwidthTestMode int testMode, float percent, SpeedTestReport report) {

    }

    @Override
    public void onTestFinished(@BandwidthTestMode int testMode, SpeedTestReport report) {

    }

    @Override
    public void onBestServerSelected(ServerInformation.ServerDetails bestServer) {

    }

    @Override
    public void onClosestServersSelected(List<ServerInformation.ServerDetails> closestServers) {

    }

    @Override
    public void onRepeatTestFinished(@BandwidthTestMode int testMode, SpeedTestReport report) {

    }

    //Ping analyzer callbacks

    @Override
    public void onPingResults(PingAnalyzer.PingStats stats) {

    }

    @Override
    public void onPingError(String error) {

    }
}
