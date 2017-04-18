package com.inceptai.dobby.ai;

import android.support.annotation.IntDef;

import com.inceptai.dobby.connectivity.ConnectivityAnalyzer;
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
            8.0, /* good */
            3.0, /* average */
            0.05 /* poor */
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

    private static final double[] WIFI_RSSI_STEPS_MS = { /* higher is better */
            -50.0, /* excellent */
            -70.0, /* good */
            -88.0, /* average */
            -105.0 /* poor */
    };

    private static final double[] WIFI_CONTENTION_STEPS_MS = { /* lower is better */
            0.2, /* excellent */
            0.4, /* good */
            0.7, /* average */
            0.9 /* poor */
    };

    @IntDef({MetricType.EXCELLENT, MetricType.GOOD, MetricType.AVERAGE, MetricType.POOR,
            MetricType.ABYSMAL, MetricType.UNKNOWN})
    @Retention(RetentionPolicy.SOURCE)
    public @interface MetricType {
        int EXCELLENT = 0;
        int GOOD = 1;
        int AVERAGE = 2;
        int POOR = 3;
        int ABYSMAL = 4;  /* all valid values that are POOR or worse */
        int UNKNOWN = 5;  /* no valid result available */
    }

    public static class BandwidthGrade {
        @MetricType int uploadBandwidth;
        @MetricType int downloadBandwidth;
        @MetricType int overallResult;

        double uploadMbps;
        double downloadMbps;
    }

    public static class PingGrade {
        @MetricType int externalServerLatency;
        @MetricType int dnsServerLatency;
        @MetricType int routerLatency;
    }

    /**
     * Metrics based on doing a short http download of the main URL, such as the router webpage
     * or google.com, etc.
     */
    public static class HttpGrade {
        @MetricType int httpDownloadLatency;
    }

    public static class WifiGrade {
        HashMap<Integer, Integer> wifiChannelAvailabiltyMap;  /* based on congestion metric */
        @MetricType int primaryApSignal;
        @ConnectivityAnalyzer.WifiConnectivityMode int wifiConnetivityMode;
        @WifiState.WifiLinkMode
        int wifiProblemMode;
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

        grade.downloadBandwidth = getGradeHigherIsBetter(downloadMbps, BW_DOWNLOAD_STEPS_MBPS, downloadMbps > 0.0);
        grade.uploadBandwidth = getGradeHigherIsBetter(uploadMbps, BW_UPLOAD_STEPS_MBPS, uploadMbps > 0.0);

        return grade;
    }

    public static PingGrade interpret(HashMap<String, PingStats> externalServerStats,
                                      PingStats routerStats, PingStats primaryDnsStats) {
        PingGrade pingGrade = new PingGrade();
        double avgExtenalServerLatency = 0.0;
        int count = 0;
        for(PingStats pingStats : externalServerStats.values()) {
            if (pingStats.avgLatencyMs > 0.0){
                avgExtenalServerLatency += pingStats.avgLatencyMs;
                count ++;
            }
        }
        avgExtenalServerLatency /= (double) count;

        pingGrade.externalServerLatency = getGradeLowerIsBetter(
                avgExtenalServerLatency,
                PING_LATENCY_EXTSERVER_STEPS_MS,
                avgExtenalServerLatency > 0.0);

        pingGrade.dnsServerLatency = getGradeLowerIsBetter(
                primaryDnsStats.avgLatencyMs,
                PING_LATENCY_DNS_STEPS_MS,
                avgExtenalServerLatency > 0.0);

        pingGrade.routerLatency = getGradeLowerIsBetter(routerStats.avgLatencyMs,
                PING_LATENCY_ROUTER_STEPS_MS,
                routerStats.avgLatencyMs > 0.0);

        return pingGrade;
    }

    public static HttpGrade interpret(PingStats httpRouterStats) {
        HttpGrade httpGrade = new HttpGrade();
        httpGrade.httpDownloadLatency = getGradeLowerIsBetter(httpRouterStats.avgLatencyMs,
                HTTP_LATENCY_ROUTER_STEPS_MS,
                httpRouterStats.avgLatencyMs > 0.0);
        return  httpGrade;
    }

    public static WifiGrade interpret(HashMap<Integer, Double> contentionMetric,
                                      int apRssi,
                                      @WifiState.WifiLinkMode int wifiProblemMode,
                                      @ConnectivityAnalyzer.WifiConnectivityMode int wifiConnectivityMode) {
        WifiGrade wifiGrade = new WifiGrade();
        wifiGrade.wifiChannelAvailabiltyMap = new HashMap<>();
        wifiGrade.primaryApSignal = getGradeHigherIsBetter(apRssi, WIFI_RSSI_STEPS_MS, apRssi < 0);
        wifiGrade.wifiConnetivityMode = wifiConnectivityMode;
        wifiGrade.wifiProblemMode = wifiProblemMode;
        return wifiGrade;
    }

    @MetricType
    private static int getGradeLowerIsBetter(double value, double[] steps, boolean isValid) {
        if (!isValid) return MetricType.UNKNOWN;
        if (value < steps[0]) return MetricType.EXCELLENT;
        else if (value < steps[1]) return MetricType.GOOD;
        else if (value < steps[2]) return MetricType.AVERAGE;
        else if (value < steps[3]) return MetricType.POOR;
        else return MetricType.ABYSMAL;
    }

    @MetricType
    private static int getGradeHigherIsBetter(double value, double[] steps, boolean isValid) {
        if (!isValid) return MetricType.UNKNOWN;
        if (value > steps[0]) return MetricType.EXCELLENT;
        else if (value > steps[1]) return MetricType.GOOD;
        else if (value > steps[2]) return MetricType.AVERAGE;
        else if (value > steps[3]) return MetricType.POOR;
        else return MetricType.ABYSMAL;
    }
}
