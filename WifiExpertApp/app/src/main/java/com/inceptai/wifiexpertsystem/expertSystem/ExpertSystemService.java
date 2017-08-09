package com.inceptai.wifiexpertsystem.expertSystem;

import android.content.Context;
import android.support.annotation.NonNull;

import com.inceptai.wifiexpertsystem.expertSystem.messages.ExpertMessage;
import com.inceptai.wifiexpertsystem.expertSystem.messages.UserMessage;
import com.inceptai.wifimonitoringservice.ActionRequest;
import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.Action;
import com.inceptai.wifimonitoringservice.actionlibrary.utils.ActionLibraryCodes;

import java.util.List;

/**
 * Created by vivek on 8/5/17.
 */

public class ExpertSystemService  {
    private ExpertSystemCallback callback;

    public interface ExpertSystemCallback {
        void onActionRequested(ActionRequest actionRequest);
        void onExpertMessage(ExpertMessage expertMessage);
    }

    ExpertSystemService(String userId, Context context, @NonNull ExpertSystemCallback callback) {
        this.callback = callback;
    }

    //Public methods
    void onUserMessage(UserMessage userMessage) {
        //process user message
        ActionRequest actionRequest = parseMessageAndTakeActions(userMessage.getMessage());
        callback.onActionRequested(actionRequest);
    }

    public void actionStarted(Action action) {
        ExpertMessage expertMessage = ExpertMessage.create(action);
        callback.onExpertMessage(expertMessage);
    }

    public void actionFinished(Action action, ActionResult actionResult) {
        ExpertMessage expertMessage = ExpertMessage.create(action, actionResult);
        callback.onExpertMessage(expertMessage);
    }


    public void configure(List<Action> expertActionList) {

    }

    public void registerAction(Action expertAction) {

    }

    public void connect() {
        //Expert channel connection
    }

    public void disconnect() {
        //Expert channel disconnection
    }

    //TODO -- move this to cloud
    private ActionRequest parseMessageAndTakeActions(String message) {
        ActionRequest actionRequest = null;
        if (message.toLowerCase().contains("wifiscan") || message.toLowerCase().contains("wifinearby")) {
            actionRequest = ActionRequest.getNearbyWifiNetworksRequest(0);
        } else if (message.toLowerCase().contains("wifioff")) {
            actionRequest = ActionRequest.turnWifiOffRequest(0);
        } else if (message.toLowerCase().contains("wifion")) {
            actionRequest = ActionRequest.turnWifiOnRequest(0);
        } else if (message.toLowerCase().contains("wifitoggle")) {
            actionRequest = ActionRequest.toggleWifiRequest(0);
        } else if (message.toLowerCase().contains("wifidisconnect")) {
            actionRequest = ActionRequest.disconnectWithCurrentWifiNetworkRequest(0);
        } else if (message.toLowerCase().contains("wifireset")) {
            actionRequest = ActionRequest.resetConnectionWithCurrentWifiRequest(0);
        } else if (message.toLowerCase().contains("wificonfigured") &&
                !message.toLowerCase().contains("best")) {
            actionRequest = ActionRequest.getBestConfiguredNetworksRequest(0);
        } else if (message.toLowerCase().contains("wificonfigured") &&
                message.toLowerCase().contains("best")) {
            actionRequest = ActionRequest.getBestConfiguredNetworkRequest(0);
        } else if (message.toLowerCase().contains("check5ghz")) {
            actionRequest = ActionRequest.check5GHzRequest(0);
        } else if (message.toLowerCase().contains("wificonnect")) {
            actionRequest = ActionRequest.resetConnectionWithCurrentWifiRequest(0);
        } else if (message.toLowerCase().contains("wifidhcp")) {
            actionRequest = ActionRequest.getDhcpInfoRequest(0);
        } else if (message.toLowerCase().contains("wifiinfo")) {
            actionRequest = ActionRequest.getWifiInfoRequest(0);
        } else if (message.toLowerCase().contains("wifirepair")) {
            actionRequest = ActionRequest.iterateAndRepairWifiNetworkRequest(0);
        } else if (message.toLowerCase().contains("connectiontest")) {
            actionRequest = ActionRequest.performConnectivityTestRequest(0);
        }  else if (message.toLowerCase().contains("bwtest")) {
            actionRequest = ActionRequest.performBandwidthTestRequest(ActionLibraryCodes.BandwidthTestMode.DOWNLOAD_AND_UPLOAD, 40000);
        }
        return actionRequest;
    }

}
