package com.inceptai.dobby.ping;

import android.support.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.eventbus.DobbyEvent;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.model.IPLayerInfo;
import com.inceptai.dobby.model.PingStats;
import com.inceptai.dobby.utils.DobbyLog;
import com.inceptai.dobby.utils.Utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.inceptai.dobby.speedtest.BestServerSelector.MAX_STRING_LENGTH;

/**
 * Created by vivek on 4/8/17.
 */

public class PingAnalyzer {
    public static final int MAX_ASYNC_PING = 10;
    public static final int MAX_GATEWAY_DOWNLOAD_TRIES = 5;
    public static final double MAX_LATENCY_GATEWAY_MS = 60000;

    protected IPLayerInfo ipLayerInfo;
    protected HashMap<String, PingStats> ipLayerPingStats;
    protected PingStats gatewayDownloadLatencyTestStats;
    protected DobbyThreadpool dobbyThreadpool;
    protected DobbyEventBus eventBus;
    protected AtomicBoolean pingInProgress = new AtomicBoolean(false);

    private PingAction pingAction;
    private PingActionListener pingActionListener;
    private ConcurrentHashMap<String, Boolean> pingsInFlight;
    private SettableFuture<HashMap<String, PingStats>> pingResultsFuture;
    private SettableFuture<PingStats> gatewayDownloadTestFuture;


    protected PingAnalyzer(IPLayerInfo ipLayerInfo, DobbyThreadpool dobbyThreadpool, DobbyEventBus eventBus) {
        this.ipLayerInfo = ipLayerInfo;
        this.dobbyThreadpool = dobbyThreadpool;
        this.eventBus = eventBus;
        pingActionListener = new PingActionListener();
        pingAction = PingAction.create(pingActionListener);
        pingsInFlight = new ConcurrentHashMap<String, Boolean>();
        pingInProgress = new AtomicBoolean(false);
        ipLayerPingStats = new HashMap<>();
        initializePingStats(ipLayerInfo);
        gatewayDownloadLatencyTestStats = new PingStats(ipLayerInfo.gateway);
    }

    private void initializePingStats(IPLayerInfo ipLayerInfo) {
        if (ipLayerInfo == null) {
            return;
        }
        ipLayerPingStats.put(ipLayerInfo.gateway, new PingStats(ipLayerInfo.gateway));
        ipLayerPingStats.put(ipLayerInfo.dns1, new PingStats(ipLayerInfo.dns1));
        ipLayerPingStats.put(ipLayerInfo.dns2, new PingStats(ipLayerInfo.dns2));
        ipLayerPingStats.put(ipLayerInfo.referenceExternalAddress1,
                new PingStats(ipLayerInfo.referenceExternalAddress1));
        ipLayerPingStats.put(ipLayerInfo.referenceExternalAddress2,
                new PingStats(ipLayerInfo.referenceExternalAddress2));
        ipLayerPingStats.put(ipLayerInfo.publicDns1,
                new PingStats(ipLayerInfo.publicDns1));
        ipLayerPingStats.put(ipLayerInfo.publicDns2,
                new PingStats(ipLayerInfo.publicDns2));
    }

    /**
     * Factory constructor for creating an instance
     * @param ipLayerInfo
     * @return
     */
    @Nullable
    public static PingAnalyzer create(IPLayerInfo ipLayerInfo,
                                      DobbyThreadpool dobbyThreadpool,
                                      DobbyEventBus eventBus) {
        Preconditions.checkNotNull(ipLayerInfo);
        Preconditions.checkNotNull(dobbyThreadpool);
        return new PingAnalyzer(ipLayerInfo, dobbyThreadpool, eventBus);
    }

    // Called in order to cleanup any held resources.
    public void cleanup() {
    }

    public ListenableFuture<HashMap<String, PingStats>> scheduleEssentialPingTestsAsyncSafely(int maxAgeToReTriggerPingMs) throws IllegalStateException {
        DobbyLog.v("In real ping Analyzer");
        final int maxAge = maxAgeToReTriggerPingMs;
        if (pingResultsFuture != null && !pingResultsFuture.isDone()) {
            AsyncFunction<HashMap<String, PingStats>, HashMap<String, PingStats>> redoPing = new
                    AsyncFunction<HashMap<String, PingStats>, HashMap<String, PingStats>>() {
                        @Override
                        public ListenableFuture<HashMap<String, PingStats>> apply(HashMap<String, PingStats> input) throws Exception {
                            return scheduleEssentialPingTestsAsync(maxAge);
                        }
                    };
            ListenableFuture<HashMap<String, PingStats>> newPingResultsFuture = Futures.transformAsync(pingResultsFuture, redoPing);
            return newPingResultsFuture;
        } else {
            return scheduleEssentialPingTestsAsync(maxAge);
        }
    }

    public HashMap<String, PingStats> getRecentIPLayerPingStats() {
        return ipLayerPingStats;
    }

