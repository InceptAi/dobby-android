package com.inceptai.dobby.ai;

import android.support.annotation.IntDef;

import com.inceptai.dobby.connectivity.ConnectivityAnalyzer;
import com.inceptai.dobby.wifi.WifiState;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

import static com.inceptai.dobby.ai.InferenceMap.Condition.ISP_INTERNET_DOWN;

/**
 * Created by arunesh on 4/14/17.
 */

public class InferenceMap {

    @IntDef({Condition.WIFI_CHANNEL_CONGESTION,
            Condition.WIFI_CHANNEL_BAD_SIGNAL,
            Condition.WIFI_INTERFACE_ON_PHONE_OFFLINE,
            Condition.WIFI_INTERFACE_ON_PHONE_IN_BAD_STATE,
            Condition.WIFI_INTERFACE_ON_PHONE_TURNED_OFF,
            Condition.WIFI_LINK_DHCP_ISSUE,
            Condition.WIFI_LINK_ASSOCIATION_ISSUE,
            Condition.WIFI_LINK_AUTH_ISSUE,
            Condition.ROUTER_FAULT_WIFI_OK,
            Condition.ROUTER_WIFI_INTERFACE_FAULT,
            Condition.ROUTER_SOFTWARE_FAULT,
            Condition.ROUTER_GOOD_SIGNAL_USING_SLOW_DATA_RATE,
            Condition.ISP_INTERNET_DOWN,
            Condition.ISP_INTERNET_SLOW,
            Condition.ISP_INTERNET_SLOW_DOWNLOAD,
            Condition.ISP_INTERNET_SLOW_UPLOAD,
            Condition.DNS_RESPONSE_SLOW,
            Condition.DNS_SLOW_TO_REACH,
            Condition.DNS_UNREACHABLE,
            Condition.CABLE_MODEM_FAULT,
            Condition.CAPTIVE_PORTAL_NO_INTERNET,
            Condition.REMOTE_SERVER_IS_SLOW_TO_RESPOND})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Condition {
        int WIFI_CHANNEL_CONGESTION = 10;
        int WIFI_CHANNEL_BAD_SIGNAL = 11;
        int WIFI_INTERFACE_ON_PHONE_OFFLINE = 12;
        int WIFI_INTERFACE_ON_PHONE_IN_BAD_STATE = 13;
        int WIFI_INTERFACE_ON_PHONE_TURNED_OFF = 14;
        int WIFI_LINK_DHCP_ISSUE = 15;
        int WIFI_LINK_ASSOCIATION_ISSUE = 16;
        int WIFI_LINK_AUTH_ISSUE = 17;
        int ROUTER_FAULT_WIFI_OK = 20;
        int ROUTER_WIFI_INTERFACE_FAULT = 21;
        int ROUTER_SOFTWARE_FAULT = 22;
        int ROUTER_GOOD_SIGNAL_USING_SLOW_DATA_RATE = 23;
        int ISP_INTERNET_DOWN = 31;
        int ISP_INTERNET_SLOW = 32;
        int ISP_INTERNET_SLOW_DOWNLOAD = 33;
        int ISP_INTERNET_SLOW_UPLOAD = 34;
        int DNS_RESPONSE_SLOW = 35;
        int DNS_SLOW_TO_REACH = 36;
        int DNS_UNREACHABLE = 37;
        int CABLE_MODEM_FAULT = 41;
        int CAPTIVE_PORTAL_NO_INTERNET = 50;
        int REMOTE_SERVER_IS_SLOW_TO_RESPOND = 51;
        int CONDITION_NONE_MAX = 64;  // For bitset size purposes only.
    }

    private static int[] WIFI_CONDITIONS = {
        Condition.WIFI_CHANNEL_CONGESTION,
        Condition.WIFI_CHANNEL_BAD_SIGNAL,
        Condition.WIFI_INTERFACE_ON_PHONE_OFFLINE,
        Condition.WIFI_INTERFACE_ON_PHONE_IN_BAD_STATE,
        Condition.WIFI_INTERFACE_ON_PHONE_TURNED_OFF,
        Condition.WIFI_LINK_DHCP_ISSUE,
        Condition.WIFI_LINK_ASSOCIATION_ISSUE,
        Condition.WIFI_LINK_AUTH_ISSUE,
        Condition.ROUTER_FAULT_WIFI_OK,
        Condition.ROUTER_WIFI_INTERFACE_FAULT,
        Condition.ROUTER_SOFTWARE_FAULT,
        Condition.ROUTER_GOOD_SIGNAL_USING_SLOW_DATA_RATE
    };

