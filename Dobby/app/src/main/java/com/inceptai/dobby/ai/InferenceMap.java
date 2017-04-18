package com.inceptai.dobby.ai;

import android.support.annotation.IntDef;

import com.inceptai.dobby.wifi.WifiState;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.BitSet;
import java.util.Set;

import static com.inceptai.dobby.ai.InferenceMap.NetworkCondition.CONDITION_NONE_MAX;

/**
 * Created by arunesh on 4/14/17.
 */

public class InferenceMap {

    @IntDef({NetworkCondition.WIFI_CHANNEL_CONGESTION, NetworkCondition.WIFI_CHANNEL_BAD_SIGNAL,
            NetworkCondition.ROUTER_FAULT_WIFI_OK, NetworkCondition.ROUTER_WIFI_INTERFACE_FAULT,
            NetworkCondition.ROUTER_SOFTWARE_FAULT, NetworkCondition.ISP_INTERNET_SLOW_DNS_OK,
            NetworkCondition.ISP_INTERNET_DOWN, NetworkCondition.CABLE_MODEM_FAULT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface NetworkCondition {
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


    // Condition Type: (wifi problem, connectivity problem, latency).
    // Condition value:
    // Possible conditions, with probabilities.
    //

    /**
     *
     * @param wifiProblemMode
     * @return
     */
    public static PossibleConditions possibleConditionsFor(@WifiState.WifiStateProblemMode int wifiProblemMode) {
        if (wifiProblemMode == WifiState.WifiStateProblemMode.NO_PROBLEM_DEFAULT_STATE) {
            return PossibleConditions.NOOP_CONDITION;
        }

        if (wifiProblemMode == WifiState.WifiStateProblemMode.FREQUENT_DISCONNECTIONS) {
            int[] conditions = {NetworkCondition.WIFI_CHANNEL_BAD_SIGNAL,
                    NetworkCondition.WIFI_CHANNEL_CONGESTION};
            return new PossibleConditions(PossibleConditions.TYPE_INCLUSION, conditions);
        }

        return PossibleConditions.NOOP_CONDITION;
    }

    public static class PossibleConditions {
        public static final int TYPE_EXCLUSION = 1;
        public static final int TYPE_INCLUSION = 2;
        public static final int TYPE_NOOP = 3;
        public static final PossibleConditions NOOP_CONDITION = new PossibleConditions(TYPE_NOOP);

        private int conditionType;

        private BitSet bitSet = new BitSet((int) CONDITION_NONE_MAX);

        PossibleConditions(int conditionType) {
            this.conditionType = conditionType;
            bitSet.clear();
        }

        PossibleConditions(int conditionType, @NetworkCondition  int[] conditions) {
            this(conditionType);
            setConditions(conditions);
        }

        void setCondition(@NetworkCondition int condition) {
            bitSet.set(condition);
        }

        void setConditions(@NetworkCondition int[] conditions) {
            for (int i : conditions) {
                bitSet.set(i);
            }
        }
    }
}
