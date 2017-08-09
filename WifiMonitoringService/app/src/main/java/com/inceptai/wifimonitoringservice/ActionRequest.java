package com.inceptai.wifimonitoringservice;

import android.support.annotation.Nullable;

import com.inceptai.wifimonitoringservice.actionlibrary.actions.Action;
import com.inceptai.wifimonitoringservice.actionlibrary.utils.ActionLibraryCodes;

import java.util.ArrayList;
import java.util.List;

import static com.inceptai.wifimonitoringservice.actionlibrary.actions.Action.ACTION_TIMEOUT_MS;
import static com.inceptai.wifimonitoringservice.actionlibrary.actions.GetBestConfiguredNetworks.DEFAULT_NUMBER_OF_NETWORKS_TO_RETURN;

/**
 * Created by vivek on 8/5/17.
 */


public class ActionRequest {
    @Action.ActionType
    private int actionType;
    private int networkId = 0;
    private long actionTimeOutMs = ACTION_TIMEOUT_MS;
    private int numberOfConfiguredNetworksToReturn = 0;
    private List<String> offlineWifiNetworks;
    @ActionLibraryCodes.BandwidthTestMode private int bandwidthTestMode = ActionLibraryCodes.BandwidthTestMode.DOWNLOAD_AND_UPLOAD;

    //Additional parameters
    private ActionRequest(@Action.ActionType int actionType, int networkId,
                          int numberOfConfiguredNetworksToReturn, long actionTimeOutMs) {
        if (actionTimeOutMs == 0) {
            actionTimeOutMs = ACTION_TIMEOUT_MS;
        }
        this.actionType = actionType;
        this.networkId = networkId;
        this.actionTimeOutMs = actionTimeOutMs;
        this.numberOfConfiguredNetworksToReturn = numberOfConfiguredNetworksToReturn;
        this.offlineWifiNetworks = new ArrayList<>();
    }

    private ActionRequest(@Action.ActionType int actionType, int networkId,
                          int numberOfConfiguredNetworksToReturn, long actionTimeOutMs,
                          @ActionLibraryCodes.BandwidthTestMode int bandwidthTestMode) {
        this(actionType, networkId, numberOfConfiguredNetworksToReturn, actionTimeOutMs);
        this.bandwidthTestMode = bandwidthTestMode;
    }

    private ActionRequest(@Action.ActionType int actionType, List<String> offlineNetworks,
                          long actionTimeOutMs) {
        this(actionType, 0, 0, actionTimeOutMs);
        this.offlineWifiNetworks = offlineNetworks;
    }

    /**
     * Factory constructor to create an instance
     */
    @Nullable
    public static ActionRequest check5GHzRequest(long actionTimeOutMs) {
        return new ActionRequest(Action.ActionType.CHECK_IF_5GHz_IS_SUPPORTED, 0, 0, actionTimeOutMs);
    }

    @Nullable
    public static ActionRequest connectAndTestGivenWifiNetworkRequest(int networkId, long actionTimeOutMs) {
        return new ActionRequest(Action.ActionType.CONNECT_AND_TEST_GIVEN_WIFI_NETWORK, networkId, 0, actionTimeOutMs);
    }

    @Nullable
    public static ActionRequest connectToBestConfiguredNetworkRequest(long actionTimeOutMs) {
        return new ActionRequest(Action.ActionType.CONNECT_TO_BEST_CONFIGURED_NETWORK, 0, 0, actionTimeOutMs);
    }

    @Nullable
    public static ActionRequest connectToBestConfiguredNetworkRequest(List<String> listOfOfflineRouters, long actionTimeOutMs) {
        return new ActionRequest(Action.ActionType.CONNECT_TO_BEST_CONFIGURED_NETWORK, listOfOfflineRouters, actionTimeOutMs);
    }

    @Nullable
    public static ActionRequest connectToBestConfiguredNetworkRequest(List<String> listOfOfflineRouters) {
        return new ActionRequest(Action.ActionType.CONNECT_TO_BEST_CONFIGURED_NETWORK, listOfOfflineRouters, 0);
    }


    @Nullable
    public static ActionRequest connectWithGivenWifiNetworkRequest(long actionTimeOutMs) {
        return new ActionRequest(Action.ActionType.CONNECT_WITH_GIVEN_WIFI_NETWORK, 0, 0, actionTimeOutMs);
    }

    @Nullable
    public static ActionRequest disconnectWithCurrentWifiNetworkRequest(long actionTimeOutMs) {
        return new ActionRequest(Action.ActionType.DISCONNECT_FROM_CURRENT_WIFI, 0, 0, actionTimeOutMs);
    }