    private static int[] ISP_CONDITIONS = {
        Condition.ISP_INTERNET_DOWN,
        Condition.ISP_INTERNET_SLOW,
        Condition.ISP_INTERNET_SLOW_DOWNLOAD,
        Condition.ISP_INTERNET_SLOW_UPLOAD,
        Condition.REMOTE_SERVER_IS_SLOW_TO_RESPOND
    };

    private static int[] DNS_CONDITIONS = {
        Condition.DNS_RESPONSE_SLOW,
        Condition.DNS_SLOW_TO_REACH,
        Condition.DNS_UNREACHABLE,
    };


    private static int[] ROUTER_CONDITIONS = {
        Condition.ROUTER_FAULT_WIFI_OK,
        Condition.ROUTER_SOFTWARE_FAULT,
        Condition.ROUTER_WIFI_INTERFACE_FAULT,
    };


    /**
     * Exhaustive list of problems:
     *
     */
    public static PossibleConditions getPossibleConditionsFor(DataInterpreter.BandwidthGrade bandwidthGrade) {
        //Check if both download and upload are good
        PossibleConditions conditions = new PossibleConditions();

        if (DataInterpreter.isGoodOrExcellent(bandwidthGrade.getDownloadBandwidthMetric()) &&
                DataInterpreter.isGoodOrExcellent(bandwidthGrade.getUploadBandwidthMetric())) {
            conditions.exclude(ISP_CONDITIONS);
            //We can exclude WIFI CONDITIONS here but what about FREQUENCT DISCONNECTIONS etc.
            //TODO: Decide whether to exclude wifi conditions here
            //conditions.exclude(WIFI_CONDITIONS);
            return conditions;
        }


        if (DataInterpreter.isGoodOrExcellent(bandwidthGrade.getDownloadBandwidthMetric()) &&
                DataInterpreter.isAverageOrPoorOrNonFunctional(bandwidthGrade.getUploadBandwidthMetric())) {
            conditions.include(Condition.WIFI_CHANNEL_BAD_SIGNAL, 0.3);
            conditions.include(Condition.WIFI_CHANNEL_CONGESTION, 0.3);
            conditions.include(Condition.ISP_INTERNET_SLOW_UPLOAD, 0.3);
        } else if (DataInterpreter.isAverageOrPoorOrNonFunctional(bandwidthGrade.getDownloadBandwidthMetric()) &&
                DataInterpreter.isGoodOrExcellent(bandwidthGrade.getUploadBandwidthMetric())) {
            conditions.include(Condition.WIFI_CHANNEL_BAD_SIGNAL, 0.3);
            conditions.include(Condition.WIFI_CHANNEL_CONGESTION, 0.3);
            conditions.include(Condition.ISP_INTERNET_SLOW_DOWNLOAD, 0.3);
        } else if (DataInterpreter.isAverageOrPoor(bandwidthGrade.getDownloadBandwidthMetric()) &&
                DataInterpreter.isAverageOrPoor(bandwidthGrade.getUploadBandwidthMetric())) {
            conditions.include(Condition.WIFI_CHANNEL_BAD_SIGNAL, 0.3);
            conditions.include(Condition.WIFI_CHANNEL_CONGESTION, 0.3);
            conditions.include(Condition.ISP_INTERNET_SLOW, 0.3);
        } else if (DataInterpreter.isNonFunctional(bandwidthGrade.getDownloadBandwidthMetric()) ||
                DataInterpreter.isNonFunctional(bandwidthGrade.getUploadBandwidthMetric())) {
            conditions.include(Condition.WIFI_CHANNEL_BAD_SIGNAL, 0.25);
            conditions.include(Condition.WIFI_CHANNEL_CONGESTION, 0.25);
            conditions.include(Condition.DNS_UNREACHABLE, 0.25);
            conditions.include(ISP_INTERNET_DOWN, 0.25);
        }
        return conditions;
    }

