package com.inceptai.dobby;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.dobby.connectivity.ConnectivityAnalyzer;
import com.inceptai.dobby.eventbus.DobbyEvent;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.model.DobbyWifiInfo;
import com.inceptai.dobby.model.IPLayerInfo;
import com.inceptai.dobby.model.PingStats;
import com.inceptai.dobby.ping.PingAnalyzer;
import com.inceptai.dobby.speedtest.BandwidthObserver;
import com.inceptai.dobby.speedtest.BandwithTestCodes;
import com.inceptai.dobby.speedtest.NewBandwidthAnalyzer;
import com.inceptai.dobby.wifi.WifiAnalyzer;
import com.inceptai.dobby.wifi.WifiState;

import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import static com.inceptai.dobby.DobbyApplication.TAG;
import static com.inceptai.dobby.connectivity.ConnectivityAnalyzerFactory.getConnecitivityAnalyzer;
import static com.inceptai.dobby.ping.PingAnalyzerFactory.getPingAnalyzer;
import static com.inceptai.dobby.wifi.WifiAnalyzerFactory.getWifiAnalyzer;

/**
 * This class abstracts out the implementation details of all things 'network' related. The UI and
 * local bots would interact with this class to run tests, diagnostics etc. Also
 */

public class NetworkLayer {
    public static final int MIN_TIME_GAP_TO_RETRIGGER_PING_MS = 10000; //10sec
    public static final int MIN_PKT_LOSS_RATE_TO_RETRIGGER_PING_PERCENT = 50;
    public static final int MIN_CHECKS_CONNECTIIVITY = 3;

    private Context context;
    private DobbyThreadpool threadpool;
    private DobbyEventBus eventBus;
    private IPLayerInfo ipLayerInfo;

    @Nullable
    private BandwidthObserver bandwidthObserver;  // Represents any currently running b/w tests.

    @Inject
    NewBandwidthAnalyzer bandwidthAnalyzer;

    // Use Dagger to get a singleton instance of this class.
    public NetworkLayer(Context context, DobbyThreadpool threadpool, DobbyEventBus eventBus) {
        this.context = context;
        this.threadpool = threadpool;
        this.eventBus = eventBus;
    }

    public void initialize() {
        if (getWifiAnalyzerInstance() != null) {
            ipLayerInfo = new IPLayerInfo(getWifiAnalyzerInstance().getDhcpInfo());
        }
        eventBus.registerListener(this);
        getConnectivityAnalyzerInstance().rescheduleConnectivityTest(MIN_CHECKS_CONNECTIIVITY);
    }

    public ListenableFuture<List<ScanResult>> wifiScan() {
        return getWifiAnalyzerInstance().startWifiScan();
    }

    public WifiState getWifiState() {
        return getWifiAnalyzerInstance().getWifiState();
    }

    public HashMap<Integer, WifiState.ChannelInfo> getChannelStats() {
        return getWifiAnalyzerInstance().getChannelStats();
    }

    public HashMap<Integer, Double> getChannelContention() {
        HashMap<Integer, Double> channelContention = new HashMap<>();
        WifiState wifiState = getWifiAnalyzerInstance().getWifiState();
        if (wifiState != null) {
            channelContention = wifiState.getContentionInformation();
        }
        return channelContention;
    }

    @WifiState.WifiLinkMode
    public int getWifiLinkMode() {
        int problemMode = WifiState.WifiLinkMode.UNKNOWN;
        WifiState wifiState = getWifiAnalyzerInstance().getWifiState();
        if (wifiState != null) {
            problemMode = wifiState.getCurrentWifiProblemMode();
        }
        return problemMode;
    }

    @ConnectivityAnalyzer.WifiConnectivityMode
    public int getCurrentConnectivityMode() {
        int problemMode = ConnectivityAnalyzer.WifiConnectivityMode.UNKNOWN;
        if (getConnectivityAnalyzerInstance() != null) {
            problemMode = getConnectivityAnalyzerInstance().getWifiConnectivityMode();
        }
        return problemMode;
    }


