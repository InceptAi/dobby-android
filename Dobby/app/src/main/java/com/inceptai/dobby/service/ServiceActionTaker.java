package com.inceptai.dobby.service;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.support.annotation.Nullable;

import com.inceptai.dobby.utils.DobbyLog;
import com.inceptai.wifimonitoringservice.ActionLibrary;
import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ConnectivityTester;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.FutureAction;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;


/**
 * Created by vivek on 7/10/17.
 */

public class ServiceActionTaker {
    private static long ACTION_TIMEOUT_MS = 1000000; //10sec timeout
    private ActionLibrary actionLibrary;
    private ActionCallback actionCallback;
    private WifiInfo wifiInfoAfterRepair;
    private Executor executor;
    private ScheduledExecutorService scheduledExecutorService;

    public interface ActionCallback {
        void actionStarted(String actionName);
        void actionCompleted(String actionName, ActionResult actionResult);
    }

    public ServiceActionTaker(Context applicationContext, Executor executor, ScheduledExecutorService scheduledExecutorService) {
        this.executor = executor;
        this.scheduledExecutorService = scheduledExecutorService;
        actionLibrary = new ActionLibrary(applicationContext, executor, scheduledExecutorService);
    }

    public void registerCallback(ActionCallback actionCallback) {
        this.actionCallback = actionCallback;
    }

    public void unregisterCallback() {
        this.actionCallback = null;
    }

    public void turnWifiOff() {
        final FutureAction wifiOff = actionLibrary.turnWifiOff(ACTION_TIMEOUT_MS);
        processResultsWhenAvailable(wifiOff);
    }

    public void turnWifiOn() {
        final FutureAction wifiOn = actionLibrary.turnWifiOn(ACTION_TIMEOUT_MS);
        processResultsWhenAvailable(wifiOn);
    }

    public void toggleWifi() {
        final FutureAction toggleWifi = actionLibrary.toggleWifi(ACTION_TIMEOUT_MS);
        processResultsWhenAvailable(toggleWifi);
    }

    public void checkIf5GHzSupported() {
        final FutureAction check5GHz = actionLibrary.checkIf5GHzSupported(ACTION_TIMEOUT_MS);
        processResultsWhenAvailable(check5GHz);
    }

    public void disconnect() {
        FutureAction disconnectAction = actionLibrary.disconnect(ACTION_TIMEOUT_MS);
        processResultsWhenAvailable(disconnectAction);
    }

    public void connectWithWifiNetwork(int networkId) {
        FutureAction connectAction = actionLibrary.connectWithWifiNetwork(networkId, ACTION_TIMEOUT_MS);
        processResultsWhenAvailable(connectAction);
    }

    public void forgetWifiNetwork(int networkId) {
        FutureAction forgetWifiNetworkAction = actionLibrary.forgetWifiNetwork(networkId, ACTION_TIMEOUT_MS);
        processResultsWhenAvailable(forgetWifiNetworkAction);
    }

    public void getBestConfiguredNetwork() {
        FutureAction getBestNetwork = actionLibrary.getBestConfiguredNetwork(ACTION_TIMEOUT_MS);
        processResultsWhenAvailable(getBestNetwork);
    }

    public void getConfiguredNetworks() {
        FutureAction getConfiguredNetworksAction = actionLibrary.getConfiguredNetworks(ACTION_TIMEOUT_MS);
        processResultsWhenAvailable(getConfiguredNetworksAction);
    }

    public void getDhcpInfo() {
        FutureAction getDhcpInfoAction = actionLibrary.getDhcpInfo(ACTION_TIMEOUT_MS);
        processResultsWhenAvailable(getDhcpInfoAction);
    }

    public void getWifiInfo() {
        FutureAction getWifiInfoAction = actionLibrary.getWifiInfo(ACTION_TIMEOUT_MS);
        processResultsWhenAvailable(getWifiInfoAction);
    }

    public void getNearbyWifiNetworks() {
        FutureAction nearbyWifiNetworksAction = actionLibrary.getNearbyWifiNetworks(ACTION_TIMEOUT_MS);
        processResultsWhenAvailable(nearbyWifiNetworksAction);
    }