    public static PossibleConditions getPossibleConditionsFor(DataInterpreter.WifiGrade wifiGrade) {
        //Check if both download and upload are good
        PossibleConditions conditions = new PossibleConditions();
        int[] allWifiMetrics = {wifiGrade.primaryApSignalMetric,
                wifiGrade.primaryApLinkSpeedMetric, wifiGrade.primaryLinkChannelOccupancyMetric};


        //All izz well -- from Three Idiots
        if (DataInterpreter.isGoodOrExcellent(allWifiMetrics) &&
                wifiGrade.wifiConnectivityMode == ConnectivityAnalyzer.WifiConnectivityMode.CONNECTED_AND_ONLINE &&
                wifiGrade.wifiProblemMode == WifiState.WifiLinkMode.NO_PROBLEM_DEFAULT_STATE) {
            conditions.exclude(WIFI_CONDITIONS);
            return conditions;
        }

        //Connectivity mode
        if (wifiGrade.wifiConnectivityMode == ConnectivityAnalyzer.WifiConnectivityMode.CONNECTED_AND_CAPTIVE_PORTAL) {
            //Connected but offline -- we are getting 302 download google.com
            conditions.include(Condition.CAPTIVE_PORTAL_NO_INTERNET, 1.0);
        } else if (wifiGrade.wifiConnectivityMode == ConnectivityAnalyzer.WifiConnectivityMode.CONNECTED_AND_OFFLINE) {
            //Connected but offline -- we can't download http://client3.google.com/204
            conditions.include(Condition.ISP_INTERNET_DOWN, 0.8);
            conditions.include(Condition.DNS_UNREACHABLE, 0.2);
        } else if (wifiGrade.wifiConnectivityMode == ConnectivityAnalyzer.WifiConnectivityMode.OFF) {
            //Wifi is off. Need to turn it on.
            conditions.include(Condition.WIFI_INTERFACE_ON_PHONE_TURNED_OFF, 1.0);
        }

        //Wifi signal
        if (DataInterpreter.isUnknown(wifiGrade.primaryApSignalMetric) ||
                wifiGrade.wifiProblemMode == WifiState.WifiLinkMode.HANGING_ON_SCANNING ||
                wifiGrade.wifiConnectivityMode == ConnectivityAnalyzer.WifiConnectivityMode.ON_AND_DISCONNECTED ||
                wifiGrade.wifiProblemMode == WifiState.WifiLinkMode.FREQUENT_DISCONNECTIONS ||
                wifiGrade.wifiProblemMode == WifiState.WifiLinkMode.HANGING_ON_AUTHENTICATING ||
                wifiGrade.wifiProblemMode == WifiState.WifiLinkMode.HANGING_ON_DHCP) {
            // Wifi is on but not connected to the router
            //It could be that client Wifi interface is not connecting or user needs to issue explicit command to connect
            conditions.include(Condition.WIFI_INTERFACE_ON_PHONE_IN_BAD_STATE, 0.3);
            //It could be that the right network is not showing up in the scans.
            conditions.include(Condition.ROUTER_SOFTWARE_FAULT, 0.7);
            conditions.include(Condition.ROUTER_WIFI_INTERFACE_FAULT, 0.7);
        }  else if (DataInterpreter.isPoorOrNonFunctional(wifiGrade.primaryApSignalMetric)) {
            //poor signal and high congestion
            conditions.include(Condition.WIFI_CHANNEL_BAD_SIGNAL, 1.0);
            if (DataInterpreter.isAverageOrPoorOrNonFunctional(wifiGrade.primaryLinkChannelOccupancyMetric)) {
                conditions.include(Condition.WIFI_CHANNEL_CONGESTION, 0.5);
            }
            if (wifiGrade.wifiProblemMode == WifiState.WifiLinkMode.FREQUENT_DISCONNECTIONS){
                conditions.include(Condition.ROUTER_SOFTWARE_FAULT, 0.8);
            } else if (wifiGrade.wifiProblemMode == WifiState.WifiLinkMode.HANGING_ON_AUTHENTICATING ||
                    wifiGrade.wifiProblemMode == WifiState.WifiLinkMode.HANGING_ON_DHCP){
                conditions.include(Condition.ROUTER_SOFTWARE_FAULT, 1.0);
            }
        } else if (DataInterpreter.isGoodOrExcellentorAverage(wifiGrade.primaryApSignalMetric)){
            conditions.exclude(Condition.WIFI_CHANNEL_BAD_SIGNAL);
            if (DataInterpreter.isAverageOrPoorOrNonFunctional(wifiGrade.primaryApLinkSpeedMetric)) {
                conditions.include(Condition.ROUTER_GOOD_SIGNAL_USING_SLOW_DATA_RATE, 0.2);
            }
            if (DataInterpreter.isAverageOrPoorOrNonFunctional(wifiGrade.primaryLinkChannelOccupancyMetric)) {
                conditions.include(Condition.WIFI_CHANNEL_CONGESTION, 0.2);
            }
            if (wifiGrade.wifiProblemMode == WifiState.WifiLinkMode.FREQUENT_DISCONNECTIONS ||
                    wifiGrade.wifiProblemMode == WifiState.WifiLinkMode.HANGING_ON_AUTHENTICATING ||
                    wifiGrade.wifiProblemMode == WifiState.WifiLinkMode.HANGING_ON_DHCP){
                conditions.include(Condition.ROUTER_SOFTWARE_FAULT, 1.0);
            }
        }

        /*
        //Based on wifi problem mode
        if (wifiGrade.wifiProblemMode == WifiState.WifiLinkMode.FREQUENT_DISCONNECTIONS) {
            conditions.include(Condition.WIFI_LINK_ASSOCIATION_ISSUE, 1.0);
        } else if (wifiGrade.wifiProblemMode == WifiState.WifiLinkMode.HANGING_ON_AUTHENTICATING) {
            conditions.include(Condition.WIFI_LINK_AUTH_ISSUE, 1.0);
        } else if (wifiGrade.wifiProblemMode == WifiState.WifiLinkMode.HANGING_ON_DHCP) {
            conditions.include(Condition.WIFI_LINK_DHCP_ISSUE, 1.0);
        }
        */
        return conditions;
    }

