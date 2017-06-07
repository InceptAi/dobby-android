package com.inceptai.dobby;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.support.annotation.Nullable;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.dobby.ai.DataInterpreter;
import com.inceptai.dobby.connectivity.ConnectivityAnalyzer;
import com.inceptai.dobby.eventbus.DobbyEvent;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.model.DobbyWifiInfo;
import com.inceptai.dobby.model.IPLayerInfo;
import com.inceptai.dobby.model.PingStats;
import com.inceptai.dobby.ping.PingAnalyzer;
import com.inceptai.dobby.speedtest.BandwidthObserver;
import com.inceptai.dobby.speedtest.BandwidthTestCodes;
import com.inceptai.dobby.speedtest.NewBandwidthAnalyzer;
import com.inceptai.dobby.utils.DobbyLog;
import com.inceptai.dobby.utils.Utils;
import com.inceptai.dobby.wifi.WifiAnalyzer;
import com.inceptai.dobby.wifi.WifiState;

import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import static com.inceptai.dobby.connectivity.ConnectivityAnalyzerFactory.getConnecitivityAnalyzer;
import static com.inceptai.dobby.ping.PingAnalyzerFactory.getPingAnalyzer;
import static com.inceptai.dobby.wifi.WifiAnalyzerFactory.getWifiAnalyzer;

/**
 * This class abstracts out the implementation details of all things 'network' related. The UI and
 * local bots would interact with this class to run tests, diagnostics etc. Also
 */