    public WifiAnalyzer getWifiAnalyzerInstance() {
        return getWifiAnalyzer(context, threadpool, eventBus);
    }

    public ConnectivityAnalyzer getConnectivityAnalyzerInstance() {
        return getConnecitivityAnalyzer(context, threadpool, eventBus);
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


    public synchronized BandwidthObserver startBandwidthTest(final @BandwithTestCodes.TestMode int mode) {
        if (bandwidthObserver != null && bandwidthObserver.testsRunning()) {
            // We have an already running bandwidth operation.
            return bandwidthObserver;
        }

        if (!getConnectivityAnalyzerInstance().isWifiOnline()) {
            //TODO: Always check if bandwidth analyzer is null
            return null;
        }

        bandwidthObserver = new BandwidthObserver(mode);
        bandwidthAnalyzer.registerCallback(bandwidthObserver);
        threadpool.submit(new Runnable() {
            @Override
            public void run() {
                bandwidthAnalyzer.startBandwidthTestSync(mode);
            }
        });
        sendBandwidthEvent(bandwidthObserver);
        return bandwidthObserver;
    }

    public synchronized boolean areBandwidthTestsRunning() {
        return bandwidthObserver != null && bandwidthObserver.testsRunning();
    }

    public synchronized void cancelBandwidthTests() {
        bandwidthAnalyzer.cancelBandwidthTests();
        bandwidthObserver.onCancelled();
        bandwidthObserver = null;
    }

    public HashMap<String, PingStats> getRecentIPLayerPingStats() {
        return getPingAnalyzerInstance().getRecentIPLayerPingStats();
    }

    public IPLayerInfo getIpLayerInfo() {
        return ipLayerInfo;
    }

    public DobbyWifiInfo getLinkInfo() { return getWifiAnalyzerInstance().getLinkInfo(); }

    // Process events from eventbus
    @Subscribe
    public void listen(DobbyEvent event) {
        Log.v(TAG, "NL, Found Event: " + event.toString());
        if (event.getEventType() == DobbyEvent.EventType.DHCP_INFO_AVAILABLE) {
            ipLayerInfo = new IPLayerInfo(getWifiAnalyzerInstance().getDhcpInfo());
            if (ipLayerInfo != null) {
                getPingAnalyzerInstance().updateIPLayerInfo(ipLayerInfo);
                startPing();
            }
        } else if (event.getEventType() == DobbyEvent.EventType.WIFI_INTERNET_CONNECTIVITY_ONLINE) {
            if (getPingAnalyzerInstance().checkIfShouldRedoPingStats(MIN_TIME_GAP_TO_RETRIGGER_PING_MS, MIN_PKT_LOSS_RATE_TO_RETRIGGER_PING_PERCENT)) {
                startPing();
            }
        } else if (event.getEventType() == DobbyEvent.EventType.PING_INFO_AVAILABLE || event.getEventType() == DobbyEvent.EventType.PING_FAILED) {
            //TODO: Do we need to autotrigger gatewayDownloadLatencyTest here ??
            startGatewayDownloadLatencyTest();
        }
    }

    public void cleanup() {
        if (getWifiAnalyzerInstance() != null) {
            getWifiAnalyzerInstance().cleanup();
        }
        if (bandwidthAnalyzer != null) {
            bandwidthAnalyzer.cleanup();
        }
        if (bandwidthObserver != null) {
            bandwidthObserver = null;
        }
    }

    private PingAnalyzer getPingAnalyzerInstance() {
        return getPingAnalyzer(ipLayerInfo, threadpool, eventBus);
    }

    private void sendBandwidthEvent(BandwidthObserver observer){
        DobbyEvent event = new DobbyEvent(DobbyEvent.EventType.BANDWIDTH_TEST_STARTING);
        event.setPayload(observer);
        eventBus.postEvent(event);
    }

}
