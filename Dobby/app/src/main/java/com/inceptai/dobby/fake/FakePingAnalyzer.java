package com.inceptai.dobby.fake;

import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.eventbus.DobbyEvent;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.model.IPLayerInfo;
import com.inceptai.dobby.model.PingStats;
import com.inceptai.dobby.ping.PingAnalyzer;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.inceptai.dobby.DobbyApplication.TAG;
import static com.inceptai.dobby.fake.FakePingAnalyzer.PingStatsMode.DEFAULT_WORKING_STATE;
import static com.inceptai.dobby.fake.FakePingAnalyzer.PingStatsMode.DNS_SLOW;
import static com.inceptai.dobby.fake.FakePingAnalyzer.PingStatsMode.DNS_UNREACHABLE;
import static com.inceptai.dobby.fake.FakePingAnalyzer.PingStatsMode.EXTERNAL_SERVER_SLOW;
import static com.inceptai.dobby.fake.FakePingAnalyzer.PingStatsMode.EXTERNAL_SERVER_UNREACHABLE;
import static com.inceptai.dobby.fake.FakePingAnalyzer.PingStatsMode.GATEWAY_SLOW;
import static com.inceptai.dobby.fake.FakePingAnalyzer.PingStatsMode.GATEWAY_UNREACHABLE;

/**
 * Created by vivek on 4/8/17.
 */

public class FakePingAnalyzer extends PingAnalyzer {
    public static final int PING_LATENCY_MS = 5000;
    private static final int RANDOM_SEED = 100;

    //Additional variables
    @FakePingAnalyzer.PingStatsMode
    public static int pingStatsMode = PingStatsMode.DEFAULT_WORKING_STATE;
    private ListenableFuture<HashMap<String, PingStats>> fakePingResultsFuture;
    private Random random;


    private FakePingAnalyzer(IPLayerInfo ipLayerInfo, DobbyThreadpool dobbyThreadpool, DobbyEventBus eventBus) {
        super(ipLayerInfo, dobbyThreadpool, eventBus);
        random = new Random(RANDOM_SEED);
    }

    private void initializePingStats(IPLayerInfo ipLayerInfo) {
        ipLayerPingStats.put(ipLayerInfo.gateway, new PingStats(ipLayerInfo.gateway));
        ipLayerPingStats.put(ipLayerInfo.dns1, new PingStats(ipLayerInfo.dns1));
        ipLayerPingStats.put(ipLayerInfo.dns2, new PingStats(ipLayerInfo.dns2));
        ipLayerPingStats.put(ipLayerInfo.referenceExternalAddress1,
                new PingStats(ipLayerInfo.referenceExternalAddress1));
        ipLayerPingStats.put(ipLayerInfo.referenceExternalAddress2,
                new PingStats(ipLayerInfo.referenceExternalAddress2));
    }