public class NetworkLayer {
    private static final int MIN_PKT_LOSS_RATE_TO_RETRIGGER_PING_PERCENT = 50;
    private static final int MIN_CHECKS_CONNECTIIVITY = 5;
    private static final int MAX_AGE_GAP_TO_RETRIGGER_PING_MS = 120000; // 2 mins
    private static final int MAX_AGE_GAP_TO_RETRIGGER_WIFI_SCAN_MS = 120000; // 2 mins
    private static final boolean RETRIGGER_PING_AUTOMATICALLY = false;
    private static final boolean DISABLE_WIFI_SCAN = true; //We will disable wifi scan for this release

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
        DobbyLog.v("NL initialized");
    }

    public ListenableFuture<List<ScanResult>> wifiScan() {
        if (DISABLE_WIFI_SCAN) {
            return null;
        }
        //If scanning not disabled, continue
        if (getConnectivityAnalyzerInstance().isWifiInCaptivePortal()) {
            return null;
        }
        return getWifiAnalyzerInstance().startWifiScan(MAX_AGE_GAP_TO_RETRIGGER_WIFI_SCAN_MS);
    }


    public DataInterpreter.WifiGrade getCurrentWifiGrade() {
        HashMap<Integer, WifiState.ChannelInfo> channelMap = getWifiState().getChannelInfoMap();
        DobbyWifiInfo wifiInfo = getWifiState().getLinkInfo();
        DataInterpreter.WifiGrade wifiGrade = DataInterpreter.interpret(channelMap,
                wifiInfo,
                getWifiLinkMode(),
                getCurrentConnectivityMode());
        return wifiGrade;
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
        if (getConnectivityAnalyzerInstance().isWifiDisconnected()) {
            DobbyLog.v("NL: In start ping, bailing because wifi is not even connected (off/disconnected)");
            return null;
        }
        try {
            DobbyLog.v("NL Calling pingAnalyzer.schedulePingsIfNeeded");
            return getPingAnalyzerInstance().schedulePingsIfNeeded(MAX_AGE_GAP_TO_RETRIGGER_PING_MS);
        } catch (IllegalStateException e) {
            DobbyLog.v("Exception while scheduling ping tests: " + e);
        }
        return null;
    }

    @Nullable
    public ListenableFuture<PingStats> startGatewayDownloadLatencyTest() {
        if (getConnectivityAnalyzerInstance().isWifiDisconnected()) {
            DobbyLog.v("NL: In start ping, bailing because wifi is not even connected (off/disconnected)");
            return null;
        }
        try {
            DobbyLog.v("NL Calling pingAnalyzer.scheduleRouterDownloadLatencyIfNeeded");
            return getPingAnalyzerInstance().scheduleRouterDownloadLatencyIfNeeded(MAX_AGE_GAP_TO_RETRIGGER_PING_MS);
        } catch (IllegalStateException e) {
            DobbyLog.v("Exception while scheduling ping tests: " + e);
        }
        return null;
    }

    public void clearStatsCache() {
        getWifiAnalyzerInstance().clearWifiScanCache();
        getPingAnalyzerInstance().clearPingStatsCache();
    }

    public synchronized BandwidthObserver startBandwidthTest(final @BandwidthTestCodes.TestMode int mode) {
        if (bandwidthObserver != null && bandwidthObserver.testsRunning()) {
            DobbyLog.i("Bandwidth tests already running.");
            // We have an already running bandwidth operation.
            return bandwidthObserver;
        }

        if (!getConnectivityAnalyzerInstance().isWifiOnline()) {
            DobbyLog.w("Abandoning bandwidth test since wifi is offline.");
            eventBus.postEvent(DobbyEvent.EventType.BANDWIDTH_TEST_FAILED_WIFI_OFFLINE);
            return null;
        }

        bandwidthObserver = new BandwidthObserver(mode);
        getNewBandwidthAnalyzerInstance().registerCallback(bandwidthObserver);
        threadpool.submit(new Runnable() {
            @Override
            public void run() {
                getNewBandwidthAnalyzerInstance().startBandwidthTestSync(mode);
            }
        });
        sendBandwidthEvent(bandwidthObserver);
        return bandwidthObserver;
    }

    public synchronized boolean areBandwidthTestsRunning() {
        return bandwidthObserver != null && bandwidthObserver.testsRunning();
    }

    public synchronized void cancelBandwidthTests() {
        DobbyLog.v("NL cancel bw test");
        if (bandwidthObserver != null) {
            bandwidthObserver.onCancelled();
        }
        bandwidthObserver = null;
        DobbyLog.v("NL calling getNewBandwidthAnalyzer cancel");
        getNewBandwidthAnalyzerInstance().cancelBandwidthTests();
        DobbyLog.v("NL done with bw cancellation");
    }

    public HashMap<String, PingStats> getRecentIPLayerPingStats() {
        return getPingAnalyzerInstance().getRecentIPLayerPingStats();
    }

    public IPLayerInfo getIpLayerInfo() {
        return ipLayerInfo;
    }

    public DobbyWifiInfo getLinkInfo() { return getWifiAnalyzerInstance().getLinkInfo(); }

    public boolean isWifiOnline() {
        return getConnectivityAnalyzerInstance().isWifiOnline();
    }

    public String getCachedClientIspIfAvailable() {
        String clientIsp = Utils.EMPTY_STRING;
        if (getNewBandwidthAnalyzerInstance().getSpeedTestConfig() != null) {
            clientIsp = getNewBandwidthAnalyzerInstance().getSpeedTestConfig().clientConfig.isp;
        }
        return clientIsp;
    }

    public String getCachedExternalIpIfAvailable() {
        String externalIp = Utils.EMPTY_STRING;
        if (getNewBandwidthAnalyzerInstance().getSpeedTestConfig() != null) {
            externalIp = getNewBandwidthAnalyzerInstance().getSpeedTestConfig().clientConfig.ip;
        }
        return externalIp;
    }

    // Process events from eventbus. Do a thread switch to prevent deadlocks.
    @Subscribe
    public void listen(final DobbyEvent event) {
        threadpool.getExecutorServiceForNetworkLayer().submit(new Runnable() {
            @Override
            public void run() {
                listenOnNetworkLayerThread(event);
            }
        });
    }

    private void listenOnNetworkLayerThread(DobbyEvent event) {
        //Called on listening executor service -- need to delegate long running tasks to other thread
        DobbyLog.v("NL, Found Event: " + event.toString());
        switch (event.getEventType()) {
            case DobbyEvent.EventType.DHCP_INFO_AVAILABLE:
                ipLayerInfo = new IPLayerInfo(getWifiAnalyzerInstance().getDhcpInfo());
                getPingAnalyzerInstance().updateIPLayerInfo(ipLayerInfo);
                if (RETRIGGER_PING_AUTOMATICALLY) {
                    startPing();
                }
                break;
            case DobbyEvent.EventType.WIFI_INTERNET_CONNECTIVITY_ONLINE:
                if (RETRIGGER_PING_AUTOMATICALLY &&
                        getPingAnalyzerInstance().checkIfShouldRedoPingStats(
                                MAX_AGE_GAP_TO_RETRIGGER_PING_MS,
                                MIN_PKT_LOSS_RATE_TO_RETRIGGER_PING_PERCENT)) {
                    startPing();
                }
                break;
            case DobbyEvent.EventType.PING_INFO_AVAILABLE:
            case DobbyEvent.EventType.PING_FAILED:
                startGatewayDownloadLatencyTest();
                break;
            case DobbyEvent.EventType.WIFI_CONNECTED:
            case DobbyEvent.EventType.WIFI_STATE_ENABLED:
                wifiScan();
                break;
            case DobbyEvent.EventType.WIFI_INTERNET_CONNECTIVITY_CAPTIVE_PORTAL:
            case DobbyEvent.EventType.WIFI_INTERNET_CONNECTIVITY_OFFLINE:
                if (getNewBandwidthAnalyzerInstance().testsCurrentlyRunning()) {
                    threadpool.getExecutorService().submit(new Runnable() {
                        @Override
                        public void run() {
                            DobbyLog.i("NL Cancelling bandwidth tests since we are no longer online");
                            getNewBandwidthAnalyzerInstance().cancelBandwidthTests();
                        }
                    });
                }
                break;
        }
        //Passing this info to ConnectivityAnalyzer
        getNewBandwidthAnalyzerInstance().processDobbyBusEvents(event);
        getConnectivityAnalyzerInstance().processDobbyBusEvents(event);
    }

    public void cleanup() {
        //Unregister from Event bus
        if (eventBus != null) {
            eventBus.unregisterListener(this);
        }

        if (getWifiAnalyzerInstance() != null) {
            getWifiAnalyzerInstance().cleanup();
        }

        if (getPingAnalyzerInstance() != null) {
            getPingAnalyzerInstance().cleanup();
        }

        if (getNewBandwidthAnalyzerInstance() != null) {
            getNewBandwidthAnalyzerInstance().cleanup();
        }

        if (bandwidthObserver != null) {
            bandwidthObserver = null;
        }
    }

    private PingAnalyzer getPingAnalyzerInstance() {
        return getPingAnalyzer(ipLayerInfo, threadpool, eventBus);
    }

    private NewBandwidthAnalyzer getNewBandwidthAnalyzerInstance() {
        return bandwidthAnalyzer;
    }


    // Asynchronous post.
    private void sendBandwidthEvent(BandwidthObserver observer){
        eventBus.postEvent(DobbyEvent.EventType.BANDWIDTH_TEST_STARTING, observer);
    }
}
