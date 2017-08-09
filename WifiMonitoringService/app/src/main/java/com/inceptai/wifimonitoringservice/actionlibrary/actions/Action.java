package com.inceptai.wifimonitoringservice.actionlibrary.actions;

import android.content.Context;
import android.support.annotation.IntDef;

import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.NetworkActionLayer;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;


/**
 * Created by vivek on 7/5/17.
 */



public abstract class Action {
    public static final long ACTION_TIMEOUT_MS = 30000;
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ActionType.CHECK_IF_5GHz_IS_SUPPORTED,
            ActionType.CONNECT_AND_TEST_GIVEN_WIFI_NETWORK,
            ActionType.CONNECT_TO_BEST_CONFIGURED_NETWORK,
            ActionType.CONNECT_WITH_GIVEN_WIFI_NETWORK,
            ActionType.DISCONNECT_FROM_CURRENT_WIFI,
            ActionType.FORGET_WIFI_NETWORK,
            ActionType.GET_BEST_CONFIGURED_NETWORK,
            ActionType.GET_BEST_CONFIGURED_NETWORKS,
            ActionType.GET_CONFIGURED_NETWORKS,
            ActionType.GET_DHCP_INFO,
            ActionType.GET_NEARBY_WIFI_NETWORKS,
            ActionType.GET_WIFI_INFO,
            ActionType.ITERATE_AND_CONNECT_TO_BEST_NETWORK,
            ActionType.ITERATE_AND_REPAIR_WIFI_NETWORK,
            ActionType.PERFORM_BANDWIDTH_TEST,
            ActionType.PERFORM_CONNECTIVITY_TEST,
            ActionType.RESET_CONNECTION_WITH_CURRENT_WIFI,
            ActionType.TOGGLE_WIFI,
            ActionType.TURN_WIFI_OFF,
            ActionType.TURN_WIFI_ON})

    public @interface ActionType {
        int CHECK_IF_5GHz_IS_SUPPORTED = 0;
        int CONNECT_AND_TEST_GIVEN_WIFI_NETWORK = 1;
        int CONNECT_TO_BEST_CONFIGURED_NETWORK = 2;
        int CONNECT_WITH_GIVEN_WIFI_NETWORK = 3;
        int DISCONNECT_FROM_CURRENT_WIFI = 4;
        int FORGET_WIFI_NETWORK = 5;
        int GET_BEST_CONFIGURED_NETWORK = 6;
        int GET_BEST_CONFIGURED_NETWORKS = 7;
        int GET_CONFIGURED_NETWORKS = 9;
        int GET_DHCP_INFO = 10;
        int GET_NEARBY_WIFI_NETWORKS = 11;
        int GET_WIFI_INFO = 12;
        int ITERATE_AND_CONNECT_TO_BEST_NETWORK = 13;
        int ITERATE_AND_REPAIR_WIFI_NETWORK = 14;
        int PERFORM_BANDWIDTH_TEST = 15;
        int PERFORM_CONNECTIVITY_TEST = 16;
        int RESET_CONNECTION_WITH_CURRENT_WIFI = 17;
        int TOGGLE_WIFI = 18;
        int TURN_WIFI_OFF = 19;
        int TURN_WIFI_ON = 20;
    }

    @ActionType
    protected int actionType;
    Executor executor;
    ScheduledExecutorService scheduledExecutorService;
    long actionTimeOutMs;
    Context context;
    NetworkActionLayer networkActionLayer;
    protected ActionResult actionResult;

    Action(@ActionType int actionType,
           Context context,
           Executor executor,
           ScheduledExecutorService scheduledExecutorService,
           NetworkActionLayer networkActionLayer,
           long actionTimeOutMs) {
        this.context = context;
        this.executor = executor;
        this.scheduledExecutorService = scheduledExecutorService;
        this.actionTimeOutMs = actionTimeOutMs;
        this.networkActionLayer = networkActionLayer;
        this.actionType = actionType;
    }

    @ActionType
    public int getActionType() {
        return actionType;
    }

    public abstract String getName();

    public abstract void cancelAction();

}
