package com.inceptai.wifiexpertsystem.expertSystem.inferencing;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.support.annotation.IntDef;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.inceptai.wifiexpertsystem.utils.Utils;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ConnectivityTester;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ping.IPLayerInfo;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ping.PingStats;
import com.inceptai.wifimonitoringservice.actionlibrary.utils.ActionLibraryCodes;
import com.inceptai.wifimonitoringservice.utils.WifiStateData;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
            -65.0, /* good */
            -80.0, /* average */
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
        String isp = Utils.EMPTY_STRING;;
        String externalIP = Utils.EMPTY_STRING;;
        String bestServerName = Utils.EMPTY_STRING;
        String bestServerCountry = Utils.EMPTY_STRING;
        double bestServerLatencyMs;
        double lat;
        double lon;
        @ActionLibraryCodes.ErrorCodes int errorCode = ActionLibraryCodes.ErrorCodes.ERROR_UNINITIAlIZED;
        private String errorCodeString = Utils.EMPTY_STRING;
        private String downloadMetricString = Utils.EMPTY_STRING;
        private String uploadMetricString  = Utils.EMPTY_STRING;

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
            builder.append("\n lat: " + lat);
            builder.append("\n lon: " + lon);
            builder.append("\n bestServerName: " + bestServerName);
            builder.append("\n bestServerCountry: " + bestServerCountry);
            builder.append("\n bestServerLatencyMs: " + bestServerLatencyMs);
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
        String routerIp;
        String ownIp;
        String netmask;
        int leaseDuration;
        double routerLatencyMs;
        double dnsServerLatencyMs;
        double externalServerLatencyMs;
        double alternativeDnsLatencyMs;
        long updatedAtMs;
        @ActionLibraryCodes.ErrorCodes int errorCode = ActionLibraryCodes.ErrorCodes.ERROR_UNINITIAlIZED;
        private String errorCodeString = Utils.EMPTY_STRING;
        private String externalServerMetricString = Utils.EMPTY_STRING;
        private String dnsServerMetricString = Utils.EMPTY_STRING;
        private String routerMetricString = Utils.EMPTY_STRING;
        private String alternativeDnsMetricString = Utils.EMPTY_STRING;

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
        @ActionLibraryCodes.ErrorCodes int errorCode = ActionLibraryCodes.ErrorCodes.ERROR_UNINITIAlIZED;
        private String errorCodeString = Utils.EMPTY_STRING;
        private String httpDownloadMetricString = Utils.EMPTY_STRING;
        private double httpDownloadLatencyMs;

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
        @WifiStateData.WifiProblemMode int wifiProblemMode;
        @ConnectivityTester.WifiConnectivityMode int wifiConnectivityMode;
        String primaryApBSSID;
        String primaryApSsid;
        int primaryApChannel;
        int leastOccupiedChannel;
        int primaryApChannelInterferingAps;
        int leastOccupiedChannelAps;
        int primaryApSignal;
        int linkSpeed;
        List<ScanResult> scanResultList;
        List<WifiConfiguration> wifiConfigurationList;
        HashMap<String, Utils.PercentileStats> detailedNetworkStateStats;
        HashMap<Long, String> networkStateTransitions;
        private long updatedAtMs;

        public WifiGrade() {
            scanResultList = new ArrayList<>();
            wifiChannelOccupancyMetric = new HashMap<>();
            detailedNetworkStateStats = new HashMap<>();
            networkStateTransitions = new HashMap<>();
            wifiConfigurationList = new ArrayList<>();
        }

        public String toJson() {
            Gson gson = new GsonBuilder()
                        .setExclusionStrategies(new TestExclStrat())
                        .create();
            return gson.toJson(this);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("WifiGrade: Primary AP signalMetric" +
                    metricTypeToString(primaryApSignalMetric) +
                    ", SpeedMetric: " + metricTypeToString(primaryApLinkSpeedMetric) +
                    ", channelOccupancy: " +
                    metricTypeToString(primaryLinkChannelOccupancyMetric));
            builder.append("\nChannel map:" + wifiChannelOccupancyMetric.toString());
            builder.append("\n primaryApChannel:" + primaryApChannel);
            builder.append("\n primaryApSignal:" + primaryApSignal);
            builder.append("\n leastOccupiedChannel:" + leastOccupiedChannel);
            builder.append("\n primaryApChannelAps:" + primaryApChannelInterferingAps);
            builder.append("\n leastOccupiedChannelAps:" + leastOccupiedChannelAps);
            builder.append("\n linkSpeed:" + linkSpeed);
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


        public boolean isWifiOff() {
            return (ConnectivityTester.isOff(wifiConnectivityMode));
        }

        public boolean isWifiDisconnected() {
            return (ConnectivityTester.isDisconnected(wifiConnectivityMode));
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

        public class TestExclStrat implements ExclusionStrategy {
            public boolean shouldSkipClass(Class<?> arg0) {
                return false;
            }

            public boolean shouldSkipField(FieldAttributes f) {
                return (f.getDeclaringClass() == ScanResult.class && (f.getName().equals("wifiSsid") || f.getName().equals("informationElements")));
            }
        }

        private HashMap<Integer, ChannelInfo> getChannelInfo(List<ScanResult> scanResultList) {
            HashMap<Integer, ChannelInfo> channelInfoHashMap = new HashMap<>();
            for (ScanResult scanResult : scanResultList) {
                //Update the channel info stuff
                ChannelInfo channelInfo = channelInfoHashMap.get(scanResult.frequency);
                if (channelInfo == null) {
                    channelInfo = new ChannelInfo(scanResult.frequency);
                    channelInfoHashMap.put(scanResult.frequency, channelInfo);
                }
                channelInfo.numberAPs++;
            }
            return channelInfoHashMap;
        }

        public class ChannelInfo {
            int channelFrequency;
            int numberAPs;
            int similarStrengthAPs;
            int higherStrengthAps;
            int highestStrengthAps;
            int lowerStrengthAps;
            int lowestStrengthAps;
            double contentionMetric;

            ChannelInfo(int channelFrequency) {
                this.channelFrequency = channelFrequency;
            }

            public String toString() {
                return toJson();
            }

            String toJson() {
                Gson gson = new Gson();
                return gson.toJson(this);
            }

            public int getNumberAPs() {
                return numberAPs;
            }

            public int getNumberOfInterferingAPs() {
                return numberAPs;
            }
        }



    }


    @MetricType
    public static int getSignalMetric(int signal) {
        return getGradeHigherIsBetter(signal, WIFI_RSSI_STEPS_DBM, signal < 0, false);
    }

    public static String convertSignalToReadableMessage(int signal) {
        @MetricType int metric = getSignalMetric(signal);
        if (DataInterpreter.isUnknown(metric)) {
            return Utils.EMPTY_STRING;
        }
        int signalPercent = Utils.convertSignalDbmToPercent(signal);
        String message = Utils.EMPTY_STRING;
        if (DataInterpreter.isPoorOrAbysmalOrNonFunctional(metric)) {
            message = "Your connection to your wifi is weak, at about " + signalPercent +
                    "% strength (100% means very high signal, usually when you " +
                    "are right next to wifi router).";
        } else if (DataInterpreter.isAverage(metric)) {
            message = "Your connection to your wifi is just ok, at about " + signalPercent +
                    "% strength (100% means very high signal, usually when you " +
                    "are right next to wifi router).";
        } else if (DataInterpreter.isGoodOrExcellent(metric)) {
            message = "Your connection to your wifi is really good, at about " + signalPercent +
                    "% strength (100% means very high signal, usually when you " +
                    "are right next to wifi router).";
        }
        return message;
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
                                           double lat, double lon,
                                           String bestServerName, String bestServerCountry,
                                           double bestServerLatency,
                                           int errorCode) {
        BandwidthGrade grade = new BandwidthGrade();
        @MetricType int downloadMetric = getGradeHigherIsBetter(downloadMbps, BW_DOWNLOAD_STEPS_MBPS, downloadMbps >= 0.0, downloadMbps == 0.0);
        @MetricType int uploadMetric = getGradeHigherIsBetter(uploadMbps, BW_UPLOAD_STEPS_MBPS, uploadMbps >= 0.0, uploadMbps == 0.0);
        grade.updateUploadInfo(uploadMbps, uploadMetric);
        grade.updateDownloadInfo(downloadMbps, downloadMetric);
        grade.isp = isp;
        grade.externalIP = externalClientIp;
        grade.lat = lat;
        grade.lon = lon;
        grade.bestServerCountry = bestServerCountry;
        grade.bestServerName = bestServerName;
        grade.bestServerLatencyMs = bestServerLatency;
        grade.errorCode = errorCode;
        grade.errorCodeString = ActionLibraryCodes.bandwidthTestErrorCodesToStrings(grade.errorCode);
        grade.downloadMetricString = DataInterpreter.metricTypeToString(grade.downloadBandwidthMetric);
        grade.uploadMetricString = DataInterpreter.metricTypeToString(grade.uploadBandwidthMetric);
        return grade;
    }



    public static PingGrade interpret(HashMap<String, PingStats> pingStatsHashMap) {
        PingGrade pingGrade = new PingGrade();
        pingGrade.errorCode = ActionLibraryCodes.ErrorCodes.NO_ERROR;
        IPLayerInfo ipLayerInfo = getIPLayerInfoFromPingStats(pingStatsHashMap);
        if (ipLayerInfo != null) {
            pingGrade.ownIp = ipLayerInfo.ownIPAddress;
            pingGrade.netmask = ipLayerInfo.netMask;
            pingGrade.leaseDuration = ipLayerInfo.leaseDuration;
            pingGrade.routerIp = ipLayerInfo.gateway;
            pingGrade.primaryDns = ipLayerInfo.dns1;
        }

        if (ipLayerInfo == null || ipLayerInfo.gateway == null || ipLayerInfo.gateway.equals("0.0.0.0")) {
            pingGrade.errorCode = ActionLibraryCodes.ErrorCodes.ERROR_DHCP_INFO_UNAVAILABLE;
            pingGrade.errorCodeString = ActionLibraryCodes.bandwidthTestErrorCodesToStrings(pingGrade.errorCode);
            return pingGrade;
        } else if (pingStatsHashMap == null) {
            pingGrade.errorCode = ActionLibraryCodes.ErrorCodes.ERROR_DHCP_INFO_UNAVAILABLE;
            pingGrade.errorCodeString = ActionLibraryCodes.bandwidthTestErrorCodesToStrings(pingGrade.errorCode);
            return pingGrade;
        } else if (pingStatsHashMap.isEmpty()) {
            pingGrade.errorCode = ActionLibraryCodes.ErrorCodes.ERROR_PERFORMING_PING;
            pingGrade.errorCodeString = ActionLibraryCodes.bandwidthTestErrorCodesToStrings(pingGrade.errorCode);
            return pingGrade;
        }

        //Get external server stats
        HashMap<String, PingStats> externalServerStats = new HashMap<>();
        if (!Utils.isNullOrEmpty(ipLayerInfo.referenceExternalAddress1)) {
            externalServerStats.put(ipLayerInfo.referenceExternalAddress1,
                    safeAccessHashMap(pingStatsHashMap, ipLayerInfo.referenceExternalAddress1));
        }
        if (!Utils.isNullOrEmpty(ipLayerInfo.referenceExternalAddress2)) {
            externalServerStats.put(ipLayerInfo.referenceExternalAddress2,
                    safeAccessHashMap(pingStatsHashMap, ipLayerInfo.referenceExternalAddress2));
        }

        //Get alternative PRIMARY_DNS stats
        PingStats alternativeDnsStats1 = safeAccessHashMap(pingStatsHashMap, ipLayerInfo.publicDns1);
        PingStats alternativeDnsStats2 = safeAccessHashMap(pingStatsHashMap, ipLayerInfo.publicDns2);
        PingStats lowerAlternativeDnsStats = new PingStats(ipLayerInfo.publicDns1);
        if (alternativeDnsStats2 != null) {
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
        }

        //Router stats
        PingStats routerStats = safeAccessHashMap(pingStatsHashMap, ipLayerInfo.gateway);
        //Primary PRIMARY_DNS stats
        PingStats primaryDnsStats = safeAccessHashMap(pingStatsHashMap, ipLayerInfo.dns1);

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
        //Grade strings
        pingGrade.errorCodeString = ActionLibraryCodes.bandwidthTestErrorCodesToStrings(pingGrade.errorCode);
        pingGrade.routerMetricString = DataInterpreter.metricTypeToString(pingGrade.routerLatencyMetric);
        pingGrade.dnsServerMetricString = DataInterpreter.metricTypeToString(pingGrade.dnsServerLatencyMetric);
        pingGrade.externalServerMetricString = DataInterpreter.metricTypeToString(pingGrade.externalServerLatencyMetric);
        pingGrade.alternativeDnsMetricString = DataInterpreter.metricTypeToString(pingGrade.alternativeDnsMetric);

        return pingGrade;
    }

    public static HttpGrade interpret(PingStats httpRouterStats) {
        HttpGrade httpGrade = new HttpGrade();
        httpGrade.errorCode = ActionLibraryCodes.ErrorCodes.NO_ERROR;
        httpGrade.errorCodeString = ActionLibraryCodes.bandwidthTestErrorCodesToStrings(httpGrade.errorCode);
        if (httpRouterStats == null || httpRouterStats.ipAddress.equals("0.0.0.0")) {
            httpGrade.errorCode = ActionLibraryCodes.ErrorCodes.ERROR_DHCP_INFO_UNAVAILABLE;
            httpGrade.errorCodeString = ActionLibraryCodes.bandwidthTestErrorCodesToStrings(httpGrade.errorCode);
            return httpGrade;
        }
        httpGrade.httpDownloadLatencyMs = httpRouterStats.avgLatencyMs;
        httpGrade.httpDownloadLatencyMetric = getGradeLowerIsBetter(httpRouterStats.avgLatencyMs,
                HTTP_LATENCY_ROUTER_STEPS_MS,
                httpRouterStats.avgLatencyMs > 0.0,
                httpRouterStats.avgLatencyMs > MAX_LATENCY_FOR_BEING_NONFUNCTIONAL_MS);
        httpGrade.httpDownloadMetricString = DataInterpreter.metricTypeToString(httpGrade.httpDownloadLatencyMetric);
        return  httpGrade;
    }

    public static WifiGrade interpret(int primaryApSignal,
                                      String primaryApSSID,
                                      int primaryApLinkSpeed,
                                      int primaryApChannel,
                                      int primaryApChannelInterferingAps,
                                      List<ScanResult> scanResultList,
                                      List<WifiConfiguration> wifiConfigurationList,
                                      HashMap<Long, String> networkStateTransitions,
                                      @WifiStateData.WifiProblemMode int wifiProblemMode,
                                      @ConnectivityTester.WifiConnectivityMode int wifiConnectivityMode) {
        WifiGrade wifiGrade = new WifiGrade();
        // Compute metrics for all channels -- for later use
        int leastOccupiedChannel = primaryApChannel; // current ap channel
        int minOccupancyAPs = primaryApChannelInterferingAps;
        HashMap<Integer, WifiGrade.ChannelInfo> channelInfoHashMap = wifiGrade.getChannelInfo(scanResultList);
        for (WifiGrade.ChannelInfo channelInfo: channelInfoHashMap.values()) {
            if (channelInfo != null) {
                int occupancy = channelInfo.getNumberOfInterferingAPs();
                if (occupancy < minOccupancyAPs) {
                    leastOccupiedChannel = channelInfo.channelFrequency;
                    minOccupancyAPs = occupancy;
                }
                if (occupancy > 0) {
                    wifiGrade.wifiChannelOccupancyMetric.put(channelInfo.channelFrequency, occupancy);
                }
            }
        }
        wifiGrade.wifiConnectivityMode = wifiConnectivityMode;
        wifiGrade.linkSpeed = primaryApLinkSpeed;
        wifiGrade.wifiProblemMode = wifiProblemMode;
        wifiGrade.primaryApSsid = primaryApSSID;
        wifiGrade.primaryApChannel = primaryApChannel;
        wifiGrade.leastOccupiedChannel = leastOccupiedChannel;
        wifiGrade.leastOccupiedChannelAps = minOccupancyAPs;
        wifiGrade.primaryApSignal = primaryApSignal;
        wifiGrade.scanResultList = scanResultList;
        wifiGrade.wifiConfigurationList = wifiConfigurationList;
        wifiGrade.networkStateTransitions = networkStateTransitions;

        //Compute metrics
        wifiGrade.primaryApChannelInterferingAps = primaryApChannelInterferingAps;
        wifiGrade.primaryLinkChannelOccupancyMetric = getGradeLowerIsBetter(primaryApChannelInterferingAps,
                WIFI_CHANNEL_OCCUPANCY_STEPS,
                (primaryApChannel > 0 && primaryApChannelInterferingAps >= 0), false);
        wifiGrade.primaryApSignalMetric = getGradeHigherIsBetter(primaryApSignal,
                WIFI_RSSI_STEPS_DBM, primaryApSignal < 0, false);
        wifiGrade.primaryApLinkSpeedMetric = getGradeHigherIsBetter(primaryApLinkSpeed,
                LINK_SPEED_STEPS_MBPS, primaryApLinkSpeed > 0, false);
        wifiGrade.primaryApBSSID = primaryApSSID;
        //Needs a network call -- put it in python script
        //wifiGrade.getPrimaryApMake = Utils.getWifiManufacturer(wifiGrade.primaryApBSSID);
        return wifiGrade;
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

    private static IPLayerInfo getIPLayerInfoFromPingStats(HashMap<String, PingStats> pingStatsHashMap) {
        IPLayerInfo ipLayerInfo = null;
        if (pingStatsHashMap != null) {
            ipLayerInfo = new IPLayerInfo();
            for (PingStats pingStats : pingStatsHashMap.values()) {
                switch (pingStats.ipAddressType) {
                    case PingStats.IPAddressType.PRIMARY_IP:
                        ipLayerInfo.ownIPAddress = pingStats.ipAddress;
                        break;
                    case PingStats.IPAddressType.PRIMARY_DNS:
                        ipLayerInfo.dns1 = pingStats.ipAddress;
                        break;
                    case  PingStats.IPAddressType.GATEWAY:
                        ipLayerInfo.gateway = pingStats.ipAddress;
                        break;
                    case PingStats.IPAddressType.EXTERNAL_DNS:
                        ipLayerInfo.dns1 = pingStats.ipAddress;
                        break;
                    case PingStats.IPAddressType.EXTERNAL_SERVER:
                        ipLayerInfo.referenceExternalAddress1 = pingStats.ipAddress;
                        break;
                    case PingStats.IPAddressType.UNINITIALIZED:
                    case PingStats.IPAddressType.UNKNOWN:
                    default:
                        break;
                }
            }
        }
        return ipLayerInfo;
    }

    private static PingStats safeAccessHashMap(HashMap<String, PingStats> pingStatsHashMap, String ipAddress) {
        if (pingStatsHashMap == null || ipAddress == null) {
            return null;
        }
        PingStats pingStats = pingStatsHashMap.get(ipAddress);
        if (pingStats == null) {
            return new PingStats(ipAddress);
        }
        return pingStats;
    }

}