    public boolean checkIfShouldRedoPingStats(int minGapToRetriggerPing, int lossRateToTriggerPing) {
        boolean redoPing = false;
        long maxTimeUpdatedAt = 0;
        for (PingStats pingStats : ipLayerPingStats.values()) {
            maxTimeUpdatedAt = Math.max(maxTimeUpdatedAt, pingStats.updatedAt);
            if (pingStats.lossRatePercent == -1 || pingStats.lossRatePercent > lossRateToTriggerPing) {
                redoPing = true;
                break;
            }
        }
        long gap = System.currentTimeMillis() - maxTimeUpdatedAt;
        if (gap > minGapToRetriggerPing) {
            redoPing = true;
        }
        return redoPing;
    }

    public void updateIPLayerInfo(IPLayerInfo updatedInfo) {
        this.ipLayerInfo = updatedInfo;
    }

    public void clearPingStatsCache() {
        ipLayerPingStats.clear();
        gatewayDownloadLatencyTestStats.setUpdatedAt(0);
        initializePingStats(ipLayerInfo);
    }

    private void schedulePingAndReturn(String[] pingAddressList) {
        final String[] pingAddressListFinal = pingAddressList;
        dobbyThreadpool.submit(new Runnable() {
            @Override
            public void run() {
                pingAction.pingAndReturnStatsList(pingAddressListFinal);
            }
        });
    }

    private void scheduleMultipleAsyncPingAndReturn(String[] pingAddressList) {
        for (String address: pingAddressList) {
            String[] list = {address};
            schedulePingAndReturn(list);
        }
    }

    private boolean checkIfAllPingsDone() {
        boolean pingsAllDone = true;
        for (Boolean value : pingsInFlight.values()) {
            pingsAllDone = pingsAllDone && !value;
        }
        return pingsAllDone;
    }

    private boolean checkIfPingStatsIsFresh(PingStats pingStats, int maxAgeForFreshness) {
        if (pingStats.getUpdatedAt() > 0 &&
                (System.currentTimeMillis() - pingStats.getUpdatedAt() < maxAgeForFreshness)) {
            return true;
        }
        return false;
    }

    private String[] selectPingAddressesBasedOnFreshnessOfCachedResults(int maxAgeToReTriggerPingMs) {
        HashMap<String, Boolean> addressToPingMap = new HashMap<>();
        String[] addressList = {ipLayerInfo.gateway,
                ipLayerInfo.dns1, ipLayerInfo.referenceExternalAddress1, ipLayerInfo.publicDns1};

        //By default ping everyone
        for (String address: addressList) {
            addressToPingMap.put(address, true);
        }
        //Mark ones with fresh enough timestamp as false
        for (PingStats stats: ipLayerPingStats.values()) {
            if (checkIfPingStatsIsFresh(stats, maxAgeToReTriggerPingMs)) {
                if (addressToPingMap.get(stats.ipAddress) != null) {
                    addressToPingMap.put(stats.ipAddress, false);
                }
            }
        }
        List<String> addressListToPing = new ArrayList<>();
        //Get the list of ips to ping
        for (HashMap.Entry<String, Boolean> entry : addressToPingMap.entrySet()) {
            if (entry.getValue()) {
                addressListToPing.add(entry.getKey());
            }
        }
        return addressListToPing.toArray(new String[addressListToPing.size()]);
    }

    protected ListenableFuture<HashMap<String, PingStats>> scheduleEssentialPingTestsAsync(int maxAgeToReTriggerPingMs) throws IllegalStateException {
        DobbyLog.v("PA: In scheduleEssentialPingTestsAsync");
        if (ipLayerInfo == null) {
            // Try to get new iplayerInfo
            eventBus.postEvent(new DobbyEvent(DobbyEvent.EventType.PING_FAILED));
            throw new IllegalStateException("Cannot schedule pings when iplayerInfo is null or own IP is 0.0.0.0");
        }
        //Get list of addresses to ping
        String[] addressList = selectPingAddressesBasedOnFreshnessOfCachedResults(maxAgeToReTriggerPingMs);
        pingResultsFuture = SettableFuture.create();
        if (addressList.length > 0) {
            DobbyLog.v("PA: Starting ping for count: " + addressList.length);
            eventBus.postEvent(new DobbyEvent(DobbyEvent.EventType.PING_STARTED));
            pingInProgress.set(true);
            pingsInFlight.clear();
            for (String address: addressList) {
                pingsInFlight.put(address, true);
            }
            scheduleMultipleAsyncPingAndReturn(addressList);
        } else {
            //No need to ping, just set the future and return
            DobbyLog.v("PA: Returning cached results");
            pingResultsFuture.set(ipLayerPingStats);
        }
        return pingResultsFuture;
    }


    private class PingActionListener implements PingAction.ResultsCallback {