    @Nullable
    public static ActionRequest forgetWifiNetworkRequest(int networkId, long actionTimeOutMs) {
        return new ActionRequest(Action.ActionType.FORGET_WIFI_NETWORK, networkId, 0, actionTimeOutMs);
    }

    @Nullable
    public static ActionRequest getBestConfiguredNetworkRequest(long actionTimeOutMs) {
        return new ActionRequest(Action.ActionType.GET_BEST_CONFIGURED_NETWORK, 0, 0, actionTimeOutMs);
    }

    @Nullable
    public static ActionRequest getBestConfiguredNetworksRequest(int numberOfConfiguredNetworksToReturn, long actionTimeOutMs) {
        return new ActionRequest(Action.ActionType.GET_BEST_CONFIGURED_NETWORKS, 0, numberOfConfiguredNetworksToReturn, actionTimeOutMs);
    }

    @Nullable
    public static ActionRequest getBestConfiguredNetworksRequest(long actionTimeOutMs) {
        return new ActionRequest(Action.ActionType.GET_BEST_CONFIGURED_NETWORKS, 0, DEFAULT_NUMBER_OF_NETWORKS_TO_RETURN, actionTimeOutMs);
    }

    @Nullable
    public static ActionRequest getDhcpInfoRequest(long actionTimeOutMs) {
        return new ActionRequest(Action.ActionType.GET_DHCP_INFO, 0, 0, actionTimeOutMs);
    }

    @Nullable
    public static ActionRequest getNearbyWifiNetworksRequest(long actionTimeOutMs) {
        return new ActionRequest(Action.ActionType.GET_NEARBY_WIFI_NETWORKS, 0, 0, actionTimeOutMs);
    }

    @Nullable
    public static ActionRequest getWifiInfoRequest(long actionTimeOutMs) {
        return new ActionRequest(Action.ActionType.GET_WIFI_INFO, 0, 0, actionTimeOutMs);
    }

    @Nullable
    public static ActionRequest iterateAndConnectToBestNetworkRequest(long actionTimeOutMs) {
        return new ActionRequest(Action.ActionType.ITERATE_AND_CONNECT_TO_BEST_NETWORK, 0, 0, actionTimeOutMs);
    }


    @Nullable
    public static ActionRequest iterateAndRepairWifiNetworkRequest(long actionTimeOutMs) {
        return new ActionRequest(Action.ActionType.ITERATE_AND_REPAIR_WIFI_NETWORK, 0, 0, actionTimeOutMs);
    }

    @Nullable
    public static ActionRequest performConnectivityTestRequest(long actionTimeOutMs) {
        return new ActionRequest(Action.ActionType.PERFORM_CONNECTIVITY_TEST, 0, 0, actionTimeOutMs);
    }

    @Nullable
    public static ActionRequest resetConnectionWithCurrentWifiRequest(long actionTimeOutMs) {
        return new ActionRequest(Action.ActionType.RESET_CONNECTION_WITH_CURRENT_WIFI, 0, 0, actionTimeOutMs);
    }

    @Nullable
    public static ActionRequest toggleWifiRequest(long actionTimeOutMs) {
        return new ActionRequest(Action.ActionType.TOGGLE_WIFI, 0, 0, actionTimeOutMs);
    }

    @Nullable
    public static ActionRequest turnWifiOffRequest(long actionTimeOutMs) {
        return new ActionRequest(Action.ActionType.TURN_WIFI_OFF, 0, 0, actionTimeOutMs);
    }

    @Nullable
    public static ActionRequest turnWifiOnRequest(long actionTimeOutMs) {
        return new ActionRequest(Action.ActionType.TURN_WIFI_ON, 0, 0, actionTimeOutMs);
    }

    //Observable action request
    @Nullable
    public static ActionRequest performBandwidthTestRequest(@ActionLibraryCodes.BandwidthTestMode int mode,  long actionTimeOutMs) {
        return new ActionRequest(Action.ActionType.PERFORM_BANDWIDTH_TEST, 0, 0, actionTimeOutMs, mode);
    }


    @Action.ActionType
    public int getActionType() {
        return actionType;
    }

    public int getNetworkId() {
        return networkId;
    }

    public long getActionTimeOutMs() {
        return actionTimeOutMs;
    }

    public int getNumberOfConfiguredNetworksToReturn() {
        return numberOfConfiguredNetworksToReturn;
    }

    @ActionLibraryCodes.BandwidthTestMode
    public int getBandwidthTestMode() {
        return bandwidthTestMode;
    }
}