    public void connectToBestWifi() {
        FutureAction connectToBestWifiAction = actionLibrary.connectToBestWifi(ACTION_TIMEOUT_MS);
        processResultsWhenAvailable(connectToBestWifiAction);
    }

    public void resetConnection() {
        FutureAction resetConnectionAction = actionLibrary.resetConnection(ACTION_TIMEOUT_MS);
        processResultsWhenAvailable(resetConnectionAction);
    }

    public void performConnectivityTest() {
        FutureAction connectivityTestAction = actionLibrary.performConnectivityTest(ACTION_TIMEOUT_MS);
        processResultsWhenAvailable(connectivityTestAction);
    }

    public void repairConnection() {
        final FutureAction repairWifiNetworkAction = actionLibrary.repairWifiNetwork(ACTION_TIMEOUT_MS);
        sendCallbackForActionStarted(repairWifiNetworkAction);
        wifiInfoAfterRepair = null;
        repairWifiNetworkAction.getFuture().addListener(new Runnable() {
            @Override
            public void run() {
                ActionResult repairResult = null;
                ActionResult wifiInfoResult = null;
                ActionResult resultToReturn = null;
                int modeAfterRepair = ConnectivityTester.WifiConnectivityMode.UNKNOWN;
                try {
                    repairResult = repairWifiNetworkAction.getFuture().get();
                    if (repairResult != null && repairResult.getStatus() == ActionResult.ActionResultCodes.SUCCESS) {
                        modeAfterRepair = (Integer) repairResult.getPayload();
                        final FutureAction getWifiInfo = actionLibrary.getWifiInfo(ACTION_TIMEOUT_MS);
                        wifiInfoResult = getWifiInfo.getFuture().get();
                        if (modeAfterRepair == ConnectivityTester.WifiConnectivityMode.CONNECTED_AND_ONLINE) {
                            //Repair successful -- get wifi info and show it to the user
                            resultToReturn = new ActionResult(ActionResult.ActionResultCodes.SUCCESS,
                                    "Repaired !", wifiInfoResult.getPayload());
                        } else {
                            resultToReturn = new ActionResult(ActionResult.ActionResultCodes.GENERAL_ERROR,
                                    "Toggled and connected, but unable to put wifi in connected mode: mode found " +
                                            ConnectivityTester.connectivityModeToString(modeAfterRepair), wifiInfoResult.getPayload());
                        }
                    } else {
                        resultToReturn = new ActionResult(repairResult.getStatus(),
                                "Unable to toggle and connect: error " + repairResult.getStatusString(), null);
                    }
                    DobbyLog.v("ActionTaker: Got the result for  " + repairWifiNetworkAction.getName() + " result " + repairResult.getStatusString());
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                    DobbyLog.w("ActionTaker: Exception getting wifi results: " + e.toString());
                } finally {
                    sendCallbackForActionCompleted(repairWifiNetworkAction, resultToReturn);
                }
            }
        }, executor);
    }

    private void sendCallbackForActionStarted(FutureAction action) {
        if (actionCallback != null) {
            actionCallback.actionStarted(action.getName());
        }
    }

    private void sendCallbackForActionCompleted(FutureAction action, @Nullable ActionResult actionResult) {
        if (actionCallback != null) {
            actionCallback.actionCompleted(action.getName(), actionResult);
        }
    }

    private void processResultsWhenAvailable(final FutureAction futureAction) {
        sendCallbackForActionStarted(futureAction);
        futureAction.getFuture().addListener(new Runnable() {
            @Override
            public void run() {
                ActionResult result = null;
                try {
                    result = futureAction.getFuture().get();
                    DobbyLog.v("ActionTaker: Got the result for  " + futureAction.getName() + " result " + result.getStatusString());
                }catch (Exception e) {
                    e.printStackTrace(System.out);
                    DobbyLog.w("ActionTaker: Exception getting wifi results: " + e.getStackTrace().toString());
                    //Informing inference engine of the error.
                } finally {
                    sendCallbackForActionCompleted(futureAction, result);
                }
            }
        }, executor);
    }
}
