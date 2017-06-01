package com.inceptai.dobby.ai;

import android.support.annotation.IntDef;

import com.google.gson.Gson;
import com.inceptai.dobby.connectivity.ConnectivityAnalyzer;
import com.inceptai.dobby.model.DobbyWifiInfo;
import com.inceptai.dobby.model.IPLayerInfo;
import com.inceptai.dobby.model.PingStats;
import com.inceptai.dobby.speedtest.BandwithTestCodes;
import com.inceptai.dobby.utils.Utils;
import com.inceptai.dobby.wifi.WifiState;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;

/**
 * Created by arunesh on 4/14/17.
 */

public class DataInterpreter {
    private static final long MAX_STALENESS_MS = 120 * 1000; // 120 seconds.
    private static final long MAX_LATENCY_FOR_BEING_NONFUNCTIONAL_MS = 10000; // 10secs

    private static final double[] BW_DOWNLOAD_STEPS_MBPS = { /* higher is better */
            20.0, /* excellent */
            15.0, /* good */
            10.0, /* average */
            5.0, /* poor */
    };

    private static final double[] BW_UPLOAD_STEPS_MBPS = { /* higher is better */
            10.0, /* excellent */
            5.0, /* good */
            2.0, /* average */
            1.0 /* poor */
    };

    private static final double[] PING_LATENCY_EXTSERVER_STEPS_MS = { /* lower is better */
            20.0, /* excellent */
            40.0, /* good */
            80.0, /* average */
            200.0 /* poor */
    };

    private static final double[] PING_LATENCY_DNS_STEPS_MS = { /* lower is better */
            15.0, /* excellent */
            25.0, /* good */
            50.0, /* average */
            100.0 /* poor */
    };

