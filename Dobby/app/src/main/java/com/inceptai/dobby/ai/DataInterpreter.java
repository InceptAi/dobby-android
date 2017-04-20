package com.inceptai.dobby.ai;

import android.support.annotation.IntDef;

import com.inceptai.dobby.connectivity.ConnectivityAnalyzer;
import com.inceptai.dobby.model.DobbyWifiInfo;
import com.inceptai.dobby.model.IPLayerInfo;
import com.inceptai.dobby.model.PingStats;
import com.inceptai.dobby.wifi.WifiState;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;

/**
 * Created by arunesh on 4/14/17.
 */

public class DataInterpreter {

    private static final double[] BW_DOWNLOAD_STEPS_MBPS = { /* higher is better */
            20.0, /* excellent */
            15.0, /* good */
            10.0, /* average */
            5.0 /* poor */
    };

    private static final double[] BW_UPLOAD_STEPS_MBPS = { /* higher is better */
            6.0, /* excellent */
            2.0, /* good */
            0.8, /* average */
            0.05 /* poor */
    };

    private static final double[] PING_LATENCY_EXTSERVER_STEPS_MS = { /* lower is better */
            5.0, /* excellent */
            15.0, /* good */
            40.0, /* average */
            100.0 /* poor */
    };

    private static final double[] PING_LATENCY_DNS_STEPS_MS = { /* lower is better */
            15.0, /* excellent */
            25.0, /* good */
            50.0, /* average */
            100.0 /* poor */
    };

    private static final double[] PING_LATENCY_ROUTER_STEPS_MS = { /* lower is better */
            4.0, /* excellent */
            8.0, /* good */
            20.0, /* average */
            100.0 /* poor */
    };

    private static final double[] HTTP_LATENCY_ROUTER_STEPS_MS = { /* lower is better */
            15.0, /* excellent */
            30.0, /* good */
            50.0, /* average */
            100.0 /* poor */
    };

    private static final double[] WIFI_RSSI_STEPS_DBM = { /* higher is better */
            -50.0, /* excellent */
            -70.0, /* good */
            -88.0, /* average */
            -105.0 /* poor */
    };

    private static final double[] WIFI_CONTENTION_STEPS_LEVELS = { /* lower is better */
            0.2, /* excellent */
            0.4, /* good */
            0.7, /* average */
            0.9 /* poor */
    };

    private static final double[] WIFI_CHANNEL_OCCUPANCY_STEPS = { /* lower is better */
            0.0, /* excellent */
            1.0, /* good */
            2.0, /* average */
            4.0 /* poor */
    };

    private static final double[] LINK_SPEED_STEPS_MBPS = { /* higher is better */
            100, /* excellent */
            50, /* good */
            30, /* average */
            10 /* poor */
    };


    @IntDef({MetricType.EXCELLENT, MetricType.GOOD, MetricType.AVERAGE, MetricType.POOR,
            MetricType.NONFUNCTIONAL, MetricType.UNKNOWN})
    @Retention(RetentionPolicy.SOURCE)
    public @interface MetricType {
        int EXCELLENT = 0;
        int GOOD = 1;
        int AVERAGE = 2;
        int POOR = 3;
        int NONFUNCTIONAL = 4;  /* all valid values that are POOR or worse */
        int UNKNOWN = 5;  /* no valid result available */
    }

    public static boolean isUnknown(@MetricType int metric) {
        return (metric == MetricType.UNKNOWN);
    }

    public static boolean isGoodOrExcellent(@MetricType int metric) {
        return (metric == MetricType.EXCELLENT || metric == MetricType.GOOD);
    }

    public static boolean isAverageOrPoor(@MetricType int metric) {
        return (metric == MetricType.AVERAGE || metric == MetricType.POOR);
    }

    public static boolean isNonFunctional(@MetricType int metric) {
        return (metric == MetricType.NONFUNCTIONAL);
    }

    public static boolean isGoodOrExcellent(@MetricType int[] metric) {
        boolean allExcellent = true;
        for (int index=0; index < metric.length; index++) {
            allExcellent = allExcellent && (metric[index] == MetricType.EXCELLENT ||
                    metric[index] == MetricType.GOOD);
        }
        return allExcellent;
    }

    public static boolean isAverageOrPoor(@MetricType int[] metric) {
        boolean allPoor = true;
        for (int index=0; index < metric.length; index++) {
            allPoor = allPoor && (metric[index] == MetricType.AVERAGE ||
                    metric[index] == MetricType.POOR);
        }
        return allPoor;
    }