    public static PossibleConditions getPossibleConditionsFor(DataInterpreter.PingGrade pingGrade) {
        PossibleConditions conditions = new PossibleConditions();

        int[] allPingMetrics = {pingGrade.routerLatencyMetric, pingGrade.dnsServerLatencyMetric, pingGrade.externalServerLatencyMetric};
        if (DataInterpreter.isGoodOrExcellent(allPingMetrics)) {
            //conditions.exclude(ROUTER_CONDITIONS);
            conditions.exclude(DNS_CONDITIONS);
            return conditions;
        }
        //Router is quick
        if (DataInterpreter.isGoodOrExcellent(pingGrade.routerLatencyMetric)) {
            //conditions.exclude(ROUTER_CONDITIONS);
            if (DataInterpreter.isGoodOrExcellentorAverage(pingGrade.dnsServerLatencyMetric)) {

                conditions.exclude(DNS_CONDITIONS);

                //Good Router / dns latency but poor external server latency
                if (DataInterpreter.isPoor(pingGrade.externalServerLatencyMetric)) {
                    conditions.include(Condition.ISP_INTERNET_SLOW, 0.7);
                    conditions.include(Condition.CABLE_MODEM_FAULT, 0.3);
                    conditions.include(Condition.REMOTE_SERVER_IS_SLOW_TO_RESPOND, 0.05);
                } else if (DataInterpreter.isNonFunctional(pingGrade.externalServerLatencyMetric)) { // Use loss rates here instead of unknown
                    conditions.include(Condition.ISP_INTERNET_DOWN, 0.7);
                    conditions.include(Condition.CABLE_MODEM_FAULT, 0.3);
                }
            } else if (DataInterpreter.isPoor(pingGrade.dnsServerLatencyMetric)) {
                //Good router / slow dns
                conditions.include(Condition.DNS_SLOW_TO_REACH, 0.8);
                conditions.include(Condition.CABLE_MODEM_FAULT, 0.2);
            } else if (DataInterpreter.isNonFunctional(pingGrade.dnsServerLatencyMetric)) {
                conditions.include(Condition.DNS_UNREACHABLE, 0.8);
                conditions.include(Condition.CABLE_MODEM_FAULT, 0.2);
            }
        } else if (DataInterpreter.isAverage(pingGrade.routerLatencyMetric)){
            //Router has average latency to respond to ping
            if (DataInterpreter.isPoor(pingGrade.dnsServerLatencyMetric)) {
                //Good router / slow dns
                conditions.include(Condition.DNS_SLOW_TO_REACH, 0.5);
                conditions.include(Condition.CABLE_MODEM_FAULT, 0.2);
            } else if (DataInterpreter.isNonFunctional(pingGrade.dnsServerLatencyMetric)) {
                conditions.include(Condition.DNS_UNREACHABLE, 0.5);
                conditions.include(Condition.CABLE_MODEM_FAULT, 0.2);
            }
        } else if (DataInterpreter.isPoor(pingGrade.routerLatencyMetric)) {
            //Router has average latency to respond to ping
            conditions.include(Condition.WIFI_CHANNEL_CONGESTION, 0.2);
            conditions.include(Condition.WIFI_CHANNEL_BAD_SIGNAL, 0.8);
            conditions.include(Condition.ROUTER_SOFTWARE_FAULT, 0.2);
            if (DataInterpreter.isNonFunctional(pingGrade.dnsServerLatencyMetric)) {
                conditions.include(Condition.DNS_UNREACHABLE, 0.1);
            }
        } else if (DataInterpreter.isNonFunctional(pingGrade.routerLatencyMetric)) {
            //Router ping is Non functional -- loss rate could be 100%
            conditions.include(Condition.WIFI_CHANNEL_CONGESTION, 0.2);
            conditions.include(Condition.WIFI_CHANNEL_BAD_SIGNAL, 0.7);
            conditions.include(Condition.ROUTER_SOFTWARE_FAULT, 0.7);
        }
        return conditions;
    }

