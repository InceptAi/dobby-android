package com.inceptai.wifiexpertsystem.actions.api;

import android.content.Context;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.inceptai.wifiexpertsystem.utils.DobbyLog;
import com.inceptai.wifimonitoringservice.ActionLibrary;
import com.inceptai.wifimonitoringservice.ActionRequest;
import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.Action;
import com.inceptai.wifimonitoringservice.actionlibrary.utils.ActionLibraryCodes;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import static com.inceptai.wifiexpertsystem.actions.ActionStrings.*;

/**
 * Created by vivek on 11/15/17.
 */

public class APIActionLibraryClient implements ActionLibrary.ActionCallback {
    private ActionLibrary actionLibrary;
    private ExecutorService executorService;
    private Context context;
    private ActionLibraryClientCallback actionLibraryClientCallback;

    public interface ActionLibraryClientCallback {
        void actionStarted(Action action);
        void actionFinished(Action action, ActionResult actionResult);
    }

    public APIActionLibraryClient(Context context, ExecutorService executorService,
                                  ListeningScheduledExecutorService listeningScheduledExecutorService,
                                  ScheduledExecutorService scheduledExecutorService,
                                  ActionLibraryClientCallback actionLibraryClientCallback) {
        this.context = context.getApplicationContext();
        this.executorService = executorService;
        actionLibrary = new ActionLibrary(context, executorService, listeningScheduledExecutorService, scheduledExecutorService);
        actionLibrary.registerCallback(this);
        this.actionLibraryClientCallback = actionLibraryClientCallback;
    }

    //Expert system service callbacks
    public boolean takeAPIAction(String actionMessage) {
        ActionRequest actionRequest = parseMessageAndTakeActions(actionMessage);
        if (actionRequest != null) {
            takeAPIAction(actionRequest);
            return true;
        }
        return false;
    }

    private void takeAPIAction(final ActionRequest actionRequest) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                actionLibrary.takeAction(actionRequest);
            }
        });
    }

    //Action Library callbacks
    @Override
    public void actionStarted(final Action action) {
        DobbyLog.v("Action started : " + action.getName());
        if (actionLibraryClientCallback != null) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    actionLibraryClientCallback.actionStarted(action);
                }
            });
        }
    }

    @Override
    public void actionCompleted(final Action action, final ActionResult actionResult) {
        DobbyLog.v("Action completed : " + action.getName());
        if (actionLibraryClientCallback != null) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    actionLibraryClientCallback.actionFinished(action, actionResult);
                }
            });
        }
    }

    private ActionRequest parseMessageAndTakeActions(String message) {
        ActionRequest actionRequest = null;
        switch (message.trim().toLowerCase()) {
            case TURN_WIFI_OFF:
                actionRequest = ActionRequest.turnWifiOffRequest(0);
                break;
            case TURN_WIFI_ON:
                actionRequest = ActionRequest.turnWifiOnRequest(0);
                break;
            case SCAN_WIFI_NETWORK:
                actionRequest = ActionRequest.getNearbyWifiNetworksRequest(0);
                break;
            case TOGGLE_WIFI:
                actionRequest = ActionRequest.toggleWifiRequest(0);
                break;
            case RESET_WIFI_CONNECTION:
                actionRequest = ActionRequest.resetConnectionWithCurrentWifiRequest(0);
                break;
            case DISCONNECT_CURRENT_WIFI:
                actionRequest = ActionRequest.disconnectWithCurrentWifiNetworkRequest(0);
                break;
            case CHECK_IF_5GHZ_SUPPORTED:
                actionRequest = ActionRequest.check5GHzRequest(0);
                break;
            case ITERATE_AND_REPAIR_WIFI_NETWORK:
                actionRequest = ActionRequest.iterateAndRepairWifiNetworkRequest(0);
                break;
            case GET_DHCP_INFO:
                actionRequest = ActionRequest.getDhcpInfoRequest(0);
                break;
            case GET_WIFI_INFO:
                actionRequest = ActionRequest.getWifiInfoRequest(0);
                break;
            case PERFORM_PING_TEST:
                actionRequest = ActionRequest.performPingForDhcpInfoRequest(0);
                break;
            case PERFORM_BW_TEST:
                actionRequest = ActionRequest.performBandwidthTestRequest(ActionLibraryCodes.BandwidthTestMode.DOWNLOAD_AND_UPLOAD, 40000);
                break;
            case CANCEL_BW_TEST:
                actionRequest = ActionRequest.cancelBandwidthTestsRequest(1000);
                break;
            case PERFORM_CONNECTIVITY_TEST:
                actionRequest = ActionRequest.performConnectivityTestRequest(0);
                break;
            case RESET_NETWORK_SETTINGS:
            case TURN_BLUETOOTH_ON:
            case TURN_BLUETOOTH_OFF:
            case ALWAYS_KEEP_WIFI_ON_DURING_SLEEP:
            case NEVER_KEEP_WIFI_ON_DURING_SLEEP:
            case KEEP_WIFI_ON_DURING_SLEEP_WHEN_PLUGGED_IN:
            case SWITCH_TO_2GHZ_BAND:
            case SWITCH_TO_5GHZ_BAND:
            case SWITCH_TO_AUTOMATIC_BAND_SELECTION:
            default:
                break;
        }
        return actionRequest;
    }
}