    private static final double[] PING_LATENCY_ROUTER_STEPS_MS = { /* lower is better */
            5.0, /* excellent */
            10.0, /* good */
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

    private static final double[] PKT_LOSS_RATE_STEPS = { /* lower is better */
            0.0, /* excellent */
            10.0, /* good */
            20.0, /* average */
            50.0 /* poor */
    };


    @IntDef({MetricType.EXCELLENT, MetricType.GOOD, MetricType.AVERAGE, MetricType.POOR,
            MetricType.ABYSMAL, MetricType.NONFUNCTIONAL, MetricType.UNKNOWN})
    @Retention(RetentionPolicy.SOURCE)
    public @interface MetricType {
        int EXCELLENT = 0;
        int GOOD = 1;
        int AVERAGE = 2;
        int POOR = 3;
        int ABYSMAL = 4;
        int NONFUNCTIONAL = 5;  /* all valid values that are POOR or worse */
        int UNKNOWN = 6;  /* no valid result available */
    }

    public static int compareMetric(@MetricType int metric1, @MetricType int metric2) {
        //Positive value means metric1 is better, neg means worse, 0 means equal
        return (metric2 - metric1);
    }


    public static boolean isUnknown(@MetricType int metric) {
        return (metric == MetricType.UNKNOWN);
    }

    public static boolean isGoodOrExcellent(@MetricType int metric) {
        return (metric == MetricType.EXCELLENT || metric == MetricType.GOOD);
    }

    public static boolean isGoodOrExcellentOrAverage(@MetricType int metric) {
        return (metric == MetricType.EXCELLENT || metric == MetricType.GOOD || metric == MetricType.AVERAGE);
    }

    public static boolean isAverageOrPoor(@MetricType int metric) {
        return (metric == MetricType.AVERAGE || metric == MetricType.POOR);
    }

    public static boolean isAverageOrPoorOrNonFunctional(@MetricType int metric) {
        return (metric == MetricType.AVERAGE || metric == MetricType.POOR || metric == MetricType.NONFUNCTIONAL);
    }


    public static boolean isAbysmal(@MetricType int metric) {
        return (metric == MetricType.ABYSMAL);
    }

    public static boolean isNonFunctional(@MetricType int metric) {
        return (metric == MetricType.NONFUNCTIONAL);
    }

    public static boolean isNonFunctionalOrUnknown(@MetricType int metric) {
        return (metric == MetricType.NONFUNCTIONAL || metric == MetricType.UNKNOWN);
    }

    public static boolean isAverage(@MetricType int metric) {
        return (metric == MetricType.AVERAGE);
    }

    public static boolean isPoorOrAbysmal(@MetricType int metric) {
        return (metric == MetricType.POOR || metric == MetricType.ABYSMAL);
    }

    public static boolean isPoorOrAbysmalOrNonFunctional(@MetricType int metric) {
        return (metric == MetricType.POOR || metric == MetricType.ABYSMAL || metric == MetricType.NONFUNCTIONAL);
    }

    public static boolean isAbysmalOrNonFunctional(@MetricType int metric) {
        return (metric == MetricType.ABYSMAL || metric == MetricType.NONFUNCTIONAL);
    }

    public static boolean isPoor(@MetricType int metric) {
        return (metric == MetricType.POOR);
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

    private static boolean isFresh(long timestampMs) {
        return (System.currentTimeMillis() - timestampMs < MAX_STALENESS_MS);
    }


    public static class BandwidthGrade {
        private @MetricType int uploadBandwidthMetric = MetricType.UNKNOWN;
        private @MetricType int downloadBandwidthMetric = MetricType.UNKNOWN;
        private long downloadUpdatedAtMs;
        private long uploadUpdatedAtMs;
        private double uploadMbps;
        private double downloadMbps;
        String isp;
        String externalIP;
        @BandwithTestCodes.ErrorCodes int errorCode = BandwithTestCodes.ErrorCodes.ERROR_UNINITIAlIZED;

        public BandwidthGrade() {
            //Set timestamp here
        }

        public double getDownloadBandwidth() {
            return downloadMbps;
        }

        public double getUploadBandwidth() {
            return uploadMbps;
        }

        public String toJson() {
            Gson gson = new Gson();
            String json = gson.toJson(this);
            return json;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("Bandwidth Grade: Upload: " + metricTypeToString(uploadBandwidthMetric));
            builder.append("\n Download: " + metricTypeToString(downloadBandwidthMetric));
            builder.append("\n Download BW: " + downloadMbps);
            builder.append("\n Upload BW: " + uploadMbps);
            builder.append("\n Download Updated: " + downloadUpdatedAtMs);
            builder.append("\n Upload Updated: " + downloadUpdatedAtMs);
            builder.append("\n isp : " + isp);
            builder.append("\n external IP: " + externalIP);
            builder.append("\n error code: " + errorCode);
            return builder.toString();
        }

        public void clearUpload() {
            uploadMbps = -1.0;
            uploadUpdatedAtMs = 0;
        }

        public void clearDownload() {
            downloadMbps = -1.0;
            downloadUpdatedAtMs = 0;
        }

        public void clear() {
            clearUpload();
            clearDownload();
        }

        boolean hasValidUpload() {
            return uploadUpdatedAtMs > 0 && isFresh(uploadUpdatedAtMs);
        }

        boolean hasValidDownload() {
            return downloadUpdatedAtMs > 0 && isFresh(downloadUpdatedAtMs);
        }

        public double getUploadMbps() {
            return uploadMbps;
        }

        public double getDownloadMbps() {
            return downloadMbps;
        }

        public void updateTimestamp() {
            updateDownloadTimestamp();
            updateUploadTimestamp();
        }

        public void updateUploadTimestamp() {
            uploadUpdatedAtMs = System.currentTimeMillis();
        }

        public void updateDownloadTimestamp() {
            downloadUpdatedAtMs = System.currentTimeMillis();
        }

        public void updateUploadInfo(double speedMbps, @MetricType int speedMetric) {
            uploadBandwidthMetric = speedMetric;
            uploadMbps = speedMbps;
            updateUploadTimestamp();
        }

        public void updateDownloadInfo(double speedMbps, @MetricType int speedMetric) {
            downloadBandwidthMetric = speedMetric;
            downloadMbps = speedMbps;
            updateDownloadTimestamp();
        }

        @MetricType
        public int getDownloadBandwidthMetric() {
            return downloadBandwidthMetric;
        }

        @MetricType
        public int getUploadBandwidthMetric() {
            return uploadBandwidthMetric;
        }

        public void updateUploadMetric(@MetricType int speedMetric) {
            //Not updating timestamp here
            uploadBandwidthMetric = speedMetric;
        }

        public void updateDownloadMetric(@MetricType int speedMetric) {
            //Not updating timestamp here
            downloadBandwidthMetric = speedMetric;
        }

    }


    public static class PingGrade {
        @MetricType int externalServerLatencyMetric = MetricType.UNKNOWN;
        @MetricType int dnsServerLatencyMetric = MetricType.UNKNOWN;
        @MetricType int routerLatencyMetric = MetricType.UNKNOWN;
        @MetricType int alternativeDnsMetric = MetricType.UNKNOWN;
        String primaryDns;
        String alternativeDns;
        double routerLatencyMs;
        double dnsServerLatencyMs;
        double externalServerLatencyMs;
        double alternativeDnsLatencyMs;
        long updatedAtMs;
        @BandwithTestCodes.ErrorCodes int errorCode = BandwithTestCodes.ErrorCodes.ERROR_UNINITIAlIZED;


        public PingGrade() {}

        public String toJson() {
            Gson gson = new Gson();
            String json = gson.toJson(this);
            return json;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("PingGrade: ExternalServer: " +
            metricTypeToString(externalServerLatencyMetric) + " DnsServer: " +
            metricTypeToString(dnsServerLatencyMetric) + " Router: " +
            metricTypeToString(routerLatencyMetric) + " Alternative Dns: " +
            metricTypeToString(alternativeDnsMetric));
            return builder.toString();
        }

        public void clear() {
            updatedAtMs = 0;
        }

        public boolean hasValidData() {
            return updatedAtMs > 0.0 && DataInterpreter.isFresh(updatedAtMs);
        }

        public void updateTimestamp() {
            updatedAtMs = System.currentTimeMillis();
        }

        @MetricType
        public int getRouterLatencyMetric() {
            return routerLatencyMetric;
        }

        @MetricType
        public int getExternalServerLatencyMetric() {
            return externalServerLatencyMetric;
        }

        @MetricType
        public int getDnsServerLatencyMetric() {
            return dnsServerLatencyMetric;
        }

        @MetricType
        public int getAlternativeDnsMetric() {
            return alternativeDnsMetric;
        }

        public String getPrimaryDns() {
            return primaryDns;
        }

        public String getAlternativeDns() {
            return alternativeDns;
        }

        public double getRouterLatencyMs() {
            return routerLatencyMs;
        }

        public double getDnsServerLatencyMs() {
            return dnsServerLatencyMs;
        }

        public double getExternalServerLatencyMs() {
            return externalServerLatencyMs;
        }

        public double getAlternativeDnsLatencyMs() {
            return alternativeDnsLatencyMs;
        }

        public boolean isFresh() {
            return updatedAtMs > 0.0 && DataInterpreter.isFresh(updatedAtMs);
        }
    }

    /**
     * Metrics based on doing a short http download of the main URL, such as the router webpage
     * or google.com, etc.
     */
    public static class HttpGrade {
        @MetricType int httpDownloadLatencyMetric = MetricType.UNKNOWN;
        private long updatedAtMs;
        @BandwithTestCodes.ErrorCodes int errorCode = BandwithTestCodes.ErrorCodes.ERROR_UNINITIAlIZED;

        public HttpGrade() {
        }

        public boolean hasValidData() {
            return updatedAtMs > 0.0 && DataInterpreter.isFresh(updatedAtMs);
        }

        public void clear() {
            updatedAtMs = 0;
        }

        public boolean isFresh() {
            return updatedAtMs > 0.0 && DataInterpreter.isFresh(updatedAtMs);
        }

        public void updateTimestamp() {
            updatedAtMs = System.currentTimeMillis();
        }

        public String toJson() {
            Gson gson = new Gson();
            String json = gson.toJson(this);
            return json;
        }

        @Override
        public String toString() {
            return "HttpGrade: " + metricTypeToString(httpDownloadLatencyMetric);
        }
    }

    public static class WifiGrade {
        HashMap<Integer, Integer> wifiChannelOccupancyMetric;  /* based on congestion metric */
        @MetricType int primaryApSignalMetric = MetricType.UNKNOWN;
        @MetricType int primaryApLinkSpeedMetric = MetricType.UNKNOWN;
        @MetricType int primaryLinkChannelOccupancyMetric = MetricType.UNKNOWN;
        @ConnectivityAnalyzer.WifiConnectivityMode int wifiConnectivityMode = ConnectivityAnalyzer.WifiConnectivityMode.UNKNOWN;
        @WifiState.WifiLinkMode int wifiLinkMode = WifiState.WifiLinkMode.UNKNOWN;
        String primaryApSsid;
        int primaryApChannel;
        int leastOccupiedChannel;
        int primaryApChannelInterferingAps;
        int leastOccupiedChannelAps;
        int primaryApSignal;
        private long updatedAtMs;
        @BandwithTestCodes.ErrorCodes int errorCode = BandwithTestCodes.ErrorCodes.ERROR_UNINITIAlIZED;


        public WifiGrade() {
            wifiChannelOccupancyMetric = new HashMap<>();
        }

        public String toJson() {
            Gson gson = new Gson();
            String json = gson.toJson(this);
            return json;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("WifiGrade: Primary AP signalMetric" +
                    metricTypeToString(primaryApSignalMetric) +
                    ", SpeedMetric: " + metricTypeToString(primaryApLinkSpeedMetric) +
                    ", channelOccupancy: " +
                    metricTypeToString(primaryLinkChannelOccupancyMetric));
            builder.append("\nWifi connectivity mode: " + ConnectivityAnalyzer.connectivityModeToString(wifiConnectivityMode));
            builder.append("\nWifi link mode: " + WifiState.wifiLinkModeToString(wifiLinkMode));
            builder.append("\nChannel map:" + wifiChannelOccupancyMetric.toString());
            builder.append("\n primaryApChannel:" + primaryApChannel);
            builder.append("\n primaryApSignal:" + primaryApSignal);
            builder.append("\n leastOccupiedChannel:" + leastOccupiedChannel);
            builder.append("\n primaryApChannelAps:" + primaryApChannelInterferingAps);
            builder.append("\n leastOccupiedChannelAps:" + leastOccupiedChannelAps);
            return builder.toString();
        }

        public boolean isWifiOff() {
            return (wifiConnectivityMode == ConnectivityAnalyzer.WifiConnectivityMode.OFF);
        }

        public boolean isWifiDisconnected() {
            return (wifiConnectivityMode == ConnectivityAnalyzer.WifiConnectivityMode.ON_AND_DISCONNECTED);
        }

        public String userReadableInterpretation() {
            StringBuilder sb = new StringBuilder();
            //Wifi is off
            switch (wifiConnectivityMode) {
                case ConnectivityAnalyzer.WifiConnectivityMode.CONNECTED_AND_ONLINE:
                    sb.append("You are connected and online via wifi network: " + getPrimaryApSsid() + ".");
                    sb.append(convertSignalToReadableMessage());
                    break;
                case ConnectivityAnalyzer.WifiConnectivityMode.CONNECTED_AND_CAPTIVE_PORTAL:
                    sb.append("You are behind a captive portal -- " +
                            "basically the wifi you are connected to " +
                            getPrimaryApSsid() + " is managed by someone who " +
                            "restricts access unless you sign in. " +
                            "You should launch a browser and it should take you to a login page");
                    break;
                case ConnectivityAnalyzer.WifiConnectivityMode.CONNECTED_AND_OFFLINE:
                    sb.append("You are connected to wifi network: " + getPrimaryApSsid() + " but we can't reach Internet through this network. " +
                            "This could be an issue with the router or your Internet provider. We can run full tests to see whats going on ?");
                    break;
                case ConnectivityAnalyzer.WifiConnectivityMode.CONNECTED_AND_UNKNOWN:
                    sb.append("You are connected via wifi network: " + getPrimaryApSsid() + ".");
                    sb.append(convertSignalToReadableMessage());
                    break;
                case ConnectivityAnalyzer.WifiConnectivityMode.ON_AND_DISCONNECTED:
                    sb.append("You are currently not connected to any wifi network. If your phone is not connecting, try running full tests and we can diagnose why that could be ?");
                    break;
                case ConnectivityAnalyzer.WifiConnectivityMode.OFF:
                    sb.append("Your wifi is currently switched off. Try turing it on from the settings and we can run some speed tests to see how much bandwidth you are getting.");
                    break;
            }
            return sb.toString();
        }

        private String convertSignalToReadableMessage() {
            if (DataInterpreter.isUnknown(getPrimaryApSignalMetric())) {
                return Utils.EMPTY_STRING;
            }
            int signalPercent = Utils.convertSignalDbmToPercent(getPrimaryApSignal());
            String message = Utils.EMPTY_STRING;
            if (DataInterpreter.isPoorOrAbysmalOrNonFunctional(getPrimaryApSignalMetric())) {
                message = "Your connection to your wifi is weak, at about " + signalPercent +
                        "% strength (100% means very high signal, usually when you " +
                        "are right next to wifi router).";
            } else if (DataInterpreter.isAverage(getPrimaryApSignalMetric())) {
                message = "You connection to your wifi is just ok, at about " + signalPercent +
                        "% strength (100% means very high signal, usually when you " +
                        "are right next to wifi router).";
            } else if (DataInterpreter.isGoodOrExcellent(getPrimaryApSignalMetric())) {
                message = "You connection to your wifi is really good, at about " + signalPercent +
                        "% strength (100% means very high signal, usually when you " +
                        "are right next to wifi router).";
            }
            return message;
        }

        public void clear() {
            updatedAtMs = 0;
        }

        public boolean hasValidData() {
            return updatedAtMs > 0.0 && DataInterpreter.isFresh(updatedAtMs);
        }

        public void updateTimestamp() {
            updatedAtMs = System.currentTimeMillis();
        }

        @MetricType
        public int getPrimaryApSignalMetric() {
            return primaryApSignalMetric;
        }

        public HashMap<Integer, Integer> getWifiChannelOccupancyMetric() {
            return wifiChannelOccupancyMetric;
        }

        public int getPrimaryApLinkSpeedMetric() {
            return primaryApLinkSpeedMetric;
        }

        @MetricType
        public int getPrimaryLinkChannelOccupancyMetric() {
            return primaryLinkChannelOccupancyMetric;
        }

        public double getPrimaryLinkCongestionPercentage() {
            double x = (double) getPrimaryApChannelInterferingAps();
            return 1.0 / (1.0 + Math.exp(-x + 4.0));
        }

        public int getWifiConnectivityMode() {
            return wifiConnectivityMode;
        }

        public int getWifiLinkMode() {
            return wifiLinkMode;
        }

        public String getPrimaryApSsid() {
            return primaryApSsid;
        }

        public int getPrimaryApChannel() {
            return primaryApChannel;
        }

        public int getLeastOccupiedChannel() {
            return leastOccupiedChannel;
        }

        public int getPrimaryApChannelInterferingAps() {
            return primaryApChannelInterferingAps;
        }

        public int getLeastOccupiedChannelAps() {
            return leastOccupiedChannelAps;
        }

        public int getPrimaryApSignal() {
            return primaryApSignal;
        }

        public boolean isFresh() {
            return updatedAtMs > 0.0 && DataInterpreter.isFresh(updatedAtMs);
        }
    }

    public static String metricTypeToString(@MetricType int metricType) {
        switch(metricType) {
            case MetricType.EXCELLENT:
                return "EXCELLENT";
            case MetricType.AVERAGE:
                return "AVERAGE";
            case MetricType.GOOD:
                return "GOOD";
            case MetricType.POOR:
                return "POOR";
            case MetricType.ABYSMAL:
                return "ABYSMAL";
            case MetricType.NONFUNCTIONAL:
                return "NONFUNCTIONAL";

        }
        return "UNKNOWN";
    }

    /**
     *
     * @param uploadMbps Bandwidth in Mbps or -1 if failed.
     * @param downloadMbps Bandwidth in Mbps or -1 if failed.
     * @return
     */
    public static BandwidthGrade interpret(double downloadMbps, double uploadMbps,
                                           String isp, String externalClientIp,
                                           int errorCode) {
        BandwidthGrade grade = new BandwidthGrade();
        @MetricType int downloadMetric = getGradeHigherIsBetter(downloadMbps, BW_DOWNLOAD_STEPS_MBPS, downloadMbps >= 0.0, downloadMbps == 0.0);
        @MetricType int uploadMetric = getGradeHigherIsBetter(uploadMbps, BW_UPLOAD_STEPS_MBPS, uploadMbps >= 0.0, uploadMbps == 0.0);
        grade.updateUploadInfo(uploadMbps, uploadMetric);
        grade.updateDownloadInfo(downloadMbps, downloadMetric);
        grade.isp = isp;
        grade.externalIP = externalClientIp;
        grade.errorCode = errorCode;
        return grade;
    }

    public static PingGrade interpret(HashMap<String, PingStats> pingStatsHashMap, IPLayerInfo ipLayerInfo) {
        PingGrade pingGrade = new PingGrade();
        if (pingStatsHashMap == null || ipLayerInfo == null) {
            return pingGrade;
        }
        //Get external server stats
        pingGrade.primaryDns = ipLayerInfo.dns1;
        HashMap<String, PingStats> externalServerStats = new HashMap<>();
        if (ipLayerInfo.referenceExternalAddress1 != null) {
            externalServerStats.put(ipLayerInfo.referenceExternalAddress1,
                    pingStatsHashMap.get(ipLayerInfo.referenceExternalAddress1));
        }
        if (ipLayerInfo.referenceExternalAddress2 != null) {
            externalServerStats.put(ipLayerInfo.referenceExternalAddress2,
                    pingStatsHashMap.get(ipLayerInfo.referenceExternalAddress2));
        }

        //Get alternative DNS stats
        PingStats alternativeDnsStats1 = pingStatsHashMap.get(ipLayerInfo.publicDns1);
        PingStats alternativeDnsStats2 = pingStatsHashMap.get(ipLayerInfo.publicDns2);
        PingStats lowerAlternativeDnsStats = new PingStats(ipLayerInfo.publicDns1);
        if (alternativeDnsStats1.avgLatencyMs > 0) {
            lowerAlternativeDnsStats = alternativeDnsStats1;
            pingGrade.alternativeDns = ipLayerInfo.publicDns1;
        }
        if (alternativeDnsStats2.avgLatencyMs > 0) {
            if (alternativeDnsStats1.avgLatencyMs < 0 ||
                    alternativeDnsStats2.avgLatencyMs < alternativeDnsStats1.avgLatencyMs) {
                lowerAlternativeDnsStats = alternativeDnsStats2;
                pingGrade.alternativeDns = ipLayerInfo.publicDns2;
            }
        }

        //Router stats
        PingStats routerStats = pingStatsHashMap.get(ipLayerInfo.gateway);
        //Primary DNS stats
        PingStats primaryDnsStats = pingStatsHashMap.get(ipLayerInfo.dns1);

        double avgExternalServerLatencyMs = 0.0;
        double avgExternalServerLossPercent = 0.0;
        int count = 0;
        for(PingStats pingStats : externalServerStats.values()) {
            if (pingStats.avgLatencyMs > 0.0){
                avgExternalServerLatencyMs += pingStats.avgLatencyMs;
                avgExternalServerLossPercent += pingStats.lossRatePercent;
                count ++;
            }
        }
        if (count > 0) {
            avgExternalServerLatencyMs /= (double) count;
            avgExternalServerLossPercent /= (double) count;
        } else {
            avgExternalServerLossPercent = -1.0;
            avgExternalServerLatencyMs = 0.0;
        }


        pingGrade.externalServerLatencyMetric = getPingGradeLowerIsBetter(avgExternalServerLatencyMs,
                avgExternalServerLossPercent, PING_LATENCY_EXTSERVER_STEPS_MS);

        pingGrade.dnsServerLatencyMetric = getPingGradeLowerIsBetter(primaryDnsStats.avgLatencyMs,
                primaryDnsStats.lossRatePercent, PING_LATENCY_DNS_STEPS_MS);

        pingGrade.routerLatencyMetric = getPingGradeLowerIsBetter(routerStats.avgLatencyMs,
                routerStats.lossRatePercent, PING_LATENCY_ROUTER_STEPS_MS);

        pingGrade.alternativeDnsMetric = getPingGradeLowerIsBetter(lowerAlternativeDnsStats.avgLatencyMs,
                lowerAlternativeDnsStats.lossRatePercent, PING_LATENCY_DNS_STEPS_MS);


        //putting in the values
        pingGrade.routerLatencyMs = getEffectivePingLatency(routerStats.avgLatencyMs,
                routerStats.lossRatePercent);
        pingGrade.alternativeDnsLatencyMs = getEffectivePingLatency(lowerAlternativeDnsStats.avgLatencyMs,
                lowerAlternativeDnsStats.lossRatePercent);
        pingGrade.dnsServerLatencyMs = getEffectivePingLatency(primaryDnsStats.avgLatencyMs,
                primaryDnsStats.lossRatePercent);
        pingGrade.externalServerLatencyMs = getEffectivePingLatency(avgExternalServerLatencyMs, avgExternalServerLossPercent);

        return pingGrade;
    }

    public static HttpGrade interpret(PingStats httpRouterStats) {
        HttpGrade httpGrade = new HttpGrade();
        if (httpRouterStats == null) {
            return httpGrade;
        }
        httpGrade.httpDownloadLatencyMetric = getGradeLowerIsBetter(httpRouterStats.avgLatencyMs,
                HTTP_LATENCY_ROUTER_STEPS_MS,
                httpRouterStats.avgLatencyMs > 0.0,
                httpRouterStats.avgLatencyMs > MAX_LATENCY_FOR_BEING_NONFUNCTIONAL_MS);
        return  httpGrade;
    }

    public static WifiGrade interpret(HashMap<Integer, WifiState.ChannelInfo> wifiChannelInfo,
                                      DobbyWifiInfo linkInfo,
                                      @WifiState.WifiLinkMode int wifiProblemMode,
                                      @ConnectivityAnalyzer.WifiConnectivityMode int wifiConnectivityMode) {
        WifiGrade wifiGrade = new WifiGrade();
        // Figure out the # of APs on primary channel
        WifiState.ChannelInfo primaryChannelInfo = wifiChannelInfo.get(linkInfo.getFrequency());
        int numStrongInterferingAps = computeStrongInterferingAps(primaryChannelInfo);
        // Compute metrics for all channels -- for later use
        int leastOccupiedChannel = linkInfo.getFrequency(); // current ap channel
        int minOccupancyAPs = numStrongInterferingAps;
        for (WifiState.ChannelInfo channelInfo: wifiChannelInfo.values()) {
            int occupancy = computeStrongInterferingAps(channelInfo);
            if (occupancy < minOccupancyAPs) {
                leastOccupiedChannel = channelInfo.channelFrequency;
                minOccupancyAPs = occupancy;
            }
            wifiGrade.wifiChannelOccupancyMetric.put(channelInfo.channelFrequency, occupancy);
        }

        wifiGrade.primaryApChannelInterferingAps = numStrongInterferingAps;
        wifiGrade.primaryLinkChannelOccupancyMetric = getGradeLowerIsBetter(numStrongInterferingAps,
                WIFI_CHANNEL_OCCUPANCY_STEPS,
                (linkInfo.getFrequency() > 0 && numStrongInterferingAps >= 0), false);
        wifiGrade.primaryApSignalMetric = getGradeHigherIsBetter(linkInfo.getRssi(),
                WIFI_RSSI_STEPS_DBM, linkInfo.getRssi() < 0, false);
        wifiGrade.primaryApLinkSpeedMetric = getGradeHigherIsBetter(linkInfo.getLinkSpeed(),
                LINK_SPEED_STEPS_MBPS, linkInfo.getLinkSpeed() > 0, false);

        wifiGrade.wifiConnectivityMode = wifiConnectivityMode;
        wifiGrade.wifiLinkMode = wifiProblemMode;
        wifiGrade.primaryApSsid = linkInfo.getSSID();
        wifiGrade.primaryApChannel = linkInfo.getFrequency();
        wifiGrade.leastOccupiedChannel = leastOccupiedChannel;
        wifiGrade.leastOccupiedChannelAps = minOccupancyAPs;
        wifiGrade.primaryApSignal = linkInfo.getRssi();
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
    private static int getPingGradeLowerIsBetter(double latencyMs, double lossRatePercent, double[] steps) {
        double effectiveLatencyMs = getEffectivePingLatency(latencyMs, lossRatePercent);
        @MetricType int latencyMetric = getGradeLowerIsBetter(getEffectivePingLatency(latencyMs, lossRatePercent), steps,
                effectiveLatencyMs > 0, effectiveLatencyMs >= MAX_LATENCY_FOR_BEING_NONFUNCTIONAL_MS);
        return latencyMetric;
    }


    @MetricType
    private static int getGradeLowerIsBetter(double value, double[] steps, boolean isValid, boolean isNonFunctional) {
        if (!isValid) return MetricType.UNKNOWN;
        if (isNonFunctional) return MetricType.NONFUNCTIONAL;
        if (value < steps[0]) return MetricType.EXCELLENT;
        else if (value < steps[1]) return MetricType.GOOD;
        else if (value < steps[2]) return MetricType.AVERAGE;
        else if (value < steps[3]) return MetricType.POOR;
        else return MetricType.ABYSMAL;
    }

    @MetricType
    private static int getGradeHigherIsBetter(double value, double[] steps, boolean isValid, boolean isNonFunctional) {
        if (!isValid) return MetricType.UNKNOWN;
        if (isNonFunctional) return MetricType.NONFUNCTIONAL;
        if (value > steps[0]) return MetricType.EXCELLENT;
        else if (value > steps[1]) return MetricType.GOOD;
        else if (value > steps[2]) return MetricType.AVERAGE;
        else if (value > steps[3]) return MetricType.POOR;
        else return MetricType.ABYSMAL;
    }

    private static double getEffectivePingLatency(double latencyMs, double lossRatePercent) {
        if (lossRatePercent == 100.0) {
            return MAX_LATENCY_FOR_BEING_NONFUNCTIONAL_MS;
        }
        return latencyMs * (1.0 / (1.0 - (lossRatePercent/100)));
    }
}