        @Override
        public void onFinish(HashMap<String, PingStats> pingStatsHashMap) {
            if (pingStatsHashMap == null) {
                return;
            }
            for (String key : ipLayerPingStats.keySet()) {
                PingStats returnedValue = pingStatsHashMap.get(key);
                if (returnedValue != null) {
                    ipLayerPingStats.put(key,returnedValue);
                    pingsInFlight.put(key, false);
                }
            }
            if (checkIfAllPingsDone() && pingInProgress.get()) {
                //Return the results here
                pingInProgress.set(false);
                if (pingResultsFuture != null) {
                    pingResultsFuture.set(ipLayerPingStats);
                    DobbyLog.v("IP Layer Ping Stats " + ipLayerPingStats.toString());
                    eventBus.postEvent(DobbyEvent.EventType.PING_INFO_AVAILABLE);
                }
            }
        }

        @Override
        public void onError(@PingAction.PingErrorCode int errorType, String[] addressList, String errorMessage) {
            eventBus.postEvent(new DobbyEvent(DobbyEvent.EventType.PING_FAILED));
            for (String address: addressList) {
                pingsInFlight.put(address, false);
            }
            pingInProgress.set(false);
        }
    }



    //Perform download tests with router
    private void performGatewayDownloadTest() {
        PingStats downloadLatencyStats = new PingStats(ipLayerInfo.gateway);
        if (ipLayerInfo.gateway == null || ipLayerInfo.gateway.equals("0.0.0.0")) {
            return;
        }

        String gatewayURLToDownload = "http://" + ipLayerInfo.gateway;
        List<Double> latencyMeasurementsMs = new ArrayList<>();
        for (int i = 0; i < MAX_GATEWAY_DOWNLOAD_TRIES; i++) {
            String dataFromUrl = Utils.EMPTY_STRING;
            long startTime = System.currentTimeMillis();
            try {
                dataFromUrl = Utils.getDataFromUrl(gatewayURLToDownload, MAX_STRING_LENGTH);
                if (dataFromUrl.length() > 0) {
                    latencyMeasurementsMs.add(Double.valueOf(System.currentTimeMillis() - startTime));
                }
            } catch (Utils.HTTPReturnCodeException e) {
                DobbyLog.v("HTTP Return code: " + e.httpReturnCode);
                latencyMeasurementsMs.add(Double.valueOf(System.currentTimeMillis() - startTime));
            } catch (IOException e) {
                String errorString = "Exception while performing latencyMs test: " + e;
                DobbyLog.v(errorString);
                //latencyMeasurementsMs.add(MAX_LATENCY_GATEWAY_MS);
            }
        }

        //Compute avg, min, max
        if (latencyMeasurementsMs.size() > 0) {
            try {
                Collections.sort(latencyMeasurementsMs);
                downloadLatencyStats.avgLatencyMs = Utils.computePercentileFromSortedList(latencyMeasurementsMs, 50);
                downloadLatencyStats.maxLatencyMs = Utils.computePercentileFromSortedList(latencyMeasurementsMs, 100);
                downloadLatencyStats.minLatencyMs = Utils.computePercentileFromSortedList(latencyMeasurementsMs, 0);
                downloadLatencyStats.updatedAt = System.currentTimeMillis();
            } catch (IllegalArgumentException e) {
                String errorString = "Got exception while computing average: " + e;
                DobbyLog.v(errorString);
            }
        }
        gatewayDownloadLatencyTestStats = downloadLatencyStats;
        gatewayDownloadTestFuture.set(gatewayDownloadLatencyTestStats);
        DobbyLog.v("GW server latency is : " + gatewayDownloadLatencyTestStats.toString());
    }

    public ListenableFuture<PingStats> scheduleRouterDownloadLatencyTestSafely(int maxAgeToRetriggerTest) throws IllegalStateException {
        final int maxAge = maxAgeToRetriggerTest;
        if (gatewayDownloadTestFuture != null && !gatewayDownloadTestFuture.isDone()) {
            AsyncFunction<PingStats, PingStats> redoDownloadLatencyTest = new
                    AsyncFunction<PingStats, PingStats>() {
                        @Override
                        public ListenableFuture<PingStats> apply(PingStats input) throws Exception {
                            return scheduleGatewayDownloadLatencyTest(maxAge);
                        }
                    };
            return Futures.transformAsync(gatewayDownloadTestFuture, redoDownloadLatencyTest);
        } else {
            return scheduleGatewayDownloadLatencyTest(maxAge);
        }
    }

    protected ListenableFuture<PingStats> scheduleGatewayDownloadLatencyTest(int maxAgeToRetriggerTest) throws IllegalStateException {
        gatewayDownloadTestFuture = SettableFuture.create();
        if (checkIfPingStatsIsFresh(gatewayDownloadLatencyTestStats, maxAgeToRetriggerTest)) {
            //No need to run tests, just return the current result.
            gatewayDownloadTestFuture.set(gatewayDownloadLatencyTestStats);
        } else {
            dobbyThreadpool.submit(new Runnable() {
                @Override
                public void run() {
                    performGatewayDownloadTest();
                }
            });
        }
        return gatewayDownloadTestFuture;
    }
}