    public static boolean isNonFunctional(@MetricType int[] metric) {
        boolean allNonFunctional = true;
        for (int index=0; index < metric.length; index++) {
            allNonFunctional = allNonFunctional && (metric[index] == MetricType.AVERAGE ||
                    metric[index] == MetricType.POOR);
        }
        return allNonFunctional;
    }


    public static class BandwidthGrade {
        @MetricType int uploadBandwidthMetric;
        @MetricType int downloadBandwidthMetric;
        @MetricType int overallResult;

        double uploadMbps;
        double downloadMbps;
    }

    public static class PingGrade {
        @MetricType int externalServerLatencyMetric;
        @MetricType int dnsServerLatencyMetric;
        @MetricType int routerLatencyMetric;
        @MetricType int alternativeDnsMetric;
    }

    /**
     * Metrics based on doing a short http download of the main URL, such as the router webpage
     * or google.com, etc.
     */
    public static class HttpGrade {
        @MetricType int httpDownloadLatencyMetric;
    }

    public static class WifiGrade {
        HashMap<Integer, Integer> wifiChannelOccupancyMetric;  /* based on congestion metric */
        @MetricType int primaryApSignalMetric;
        @MetricType int primaryApLinkSpeedMetric;
        @MetricType int primaryLinkChannelOccupancyMetric;
        @ConnectivityAnalyzer.WifiConnectivityMode int wifiConnectivityMode;
        @WifiState.WifiLinkMode int wifiProblemMode;

        public WifiGrade() {
            wifiChannelOccupancyMetric = new HashMap<>();
        }
    }

    /**
     *
     * @param uploadMbps Bandwidth in Mbps or -1 if failed.
     * @param downloadMbps Bandwidth in Mbps or -1 if failed.
     * @return
     */
    public static BandwidthGrade interpret(double uploadMbps, double downloadMbps) {
        BandwidthGrade grade = new BandwidthGrade();

        grade.uploadMbps = uploadMbps;
        grade.downloadMbps = downloadMbps;

        grade.downloadBandwidthMetric = getGradeHigherIsBetter(downloadMbps, BW_DOWNLOAD_STEPS_MBPS, downloadMbps > 0.0);
        grade.uploadBandwidthMetric = getGradeHigherIsBetter(uploadMbps, BW_UPLOAD_STEPS_MBPS, uploadMbps > 0.0);

        return grade;
    }

    public static PingGrade interpret(HashMap<String, PingStats> pingStatsHashMap, IPLayerInfo ipLayerInfo) {
        //Get external server stats
        HashMap<String, PingStats> externalServerStats = new HashMap<>();
        if (ipLayerInfo.referenceExternalAddress1 != null) {
            externalServerStats.put(ipLayerInfo.referenceExternalAddress1, pingStatsHashMap.get(ipLayerInfo.referenceExternalAddress1));
        }
        if (ipLayerInfo.referenceExternalAddress2 != null) {
            externalServerStats.put(ipLayerInfo.referenceExternalAddress2, pingStatsHashMap.get(ipLayerInfo.referenceExternalAddress2));
        }

        //Get alternative DNS stats
        PingStats alternativeDnsStats1 = pingStatsHashMap.get(ipLayerInfo.publicDns1);
        PingStats alternativeDnsStats2 = pingStatsHashMap.get(ipLayerInfo.publicDns2);
        PingStats lowerAlternativeDnsStats = new PingStats(ipLayerInfo.publicDns1);
        if (alternativeDnsStats1.avgLatencyMs > 0) {
            lowerAlternativeDnsStats = alternativeDnsStats1;
        }
        if (alternativeDnsStats2.avgLatencyMs > 0) {
            if (alternativeDnsStats1.avgLatencyMs < 0 || alternativeDnsStats2.avgLatencyMs < alternativeDnsStats1.avgLatencyMs) {
                lowerAlternativeDnsStats = alternativeDnsStats2;
            }
        }

        //Router stats
        PingStats routerStats = pingStatsHashMap.get(ipLayerInfo.gateway);
        //Primary DNS stats
        PingStats primaryDnsStats = pingStatsHashMap.get(ipLayerInfo.dns1);

        PingGrade pingGrade = new PingGrade();
        double avgExternalServerLatency = 0.0;
        int count = 0;
        for(PingStats pingStats : externalServerStats.values()) {
            if (pingStats.avgLatencyMs > 0.0){
                avgExternalServerLatency += pingStats.avgLatencyMs;
                count ++;
            }
        }
        avgExternalServerLatency /= (double) count;

        pingGrade.externalServerLatencyMetric = getGradeLowerIsBetter(
                avgExternalServerLatency,
                PING_LATENCY_EXTSERVER_STEPS_MS,
                avgExternalServerLatency > 0.0);

        pingGrade.dnsServerLatencyMetric = getGradeLowerIsBetter(
                primaryDnsStats.avgLatencyMs,
                PING_LATENCY_DNS_STEPS_MS,
                primaryDnsStats.avgLatencyMs > 0.0);

        pingGrade.routerLatencyMetric = getGradeLowerIsBetter(routerStats.avgLatencyMs,
                PING_LATENCY_ROUTER_STEPS_MS,
                routerStats.avgLatencyMs > 0.0);


        pingGrade.alternativeDnsMetric = getGradeLowerIsBetter(lowerAlternativeDnsStats.avgLatencyMs,
                PING_LATENCY_ROUTER_STEPS_MS,
                lowerAlternativeDnsStats.avgLatencyMs > 0.0);

        return pingGrade;
    }

