package com.inceptai.wifimonitoringservice;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ConnectivityTester;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.FutureAction;
import com.inceptai.wifimonitoringservice.utils.ServiceLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import io.reactivex.Observable;

import static com.inceptai.wifimonitoringservice.actionlibrary.ActionResult.ActionResultCodes.FAILED_TO_COMPLETE;
import static com.inceptai.wifimonitoringservice.actionlibrary.ActionResult.ActionResultCodes.GENERAL_ERROR;
import static com.inceptai.wifimonitoringservice.actionlibrary.ActionResult.ActionResultCodes.SUCCESS;


/**
 * Created by vivek on 7/10/17.
 */

public class ServiceActionTaker {
    private static long ACTION_TIMEOUT_MS = 30000; //10sec timeout
    private ActionLibrary actionLibrary;
    private ActionCallback actionCallback;
    private Executor executor;
    private ScheduledExecutorService scheduledExecutorService;
    private ListenableFuture<ActionResult> iterateAndRepairFuture;
    private FutureAction iterateAndRepairFutureAction;

    public interface ActionCallback {
        void actionStarted(String actionName);
        void actionCompleted(String actionName, ActionResult actionResult);
    }

    public ServiceActionTaker(Context applicationContext, ServiceThreadPool serviceThreadPool) {
        this.executor = serviceThreadPool.getExecutor();
        this.scheduledExecutorService = serviceThreadPool.getScheduledExecutorService();
        actionLibrary = new ActionLibrary(applicationContext, serviceThreadPool);
    }

    public void cleanup() {
        unregisterCallback();
        actionLibrary.cleanup();
    }


    public void registerCallback(ActionCallback actionCallback) {
        this.actionCallback = actionCallback;
    }


    public void unregisterCallback() {
        this.actionCallback = null;
    }


