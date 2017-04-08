package com.inceptai.dobby;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.util.Log;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.dobby.speedtest.BandwithTestCodes;
import com.inceptai.dobby.speedtest.NewBandwidthAnalyzer;
import com.inceptai.dobby.speedtest.PingAnalyzer;
import com.inceptai.dobby.utils.Utils;
import com.inceptai.dobby.wifi.WifiAnalyzer;

import java.util.List;

import javax.inject.Inject;

import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * This class abstracts out the implementation details of all things 'network' related. The UI and
 * local bots would interact with this class to run tests, diagnostics etc. Also
 */

public class NetworkLayer implements PingAnalyzer.ResultsCallback {
    private Context context;
    private DobbyThreadpool threadpool;
    private WifiAnalyzer wifiAnalyzer;
    private PingAnalyzer pingAnalyzer;
    private IPLayerInfo ipLayerInfonfo;

    @Inject
    NewBandwidthAnalyzer bandwidthAnalyzer;

    public static class IPLayerInfo {
        public String dns1;
        public String dns2;
        public String gateway;
        public int leaseDuration;
        public int netMask;
        public String serverAddress;

        public IPLayerInfo(DhcpInfo dhcpInfo) {
            this.dns1 = Utils.intToIp(dhcpInfo.dns1);
            this.dns2 = Utils.intToIp(dhcpInfo.dns2);
            this.gateway = Utils.intToIp(dhcpInfo.ipAddress);
            this.serverAddress = Utils.intToIp(dhcpInfo.serverAddress);
            this.netMask = dhcpInfo.netmask;
            this.leaseDuration = dhcpInfo.leaseDuration;
        }
    }

    // Use Dagger to get a singleton instance of this class.
    public NetworkLayer(Context context, DobbyThreadpool threadpool) {
        this.context = context;
        this.threadpool = threadpool;
        this.ipLayerInfonfo = null;
    }

    public void initialize() {
        wifiAnalyzer = WifiAnalyzer.create(context, threadpool);
        pingAnalyzer = PingAnalyzer.create(this);
        if (wifiAnalyzer != null) {
            ipLayerInfonfo = new IPLayerInfo(wifiAnalyzer.getDhcpInfo());
        }
    }

    public ListenableFuture<List<ScanResult>> wifiScan() {
        return wifiAnalyzer.startWifiScan();
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
        Log.i(TAG, "Going to start bandwidth test.");
        bandwidthAnalyzer.startBandwidthTestSafely(BandwithTestCodes.BandwidthTestMode.DOWNLOAD_AND_UPLOAD);
    }

    public void cancelBandwidthTests() {
        bandwidthAnalyzer.cancelBandwidthTests();
    }

    // Ping analyzer callbacks
    @Override
    public void onPingResults(PingAnalyzer.PingStats stats) {

    }

    @Override
    public void onPingError(String error) {

    }
}