    public static HttpGrade interpret(PingStats httpRouterStats) {
        HttpGrade httpGrade = new HttpGrade();
        httpGrade.httpDownloadLatencyMetric = getGradeLowerIsBetter(httpRouterStats.avgLatencyMs,
                HTTP_LATENCY_ROUTER_STEPS_MS,
                httpRouterStats.avgLatencyMs > 0.0);
        return  httpGrade;
    }

    public static WifiGrade interpret(HashMap<Integer, WifiState.ChannelInfo> wifiChannelInfo,
                                      DobbyWifiInfo linkInfo,
                                      @WifiState.WifiLinkMode int wifiProblemMode,
                                      @ConnectivityAnalyzer.WifiConnectivityMode int wifiConnectivityMode) {
        WifiGrade wifiGrade = new WifiGrade();
        //Figure out the # of APs on primary channel
        WifiState.ChannelInfo primaryChannelInfo = wifiChannelInfo.get(linkInfo.getFrequency());

        int numStrongInterferingAps = computeStrongInterferingAps(primaryChannelInfo);
        wifiGrade.primaryLinkChannelOccupancyMetric = getGradeLowerIsBetter(numStrongInterferingAps,
                WIFI_CHANNEL_OCCUPANCY_STEPS,
                (linkInfo.getFrequency() > 0 && numStrongInterferingAps >= 0));

        //Compute metrics for all channels -- for later use
        for (WifiState.ChannelInfo channelInfo: wifiChannelInfo.values()) {
            wifiGrade.wifiChannelOccupancyMetric.put(channelInfo.channelFrequency,
                    computeStrongInterferingAps(channelInfo));
        }

        wifiGrade.primaryApSignalMetric = getGradeHigherIsBetter(linkInfo.getRssi(),
                WIFI_RSSI_STEPS_DBM, linkInfo.getRssi() < 0);
        wifiGrade.primaryApLinkSpeedMetric = getGradeHigherIsBetter(linkInfo.getLinkSpeed(),
                LINK_SPEED_STEPS_MBPS, linkInfo.getLinkSpeed() > 0);

        wifiGrade.wifiConnectivityMode = wifiConnectivityMode;
        wifiGrade.wifiProblemMode = wifiProblemMode;
        return wifiGrade;
    }

    private static int computeStrongInterferingAps(WifiState.ChannelInfo channelInfo) {
        if (channelInfo == null) {
            return -1;
        }
        return channelInfo.similarStrengthAPs +
                channelInfo.higherStrengthAps + channelInfo.highestStrengthAps;
    }

    @MetricType
    private static int getGradeLowerIsBetter(double value, double[] steps, boolean isValid) {
        if (!isValid) return MetricType.UNKNOWN;
        if (value < steps[0]) return MetricType.EXCELLENT;
        else if (value < steps[1]) return MetricType.GOOD;
        else if (value < steps[2]) return MetricType.AVERAGE;
        else if (value < steps[3]) return MetricType.POOR;
        else return MetricType.NONFUNCTIONAL;
    }

    @MetricType
    private static int getGradeHigherIsBetter(double value, double[] steps, boolean isValid) {
        if (!isValid) return MetricType.UNKNOWN;
        if (value > steps[0]) return MetricType.EXCELLENT;
        else if (value > steps[1]) return MetricType.GOOD;
        else if (value > steps[2]) return MetricType.AVERAGE;
        else if (value > steps[3]) return MetricType.POOR;
        else return MetricType.NONFUNCTIONAL;
    }
}
