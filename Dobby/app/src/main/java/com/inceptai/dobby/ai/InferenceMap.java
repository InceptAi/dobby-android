package com.inceptai.dobby.ai;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

/**
 * Created by arunesh on 4/14/17.
 */

public class InferenceMap {

    @IntDef({NetworkCondition.WIFI_CONGESTION, NetworkCondition.WIFI_BAD_SIGNAL,
            NetworkCondition.ROUTER_FAULT_WIFI_OK, NetworkCondition.ROUTER_WIFI_INTERFACE_FAULT,
            NetworkCondition.ROUTER_SOFTWARE_FAULT, NetworkCondition.ISP_INTERNET_SLOW_DNS_OK,
            NetworkCondition.ISP_INTERNET_DOWN, NetworkCondition.CABLE_MODEM_FAULT})
    @Retention(RetentionPolicy.SOURCE)
    public @interface NetworkCondition {
        int WIFI_CONGESTION = 10;
        int WIFI_BAD_SIGNAL = 11;
        int ROUTER_FAULT_WIFI_OK = 20;
        int ROUTER_WIFI_INTERFACE_FAULT = 21;
        int ROUTER_SOFTWARE_FAULT = 22;
        int ISP_INTERNET_SLOW_DNS_OK = 30;
        int ISP_INTERNET_DOWN = 31;
        int CABLE_MODEM_FAULT = 40;
    }

    public static Set<Integer> getPossibleConditionsFor(DataInterpreter.BandwidthGrade interpretation) {
        return null;
    }
}