    public static PossibleConditions getPossibleConditionsFor(DataInterpreter.HttpGrade httpGrade) {
        PossibleConditions conditions = new PossibleConditions();

        if (DataInterpreter.isGoodOrExcellent(httpGrade.httpDownloadLatencyMetric)) {
            conditions.exclude(Condition.WIFI_CHANNEL_BAD_SIGNAL); // TODO: Confirm this with Arunesh -- Should we be doing this ?
            return conditions;
        }

        if (DataInterpreter.isAverageOrPoor(httpGrade.httpDownloadLatencyMetric)) {
            conditions.include(Condition.WIFI_CHANNEL_BAD_SIGNAL, 0.3);
            conditions.include(Condition.WIFI_CHANNEL_CONGESTION, 0.3);
            conditions.include(Condition.ROUTER_SOFTWARE_FAULT, 0.1);
        } else if (DataInterpreter.isNonFunctional(httpGrade.httpDownloadLatencyMetric)) {
            conditions.include(Condition.ROUTER_SOFTWARE_FAULT, 1.0);
        }
        return conditions;
    }

    public static String conditionString(@Condition int condition) {
        switch (condition) {
            case Condition.WIFI_CHANNEL_CONGESTION:
                return "WIFI_CHANNEL_CONGESTION";
            case Condition.WIFI_CHANNEL_BAD_SIGNAL:
                return "WIFI_CHANNEL_BAD_SIGNAL";
            case Condition.WIFI_INTERFACE_ON_PHONE_OFFLINE:
                return "WIFI_INTERFACE_ON_PHONE_OFFLINE";
            case Condition.WIFI_INTERFACE_ON_PHONE_IN_BAD_STATE:
                return "WIFI_INTERFACE_ON_PHONE_IN_BAD_STATE";
            case Condition.WIFI_INTERFACE_ON_PHONE_TURNED_OFF:
                return "WIFI_INTERFACE_ON_PHONE_TURNED_OFF";
            case Condition.WIFI_LINK_DHCP_ISSUE:
                return "WIFI_LINK_DHCP_ISSUE";
            case Condition.WIFI_LINK_ASSOCIATION_ISSUE:
                return "WIFI_LINK_ASSOCIATION_ISSUE";
            case Condition.WIFI_LINK_AUTH_ISSUE:
                return "WIFI_LINK_AUTH_ISSUE";
            case Condition.ROUTER_FAULT_WIFI_OK:
                return "ROUTER_FAULT_WIFI_OK";
            case Condition.ROUTER_WIFI_INTERFACE_FAULT:
                return "ROUTER_WIFI_INTERFACE_FAULT";
            case Condition.ROUTER_SOFTWARE_FAULT:
                return "ROUTER_SOFTWARE_FAULT";
            case Condition.ROUTER_GOOD_SIGNAL_USING_SLOW_DATA_RATE:
                return "ROUTER_GOOD_SIGNAL_USING_SLOW_DATA_RATE";
            case Condition.ISP_INTERNET_SLOW:
                return "ISP_INTERNET_SLOW";
            case Condition.ISP_INTERNET_SLOW_DOWNLOAD:
                return "ISP_INTERNET_SLOW_DOWNLOAD";
            case Condition.ISP_INTERNET_SLOW_UPLOAD:
                return "ISP_INTERNET_SLOW_UPLOAD";
            case Condition.DNS_RESPONSE_SLOW:
                return "DNS_RESPONSE_SLOW";
            case Condition.DNS_SLOW_TO_REACH:
                return "DNS_SLOW_TO_REACH";
            case Condition.DNS_UNREACHABLE:
                return "DNS_UNREACHABLE";
            case Condition.CAPTIVE_PORTAL_NO_INTERNET:
                return "CAPTIVE_PORTAL_NO_INTERNET";
            case Condition.ISP_INTERNET_DOWN:
                return "ISP_INTERNET_DOWN";
            case Condition.CABLE_MODEM_FAULT:
                return "CABLE_MODEM_FAULT";
            case Condition.REMOTE_SERVER_IS_SLOW_TO_RESPOND:
                return "REMOTE_SERVER_IS_SLOW_TO_RESPOND";
        }
        return "UNKNOWN VALUE";
    }

    public static String toString(Set<Integer> conditions) {
        StringBuilder builder = new StringBuilder("Conditions: ");
        for (int condition : conditions) {
            builder.append(conditionString(condition));
            builder.append(" ");
        }
        return builder.toString();
    }
}
