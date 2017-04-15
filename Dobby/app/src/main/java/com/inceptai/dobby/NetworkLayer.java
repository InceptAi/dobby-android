package com.inceptai.dobby;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.dobby.eventbus.DobbyEvent;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.model.IPLayerInfo;
import com.inceptai.dobby.model.PingStats;
import com.inceptai.dobby.ping.PingAnalyzer;
import com.inceptai.dobby.speedtest.BandwithTestCodes;
import com.inceptai.dobby.speedtest.NewBandwidthAnalyzer;
import com.inceptai.dobby.wifi.ConnectivityAnalyzer;
import com.inceptai.dobby.wifi.WifiAnalyzer;
import com.inceptai.dobby.wifi.WifiStats;

import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import static com.inceptai.dobby.DobbyApplication.TAG;
import static com.inceptai.dobby.ping.PingAnalyzerFactory.getPingAnalyzer;

/**
 * This class abstracts out the implementation details of all things 'network' related. The UI and
 * local bots would interact with this class to run tests, diagnostics etc. Also
 */

public class NetworkLayer {
    public static final int MIN_TIME_GAP_TO_RETRIGGER_PING_MS = 10000; //10sec
    public static final int MIN_PKT_LOSS_RATE_TO_RETRIGGER_PING_PERCENT = 50;

    private Context context;
    private DobbyThreadpool threadpool;
    private DobbyEventBus eventBus;
    private WifiAnalyzer wifiAnalyzer;
    //private PingAnalyzer pingAnalyzer;
    private IPLayerInfo ipLayerInfo;
    private ConnectivityAnalyzer connectivityAnalyzer;

    @Inject
    NewBandwidthAnalyzer bandwidthAnalyzer;

    // Use Dagger to get a singleton instance of this class.
    public NetworkLayer(Context context, DobbyThreadpool threadpool, DobbyEventBus eventBus) {
        this.context = context;
        this.threadpool = threadpool;
        this.eventBus = eventBus;
    }

    public void initialize() {
        wifiAnalyzer = WifiAnalyzer.create(context, threadpool, eventBus);
        if (wifiAnalyzer != null) {
            ipLayerInfo = new IPLayerInfo(wifiAnalyzer.getDhcpInfo());
        }
        //pingAnalyzer = PingAnalyzerFactory.create(ipLayerInfo, threadpool, eventBus);
        connectivityAnalyzer = ConnectivityAnalyzer.create(context, threadpool, eventBus);
        eventBus.registerListener(this);
    }

    public ListenableFuture<List<ScanResult>> wifiScan() {
        return wifiAnalyzer.startWifiScan();
    }

    public WifiStats getWifiStats() {
        return wifiAnalyzer.getWifiStats();
    }

    private PingAnalyzer getPingAnalyzerInstance() {
        return getPingAnalyzer(ipLayerInfo, threadpool, eventBus);
    }

    @Nullable
    public ListenableFuture<HashMap<String, PingStats>> startPing() {
        try {
            return getPingAnalyzerInstance().scheduleEssentialPingTestsAsyncSafely();
        } catch (IllegalStateException e) {
            Log.v(TAG, "Exception while scheduling ping tests: " + e);
        }
        return null;
    }

    @Nullable
    public ListenableFuture<PingStats> startGatewayDownloadLatencyTest() {
        try {
            return getPingAnalyzerInstance().scheduleRouterDownloadLatencyTestSafely();
        } catch (IllegalStateException e) {
            Log.v(TAG, "Exception while scheduling ping tests: " + e);
        }
        return null;
    }

    public boolean startBandwidthTest(NewBandwidthAnalyzer.ResultsCallback resultsCallback,
                                   @BandwithTestCodes.BandwidthTestMode int testMode) {
        if (connectivityAnalyzer.isWifiOnline()) {
            bandwidthAnalyzer.registerCallback(resultsCallback);
            Log.i(TAG, "NetworkLayer: Going to start bandwidth test.");
            bandwidthAnalyzer.startBandwidthTestSafely(testMode);
            return true;
        } else {
            Log.i(TAG, "NetworkLayer: Wifi is offline, so cannot run bandwidth tests");
            return false;
        }
    }

    public void cancelBandwidthTests() {
        bandwidthAnalyzer.cancelBandwidthTests();
    }

    public HashMap<String, PingStats> getRecentIPLayerPingStats() {
        return getPingAnalyzerInstance().getRecentIPLayerPingStats();
    }

    public IPLayerInfo getIpLayerInfo() {
        return ipLayerInfo;
    }

    //Process events from eventbus

    @Subscribe
    public void listen(DobbyEvent event) {
        if (event.getLastEventType() == DobbyEvent.EventType.DHCP_INFO_AVAILABLE) {
            ipLayerInfo = new IPLayerInfo(wifiAnalyzer.getDhcpInfo());
            if (ipLayerInfo != null) {
                pingAnalyzer.updateIPLayerInfo(ipLayerInfo);
                startPing();
            }
        } else if (event.getLastEventType() == DobbyEvent.EventType.WIFI_INTERNET_CONNECTIVITY_ONLINE) {
            if (getPingAnalyzerInstance().checkIfShouldRedoPingStats(MIN_TIME_GAP_TO_RETRIGGER_PING_MS, MIN_PKT_LOSS_RATE_TO_RETRIGGER_PING_PERCENT)) {
                startPing();
            }
        } else if (event.getLastEventType() == DobbyEvent.EventType.PING_INFO_AVAILABLE || event.getLastEventType() == DobbyEvent.EventType.PING_FAILED) {
            startGatewayDownloadLatencyTest();
        }
    }

    public void cleanup() {
        if (wifiAnalyzer != null) {
            wifiAnalyzer.cleanup();
        }
        if (bandwidthAnalyzer != null) {
            bandwidthAnalyzer.cleanup();
        }
    }
}
