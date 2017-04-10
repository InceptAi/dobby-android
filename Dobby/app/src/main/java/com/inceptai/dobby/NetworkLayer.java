package com.inceptai.dobby;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.util.Log;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.dobby.model.IPLayerInfo;
import com.inceptai.dobby.model.PingStats;
import com.inceptai.dobby.ping.PingAnalyzer;
import com.inceptai.dobby.speedtest.BandwithTestCodes;
import com.inceptai.dobby.speedtest.NewBandwidthAnalyzer;
import com.inceptai.dobby.wifi.WifiAnalyzer;
import com.inceptai.dobby.wifi.WifiStats;

import java.util.List;

import javax.inject.Inject;

import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * This class abstracts out the implementation details of all things 'network' related. The UI and
 * local bots would interact with this class to run tests, diagnostics etc. Also
 */

public class NetworkLayer {
    private Context context;
    private DobbyThreadpool threadpool;
    private WifiAnalyzer wifiAnalyzer;
    private PingAnalyzer pingAnalyzer;
    private IPLayerInfo ipLayerInfo;

    @Inject
    NewBandwidthAnalyzer bandwidthAnalyzer;



    // Use Dagger to get a singleton instance of this class.
    public NetworkLayer(Context context, DobbyThreadpool threadpool) {
        this.context = context;
        this.threadpool = threadpool;
    }

    public void initialize() {
        wifiAnalyzer = WifiAnalyzer.create(context, threadpool);
        if (wifiAnalyzer != null) {
            ipLayerInfo = new IPLayerInfo(wifiAnalyzer.getDhcpInfo());
        }
        pingAnalyzer = PingAnalyzer.create(ipLayerInfo, null);
    }

    public ListenableFuture<List<ScanResult>> wifiScan() {
        return wifiAnalyzer.startWifiScan();
    }

    public WifiStats getWifiStats() {
        return wifiAnalyzer.getWifiStats();
    }

    public IPLayerInfo.IPLayerPingStats getPingStats() {
        return pingAnalyzer.performAllPingTests();
    }

    public PingStats getGatewayPingStats() {
        return pingAnalyzer.performGatewayPing();
    }

    public boolean checkWiFiConnectivity() throws IllegalStateException {
        Preconditions.checkNotNull(context);
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            throw new IllegalStateException("Cannot get ConnectivityManager to determine WiFi data connectivity");
        }
        final NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI && activeNetwork.isConnected()) {
            return true;
        }
        return false;
    }

    public void startBandwidthTest(NewBandwidthAnalyzer.ResultsCallback resultsCallback,
                                   @BandwithTestCodes.BandwidthTestMode int testMode) {

        bandwidthAnalyzer.registerCallback(resultsCallback);
        Log.i(TAG, "NetworkLayer: Going to start bandwidth test.");
        bandwidthAnalyzer.startBandwidthTestSafely(testMode);
    }

    public void cancelBandwidthTests() {
        bandwidthAnalyzer.cancelBandwidthTests();
    }


}