    /**
     * Factory constructor for creating an instance
     * @param ipLayerInfo
     * @return
     */
    @Nullable
    public static FakePingAnalyzer create(IPLayerInfo ipLayerInfo,
                                          DobbyThreadpool dobbyThreadpool,
                                          DobbyEventBus eventBus) {
        Preconditions.checkNotNull(ipLayerInfo);
        Preconditions.checkNotNull(dobbyThreadpool);
        return new FakePingAnalyzer(ipLayerInfo, dobbyThreadpool, eventBus);
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PingStatsMode.DEFAULT_WORKING_STATE, PingStatsMode.GATEWAY_UNREACHABLE,
            PingStatsMode.GATEWAY_SLOW, PingStatsMode.EXTERNAL_SERVER_UNREACHABLE,
            PingStatsMode.EXTERNAL_SERVER_SLOW, PingStatsMode.DNS_UNREACHABLE,
            PingStatsMode.DNS_SLOW, PingStatsMode.MAX_STATES})
    public @interface PingStatsMode {
        int DEFAULT_WORKING_STATE = 0;
        int GATEWAY_UNREACHABLE = 1;
        int GATEWAY_SLOW = 2;
        int EXTERNAL_SERVER_UNREACHABLE = 3;
        int EXTERNAL_SERVER_SLOW = 4;
        int DNS_UNREACHABLE = 5;
        int DNS_SLOW = 6;
        int MAX_STATES = 7;
    }

    public static String getPingStatsModeName(@PingStatsMode int mode) {
        switch (mode) {
            case DEFAULT_WORKING_STATE:
                return "Default";
            case GATEWAY_UNREACHABLE:
                return "GW unreachable";
            case GATEWAY_SLOW:
                return "GW slow";
            case EXTERNAL_SERVER_UNREACHABLE:
                return "Ext server unreachable";
            case EXTERNAL_SERVER_SLOW:
                return "Ext server slow";
            case DNS_UNREACHABLE:
                return "DNS unreachable";
            case DNS_SLOW:
                return "DNS slow";
            default:
                return "Unknown";
        }
    }

    public static class FakePingConfig {
        @Retention(RetentionPolicy.SOURCE)
        @IntDef({LatencyRangeMs.MAX, LatencyRangeMs.VERY_HIGH, LatencyRangeMs.HIGH, LatencyRangeMs.MEDIUM,
                LatencyRangeMs.LOW, LatencyRangeMs.VERY_LOW, LatencyRangeMs.UNDEFINED})
        public @interface LatencyRangeMs {
            int MAX = 1000;
            int VERY_HIGH = 200;
            int HIGH = 100;
            int MEDIUM = 50;
            int LOW = 30;
            int VERY_LOW = 10;
            int UNDEFINED = -1;
        }

        @Retention(RetentionPolicy.SOURCE)
        @IntDef({LossRangePercent.UNREACHABLE, LossRangePercent.HIGH, LossRangePercent.LOW})
        public @interface LossRangePercent {
            int UNREACHABLE = 100;
            int HIGH = 50;
            int LOW = 10;
        }


        @FakePingConfig.LatencyRangeMs public static int gatewayLatencyRangeMs, dns1LatencyRangeMs,
                dns2LatencyRangeMs, externalServer1LatencyRangeMs, externalServer2LatencyRangeMs;
        @FakePingConfig.LossRangePercent public static int gatewayLossRangePercent, dns1LossRangePercent,
                dns2LossRangePercent, externalServer1LossRangePercent, externalServer2LossRangePercent;

        private static void InitializeToWorkingState() {
            gatewayLatencyRangeMs = LatencyRangeMs.VERY_LOW;
            dns1LatencyRangeMs = LatencyRangeMs.VERY_LOW;
            dns2LatencyRangeMs = LatencyRangeMs.VERY_LOW;
            externalServer1LatencyRangeMs = LatencyRangeMs.LOW;
            externalServer2LatencyRangeMs = LatencyRangeMs.LOW;

            gatewayLossRangePercent = LossRangePercent.LOW;
            dns1LossRangePercent = LossRangePercent.LOW;
            dns2LossRangePercent = LossRangePercent.LOW;
            externalServer1LossRangePercent = LossRangePercent.LOW;
            externalServer2LossRangePercent = LossRangePercent.LOW;
        }

        private static void InitializeAllLatenciesToUndefined() {
            gatewayLatencyRangeMs = LatencyRangeMs.UNDEFINED;
            dns1LatencyRangeMs = LatencyRangeMs.UNDEFINED;
            dns2LatencyRangeMs = LatencyRangeMs.UNDEFINED;
            externalServer1LatencyRangeMs = LatencyRangeMs.UNDEFINED;
            externalServer2LatencyRangeMs = LatencyRangeMs.UNDEFINED;
        }

        private static void InitializeAllLossRatesPercentToUnreachable() {
            gatewayLossRangePercent = LossRangePercent.UNREACHABLE;
            dns1LossRangePercent = LossRangePercent.UNREACHABLE;
            dns2LossRangePercent = LossRangePercent.UNREACHABLE;
            externalServer1LossRangePercent = LossRangePercent.UNREACHABLE;
            externalServer2LossRangePercent = LossRangePercent.UNREACHABLE;
        }

        FakePingConfig(@PingStatsMode int mode) {
            InitializeToWorkingState();
            switch (mode) {
                case PingStatsMode.GATEWAY_UNREACHABLE:
                    //Default config
                    InitializeAllLatenciesToUndefined();
                    InitializeAllLossRatesPercentToUnreachable();
                    break;
                case PingStatsMode.GATEWAY_SLOW:
                    //Default config
                    gatewayLatencyRangeMs = LatencyRangeMs.HIGH;
                    dns1LatencyRangeMs = LatencyRangeMs.HIGH;
                    dns2LatencyRangeMs = LatencyRangeMs.HIGH;
                    externalServer1LatencyRangeMs = LatencyRangeMs.HIGH;
                    externalServer2LatencyRangeMs = LatencyRangeMs.HIGH;
                    break;
                case PingStatsMode.DNS_UNREACHABLE:
                    InitializeAllLatenciesToUndefined();
                    gatewayLatencyRangeMs = LatencyRangeMs.LOW;
                    InitializeAllLossRatesPercentToUnreachable();
                    gatewayLossRangePercent = LossRangePercent.LOW;
                    break;
                case PingStatsMode.DNS_SLOW:
                    gatewayLatencyRangeMs = LatencyRangeMs.LOW;
                    dns1LatencyRangeMs = LatencyRangeMs.HIGH;
                    dns2LatencyRangeMs = LatencyRangeMs.HIGH;
                    externalServer1LatencyRangeMs = LatencyRangeMs.HIGH;
                    externalServer2LatencyRangeMs = LatencyRangeMs.HIGH;
                    break;
                case PingStatsMode.EXTERNAL_SERVER_UNREACHABLE:
                    externalServer1LatencyRangeMs = LatencyRangeMs.UNDEFINED;
                    externalServer2LatencyRangeMs = LatencyRangeMs.UNDEFINED;
                    externalServer1LossRangePercent = LossRangePercent.UNREACHABLE;
                    externalServer2LossRangePercent = LossRangePercent.UNREACHABLE;
                    break;
                case PingStatsMode.EXTERNAL_SERVER_SLOW:
                    externalServer1LatencyRangeMs = LatencyRangeMs.VERY_HIGH;
                    externalServer2LatencyRangeMs = LatencyRangeMs.VERY_HIGH;
                    externalServer1LossRangePercent = LossRangePercent.HIGH;
                    externalServer2LossRangePercent = LossRangePercent.HIGH;
                    break;
                default:
                    break;
            }
        }

    }

    public double generateUniformlyRandomValue(int lowRange, int highRange) {
        return (lowRange + (random.nextDouble() * (highRange - lowRange)));
    }

    public double generateLossValue(@FakePingConfig.LossRangePercent int lossRangePercent) {
        int lowRange = 0;
        int highRange = 0;
        double valueToReturn = 0;
        switch(lossRangePercent) {
            case FakePingConfig.LossRangePercent.LOW:
                lowRange = 0;
                highRange = FakePingConfig.LossRangePercent.LOW;
                valueToReturn = generateUniformlyRandomValue(lowRange, highRange);
                break;
            case FakePingConfig.LossRangePercent.HIGH:
                lowRange = FakePingConfig.LossRangePercent.LOW;
                highRange = FakePingConfig.LossRangePercent.HIGH;
                valueToReturn = generateUniformlyRandomValue(lowRange, highRange);
                break;
            case FakePingConfig.LossRangePercent.UNREACHABLE:
                valueToReturn = 100;
                break;
        }
        return valueToReturn;
    }

    public double generateLatencyValue(@FakePingConfig.LatencyRangeMs int latencyRangeMs) {
        int lowRange = 0;
        int highRange = 0;
        double valueToReturn = 0;
        switch(latencyRangeMs) {
            case FakePingConfig.LatencyRangeMs.VERY_HIGH:
                lowRange = FakePingConfig.LatencyRangeMs.HIGH;
                highRange = FakePingConfig.LatencyRangeMs.VERY_HIGH;
                valueToReturn = generateUniformlyRandomValue(lowRange, highRange);
                break;
            case FakePingConfig.LatencyRangeMs.HIGH:
                lowRange = FakePingConfig.LatencyRangeMs.MEDIUM;
                highRange = FakePingConfig.LatencyRangeMs.HIGH;
                valueToReturn = generateUniformlyRandomValue(lowRange, highRange);
                break;
            case FakePingConfig.LatencyRangeMs.MEDIUM:
                lowRange = FakePingConfig.LatencyRangeMs.LOW;
                highRange = FakePingConfig.LatencyRangeMs.MEDIUM;
                valueToReturn = generateUniformlyRandomValue(lowRange, highRange);
                break;
            case FakePingConfig.LatencyRangeMs.LOW:
                lowRange = FakePingConfig.LatencyRangeMs.VERY_LOW;
                highRange = FakePingConfig.LatencyRangeMs.LOW;
                valueToReturn = generateUniformlyRandomValue(lowRange, highRange);
                break;
            case FakePingConfig.LatencyRangeMs.VERY_LOW:
                lowRange = 0;
                highRange = FakePingConfig.LatencyRangeMs.VERY_LOW;
                valueToReturn = generateUniformlyRandomValue(lowRange, highRange);
                break;
            case FakePingConfig.LatencyRangeMs.UNDEFINED:
                valueToReturn = -1;
                break;
        }
        return valueToReturn;
    }

    public PingStats generateIndividualPingStats(String pingAddress,
                                       @FakePingConfig.LatencyRangeMs int latencyRangeMs,
                                       @FakePingConfig.LossRangePercent int lossPercentRangePercent) {
        PingStats pingStats = new PingStats(pingAddress);
        pingStats.lossRatePercent = generateLossValue(lossPercentRangePercent);
        double latency = generateLatencyValue(latencyRangeMs);
        pingStats.avgLatencyMs = pingStats.maxLatencyMs = pingStats.minLatencyMs = latency;
        pingStats.deviationMs = 0;
        pingStats.updatedAt = System.currentTimeMillis();
        return pingStats;
    }

    public HashMap<String, PingStats> generateFakePingStats() {
        HashMap<String, PingStats> pingStatsHashMap = new HashMap<>();
        Log.v(TAG, "FAKE Generating fake ping for mode " + getPingStatsModeName(FakePingAnalyzer.pingStatsMode));
        FakePingConfig fakePingConfig = new FakePingConfig(FakePingAnalyzer.pingStatsMode);
        PingStats gatewayPingStats = generateIndividualPingStats(ipLayerInfo.gateway,
                fakePingConfig.gatewayLatencyRangeMs, fakePingConfig.gatewayLossRangePercent);
        PingStats dns1PingStats = generateIndividualPingStats(ipLayerInfo.dns1,
                fakePingConfig.dns1LatencyRangeMs, fakePingConfig.dns1LossRangePercent);
        PingStats dns2PingStats = generateIndividualPingStats(ipLayerInfo.dns2,
                fakePingConfig.dns2LatencyRangeMs, fakePingConfig.dns2LossRangePercent);
        PingStats externalServer1PingStats = generateIndividualPingStats(ipLayerInfo.referenceExternalAddress1,
                fakePingConfig.externalServer1LatencyRangeMs, fakePingConfig.externalServer1LossRangePercent);
        PingStats externalServer2PingStats = generateIndividualPingStats(ipLayerInfo.referenceExternalAddress2,
                fakePingConfig.externalServer2LatencyRangeMs, fakePingConfig.externalServer2LossRangePercent);

        pingStatsHashMap.put(ipLayerInfo.gateway, gatewayPingStats);
        pingStatsHashMap.put(ipLayerInfo.dns1, dns1PingStats);
        pingStatsHashMap.put(ipLayerInfo.dns2, dns2PingStats);
        pingStatsHashMap.put(ipLayerInfo.referenceExternalAddress1, externalServer1PingStats);
        pingStatsHashMap.put(ipLayerInfo.referenceExternalAddress2, externalServer2PingStats);
        return pingStatsHashMap;
    }

    public ListenableFuture<HashMap<String, PingStats>> scheduleEssentialPingTestsAsyncSafely() throws IllegalStateException {
        if (fakePingResultsFuture != null && !fakePingResultsFuture.isDone()) {
            AsyncFunction<HashMap<String, PingStats>, HashMap<String, PingStats>> redoPing = new
                    AsyncFunction<HashMap<String, PingStats>, HashMap<String, PingStats>>() {
                        @Override
                        public ListenableFuture<HashMap<String, PingStats>> apply(HashMap<String, PingStats> input) throws Exception {
                            return scheduleEssentialPingTestsAsync();
                        }
                    };
            ListenableFuture<HashMap<String, PingStats>> newPingResultsFuture = Futures.transformAsync(fakePingResultsFuture, redoPing);
            return newPingResultsFuture;
        } else {
            return scheduleEssentialPingTestsAsync();
        }
    }

    private ListenableFuture<HashMap<String, PingStats>> scheduleEssentialPingTestsAsync() throws IllegalStateException {
        eventBus.postEvent(new DobbyEvent(DobbyEvent.EventType.PING_STARTED));
        if (ipLayerInfo == null) {
            //Try to get new iplayerInfo
            eventBus.postEvent(new DobbyEvent(DobbyEvent.EventType.PING_FAILED));
            throw new IllegalStateException("Cannot schedule pings when iplayerInfo is null or own IP is 0.0.0.0");
        }
        fakePingResultsFuture = dobbyThreadpool.getListeningScheduledExecutorService().schedule(new Callable<HashMap<String, PingStats>>() {
            @Override
            public HashMap<String, PingStats> call() {
                HashMap<String, PingStats> pingStatsHashMap = generateFakePingStats();
                ipLayerPingStats = pingStatsHashMap;
                Log.v(TAG, "FAKE IP Layer Ping Stats " + ipLayerPingStats.toString());
                eventBus.postEvent(new DobbyEvent(DobbyEvent.EventType.PING_INFO_AVAILABLE));
                return  pingStatsHashMap;
            }
        }, PING_LATENCY_MS, TimeUnit.MILLISECONDS);
        return fakePingResultsFuture;
    }
}
