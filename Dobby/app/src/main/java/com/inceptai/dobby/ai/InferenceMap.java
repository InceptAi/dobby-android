package com.inceptai.dobby.ai;

import android.support.annotation.IntDef;

import com.inceptai.dobby.connectivity.ConnectivityAnalyzer;
import com.inceptai.dobby.wifi.WifiState;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

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
            Condition.ISP_INTERNET_SLOW_DNS_OK,
            Condition.ISP_INTERNET_DOWN,
            Condition.CABLE_MODEM_FAULT})
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
        int ISP_INTERNET_SLOW_DNS_OK = 30;
        int ISP_INTERNET_DOWN = 31;
        int CABLE_MODEM_FAULT = 40;
        int CONDITION_NONE_MAX = 64;  // For bitset size purposes only.
    }

    /**
     * Exhaustive list of problems:
     *
     */
    public static Set<Integer> getPossibleConditionsFor(DataInterpreter.BandwidthGrade interpretation) {
        return null;
    }

    /**
     *
     * @param wifiProblemMode
     * @return
     */
    public static PossibleConditions possibleConditionsFor(@WifiState.WifiLinkMode int wifiProblemMode) {
        if (wifiProblemMode == WifiState.WifiLinkMode.NO_PROBLEM_DEFAULT_STATE) {
            return PossibleConditions.NOOP_CONDITION;
        }

        if (wifiProblemMode == WifiState.WifiLinkMode.FREQUENT_DISCONNECTIONS) {
            PossibleConditions conditions = new PossibleConditions();
            conditions.include(Condition.WIFI_CHANNEL_BAD_SIGNAL, 0.3);
            conditions.include(Condition.WIFI_CHANNEL_CONGESTION, 0.3);
            conditions.include(Condition.WIFI_INTERFACE_ON_PHONE_IN_BAD_STATE, 0.3);
            return conditions;
        }

        if (wifiProblemMode == WifiState.WifiLinkMode.HANGING_ON_AUTHENTICATING) {
            PossibleConditions conditions = new PossibleConditions();
            conditions.include(Condition.WIFI_CHANNEL_BAD_SIGNAL, 0.3);
            conditions.include(Condition.WIFI_CHANNEL_CONGESTION, 0.3);
            conditions.include(Condition.WIFI_INTERFACE_ON_PHONE_IN_BAD_STATE, 0.3);
            return conditions;
        }

        if (wifiProblemMode == WifiState.WifiLinkMode.HANGING_ON_DHCP) {
            PossibleConditions conditions = new PossibleConditions();
            conditions.include(Condition.WIFI_CHANNEL_BAD_SIGNAL, 0.3);
            conditions.include(Condition.WIFI_CHANNEL_CONGESTION, 0.3);
            conditions.include(Condition.WIFI_INTERFACE_ON_PHONE_IN_BAD_STATE, 0.3);
            return conditions;
        }

        if (wifiProblemMode == WifiState.WifiLinkMode.HANGING_ON_SCANNING) {
            PossibleConditions conditions = new PossibleConditions();
            conditions.include(Condition.WIFI_CHANNEL_BAD_SIGNAL, 0.3);
            conditions.include(Condition.WIFI_CHANNEL_CONGESTION, 0.3);
            conditions.include(Condition.WIFI_INTERFACE_ON_PHONE_IN_BAD_STATE, 0.3);
            return conditions;
        }

        if (wifiProblemMode == WifiState.WifiLinkMode.UNKNOWN) {
            PossibleConditions conditions = new PossibleConditions();
            conditions.include(Condition.WIFI_CHANNEL_BAD_SIGNAL, 0.3);
            conditions.include(Condition.WIFI_CHANNEL_CONGESTION, 0.3);
            conditions.include(Condition.WIFI_INTERFACE_ON_PHONE_IN_BAD_STATE, 0.3);
            return conditions;
        }

        return PossibleConditions.NOOP_CONDITION;
    }

    /**
     *
     * @param wifiConnectivityMode
     * @return
     */
    public static PossibleConditions possibleConditionsFor(@ConnectivityAnalyzer.WifiConnectivityMode int wifiConnectivityMode) {
        if (wifiConnectivityMode == ConnectivityAnalyzer.WifiConnectivityMode.CONNECTED_AND_ONLINE) {
            return PossibleConditions.NOOP_CONDITION;
        }

        if (wifiConnectivityMode == ConnectivityAnalyzer.WifiConnectivityMode.CONNECTED_AND_OFFLINE) {
            PossibleConditions conditions = new PossibleConditions();
            conditions.include(Condition.WIFI_CHANNEL_BAD_SIGNAL, 0.3);
            conditions.include(Condition.WIFI_CHANNEL_CONGESTION, 0.3);
            conditions.include(Condition.WIFI_INTERFACE_ON_PHONE_IN_BAD_STATE, 0.3);
            return conditions;
        }

        if (wifiConnectivityMode == ConnectivityAnalyzer.WifiConnectivityMode.CONNECTED_AND_CAPTIVE_PORTAL) {
            PossibleConditions conditions = new PossibleConditions();
            conditions.include(Condition.WIFI_CHANNEL_BAD_SIGNAL, 0.3);
            conditions.include(Condition.WIFI_CHANNEL_CONGESTION, 0.3);
            conditions.include(Condition.WIFI_INTERFACE_ON_PHONE_IN_BAD_STATE, 0.3);
            return conditions;
        }

        if (wifiConnectivityMode == ConnectivityAnalyzer.WifiConnectivityMode.ON_AND_DISCONNECTED) {
            PossibleConditions conditions = new PossibleConditions();
            conditions.include(Condition.WIFI_CHANNEL_BAD_SIGNAL, 0.3);
            conditions.include(Condition.WIFI_CHANNEL_CONGESTION, 0.3);
            conditions.include(Condition.WIFI_INTERFACE_ON_PHONE_IN_BAD_STATE, 0.3);
            return conditions;
        }

        if (wifiConnectivityMode == ConnectivityAnalyzer.WifiConnectivityMode.OFF) {
            PossibleConditions conditions = new PossibleConditions();
            conditions.include(Condition.WIFI_CHANNEL_BAD_SIGNAL, 0.3);
            conditions.include(Condition.WIFI_CHANNEL_CONGESTION, 0.3);
            conditions.include(Condition.WIFI_INTERFACE_ON_PHONE_IN_BAD_STATE, 0.3);
            return conditions;
        }

        if (wifiConnectivityMode == WifiState.WifiLinkMode.UNKNOWN) {
            PossibleConditions conditions = new PossibleConditions();
            conditions.include(Condition.WIFI_CHANNEL_BAD_SIGNAL, 0.3);
            conditions.include(Condition.WIFI_CHANNEL_CONGESTION, 0.3);
            conditions.include(Condition.WIFI_INTERFACE_ON_PHONE_IN_BAD_STATE, 0.3);
            return conditions;
        }

        return PossibleConditions.NOOP_CONDITION;
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
            case Condition.ISP_INTERNET_SLOW_DNS_OK:
                return "ISP_INTERNET_SLOW_DNS_OK";
            case Condition.ISP_INTERNET_DOWN:
                return "ISP_INTERNET_DOWN";
            case Condition.CABLE_MODEM_FAULT:
                return "CABLE_MODEM_FAULT";
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