    public FutureAction takeAction(ExpertAction.ActionRequest actionRequest) {
        switch(actionRequest.getActionType()) {
            case ExpertAction.ActionType.CHECK_IF_5GHz_IS_SUPPORTED:
                return checkIf5GHzSupported();
                break;

        }
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

    public FutureAction checkIf5GHzSupported() {
        final FutureAction check5GHz = actionLibrary.checkIf5GHzSupported(ACTION_TIMEOUT_MS);
        processResultsWhenAvailable(check5GHz);
        return check5GHz;
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

    public void connectToBestWifi(Set<String> offlineRouterIds) {
        List<String> offlineRouterIdList = new ArrayList<>();
        if (offlineRouterIds != null) {
            offlineRouterIdList.addAll(offlineRouterIds);
        }
        FutureAction connectToBestWifiAction = actionLibrary.connectToBestWifi(ACTION_TIMEOUT_MS, offlineRouterIdList);
        processResultsWhenAvailable(connectToBestWifiAction);
    }

    public void resetConnection() {
        FutureAction resetConnectionAction = actionLibrary.resetConnection(ACTION_TIMEOUT_MS);
        processResultsWhenAvailable(resetConnectionAction);
    }

    public FutureAction performConnectivityTest() {
        return actionLibrary.performConnectivityTest(ACTION_TIMEOUT_MS);
    }

    public ListenableFuture<ActionResult> repairConnectionOneShot() {
        final SettableFuture<ActionResult> repairResultFuture = SettableFuture.create();
        final FutureAction repairWifiNetworkAction = actionLibrary.repairWifiNetwork(ACTION_TIMEOUT_MS);
        sendCallbackForActionStarted(repairWifiNetworkAction);
        repairWifiNetworkAction.getFuture().addListener(new Runnable() {
            @Override
            public void run() {
                ActionResult repairResult = null;
                ActionResult wifiInfoResult = null;
                ActionResult resultToReturn = null;
                int modeAfterRepair = ConnectivityTester.WifiConnectivityMode.UNKNOWN;
                try {
                    repairResult = repairWifiNetworkAction.getFuture().get();
                    if (repairResult != null && repairResult.getStatus() == SUCCESS) {
                        modeAfterRepair = (Integer) repairResult.getPayload();
                        final FutureAction getWifiInfo = actionLibrary.getWifiInfo(ACTION_TIMEOUT_MS);
                        wifiInfoResult = getWifiInfo.getFuture().get();
                        if (ConnectivityTester.isConnected(modeAfterRepair)) {
                            //Connected
                            if (ConnectivityTester.isOnline(modeAfterRepair)) {
                                resultToReturn = new ActionResult(SUCCESS,
                                        "Repaired !", wifiInfoResult.getPayload());
                            } else {
                                resultToReturn = new ActionResult(FAILED_TO_COMPLETE,
                                        "Toggled and connected, but unable to put wifi in online mode: mode found " +
                                                ConnectivityTester.connectivityModeToString(modeAfterRepair), wifiInfoResult.getPayload());
                            }
                        } else {
                            resultToReturn = new ActionResult(GENERAL_ERROR,
                                    "Unable to toggle and connect: error ", null);
                        }
                    } else {
                        if (repairResult != null) {
                            resultToReturn = new ActionResult(repairResult.getStatus(),
                                    "Unable to toggle and connect: error " + repairResult.getStatusString(), null);
                        } else {
                            resultToReturn = new ActionResult(GENERAL_ERROR,
                                    "Unable to toggle and connect: error ", null);
                        }
                    }
                    ServiceLog.v("ActionTaker: Got the result for  " + repairWifiNetworkAction.getName() + " result " + repairResult.getStatusString());
                } catch (InterruptedException | CancellationException | ExecutionException e) {
                    e.printStackTrace(System.out);
                    ServiceLog.w("ActionTaker: Exception getting wifi results: " + e.toString());
                } finally {
                    repairResultFuture.set(resultToReturn);
                    sendCallbackForActionCompleted(repairWifiNetworkAction, resultToReturn);
                }
            }
        }, executor);
        return repairResultFuture;
    }

    public ListenableFuture<ActionResult> iterateAndRepairConnection(long timeOutMs) {

        if (iterateAndRepairFuture != null && !iterateAndRepairFuture.isDone()) {
            return iterateAndRepairFuture;
        }
        if (timeOutMs > 0) {
            iterateAndRepairFutureAction = actionLibrary.iterateAndRepairWifiNetwork(timeOutMs);
        } else {
            iterateAndRepairFutureAction = actionLibrary.iterateAndRepairWifiNetwork(ACTION_TIMEOUT_MS);
        }
        sendCallbackForActionStarted(iterateAndRepairFutureAction);
        iterateAndRepairFuture = iterateAndRepairFutureAction.getSettableFuture();
        return iterateAndRepairFuture;
    }

    public void cancelIterateAndRepair() {
        ServiceLog.v("In cancelIterateAndRepair");
        if (iterateAndRepairFuture != null && !iterateAndRepairFuture.isDone()) {
            iterateAndRepairFuture.cancel(true);
            ServiceLog.v("In cancelIterateAndRepair: Cancelling iterate and repair");
            if (iterateAndRepairFutureAction != null) {
                iterateAndRepairFutureAction.cancelAction();
            }
        }
    }


    public void cancelPendingActions() {
        actionLibrary.cancelPendingActions();
    }

    public void numberOfPendingActions() {
        actionLibrary.numberOfPendingActions();
    }

    //Private stuff
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
                    ServiceLog.v("ActionTaker: Got the result for  " + futureAction.getName() + " result " + result.getStatusString());
                }catch (Exception e) {
                    e.printStackTrace(System.out);
                    ServiceLog.w("ActionTaker: Exception getting wifi results: " + e.toString());
                    //Informing inference engine of the error.
                } finally {
                    sendCallbackForActionCompleted(futureAction, result);
                }
            }
        }, executor);
    }
}
