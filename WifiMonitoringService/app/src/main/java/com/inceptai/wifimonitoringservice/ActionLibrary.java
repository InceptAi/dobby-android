package com.inceptai.wifimonitoringservice;

import android.content.Context;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.NetworkActionLayer;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.Action;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.CancelBandwidthTests;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.CheckIf5GHzIsSupported;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.ConnectAndTestGivenWifiNetwork;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.ConnectToBestConfiguredNetwork;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.ConnectWithGivenWifiNetwork;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.DisconnectFromCurrentWifi;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.ForgetWifiNetwork;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.FutureAction;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.GetBestConfiguredNetwork;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.GetBestConfiguredNetworks;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.GetConfiguredNetworks;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.GetDHCPInfo;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.GetOverallNetworkInfo;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.GetWifiInfo;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.IterateAndConnectToBestNetwork;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.IterateAndRepairWifiNetwork;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.ObservableAction;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.PerformBandwidthTest;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.PerformConnectivityTest;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.PerformPingTest;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.PerformPingTestForDHCPInfo;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.ResetConnectionWithCurrentWifi;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.ToggleWifi;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.TurnWifiOff;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.TurnWifiOn;
import com.inceptai.wifimonitoringservice.utils.ServiceLog;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

import io.reactivex.observers.DefaultObserver;
import io.reactivex.schedulers.Schedulers;

/**
 * Created by Vivek on 7/6/17.
 */

public class ActionLibrary {
    private NetworkActionLayer networkActionLayer;
    private Context context;
    private Executor executor;
    private ScheduledExecutorService scheduledExecutorService;
    private ArrayDeque<Action> actionArrayDeque;
    private ActionCallback actionCallback;

    public interface ActionCallback {
        void actionStarted(Action action);
        void actionCompleted(Action action, ActionResult actionResult);
    }

    public ActionLibrary(Context context, ExecutorService executorService,
                         ListeningScheduledExecutorService listeningScheduledExecutorService,
                         ScheduledExecutorService scheduledExecutorService) {
        this.context = context;
        this.executor = executorService;
        this.scheduledExecutorService = scheduledExecutorService;
        networkActionLayer = new NetworkActionLayer(context, executorService,
                listeningScheduledExecutorService, scheduledExecutorService);
        actionArrayDeque = new ArrayDeque<>();
    }

    public void cleanup() {
        networkActionLayer.cleanup();
        actionArrayDeque.clear();
        unregisterCallback();
    }

    public void registerCallback(ActionCallback actionCallback) {
        this.actionCallback = actionCallback;
    }

    public void unregisterCallback() {
        this.actionCallback = null;
    }

    public Action connectToBestWifi(Set<String> offlineRouterIds) {
        List<String> offlineRouterIdList = new ArrayList<>();
        if (offlineRouterIds != null) {
            offlineRouterIdList.addAll(offlineRouterIds);
        }
        return takeAction(ActionRequest.connectToBestConfiguredNetworkRequest(offlineRouterIdList, 0));
    }

    public Action takeAction(ActionRequest actionRequest) {
        if (actionRequest == null) {
            return null;
        }

        Action action = null;
        switch(actionRequest.getActionType()) {
            case Action.ActionType.CHECK_IF_5GHz_IS_SUPPORTED:
                action = new CheckIf5GHzIsSupported(context, executor, scheduledExecutorService, networkActionLayer, actionRequest.getActionTimeOutMs());
                break;
            case Action.ActionType.CONNECT_AND_TEST_GIVEN_WIFI_NETWORK:
                action = new ConnectAndTestGivenWifiNetwork(context, executor, scheduledExecutorService, networkActionLayer, actionRequest.getActionTimeOutMs(), actionRequest.getNetworkId());
                break;
            case Action.ActionType.CONNECT_TO_BEST_CONFIGURED_NETWORK:
                action = new ConnectToBestConfiguredNetwork(context, executor, scheduledExecutorService, networkActionLayer, actionRequest.getOfflineWifiNetworks(), actionRequest.getActionTimeOutMs());
                break;
            case Action.ActionType.CONNECT_WITH_GIVEN_WIFI_NETWORK:
                action = new ConnectWithGivenWifiNetwork(context, executor, scheduledExecutorService, networkActionLayer, actionRequest.getActionTimeOutMs(), actionRequest.getNetworkId());
                break;
            case Action.ActionType.DISCONNECT_FROM_CURRENT_WIFI:
                action = new DisconnectFromCurrentWifi(context, executor, scheduledExecutorService, networkActionLayer, actionRequest.getActionTimeOutMs());
                break;
            case Action.ActionType.FORGET_WIFI_NETWORK:
                action = new ForgetWifiNetwork(context, executor, scheduledExecutorService, networkActionLayer, actionRequest.getActionTimeOutMs(), actionRequest.getNetworkId());
                break;
            case Action.ActionType.GET_BEST_CONFIGURED_NETWORK:
                action = new GetBestConfiguredNetwork(context, executor, scheduledExecutorService, networkActionLayer, actionRequest.getActionTimeOutMs());
                break;
            case Action.ActionType.GET_BEST_CONFIGURED_NETWORKS:
                action = new GetBestConfiguredNetworks(context, executor, scheduledExecutorService, networkActionLayer, actionRequest.getActionTimeOutMs(), actionRequest.getNumberOfConfiguredNetworksToReturn());
                break;
            case Action.ActionType.GET_CONFIGURED_NETWORKS:
                action = new GetConfiguredNetworks(context, executor, scheduledExecutorService, networkActionLayer, actionRequest.getActionTimeOutMs());
                break;
            case Action.ActionType.GET_DHCP_INFO:
                action = new GetDHCPInfo(context, executor, scheduledExecutorService, networkActionLayer, actionRequest.getActionTimeOutMs());
                break;
            case Action.ActionType.GET_WIFI_INFO:
                action = new GetWifiInfo(context, executor, scheduledExecutorService, networkActionLayer, actionRequest.getActionTimeOutMs());
                break;
            case Action.ActionType.ITERATE_AND_CONNECT_TO_BEST_NETWORK:
                action = new IterateAndConnectToBestNetwork(context, executor, scheduledExecutorService, networkActionLayer, actionRequest.getActionTimeOutMs());
                break;
            case Action.ActionType.ITERATE_AND_REPAIR_WIFI_NETWORK:
                action = new IterateAndRepairWifiNetwork(context, executor, scheduledExecutorService, networkActionLayer, actionRequest.getActionTimeOutMs());
                break;
            case Action.ActionType.PERFORM_BANDWIDTH_TEST:
                action = new PerformBandwidthTest(context, executor, scheduledExecutorService, networkActionLayer, actionRequest.getActionTimeOutMs(), actionRequest.getBandwidthTestMode());
                break;
            case Action.ActionType.PERFORM_CONNECTIVITY_TEST:
                action = new PerformConnectivityTest(context, executor, scheduledExecutorService, networkActionLayer, actionRequest.getActionTimeOutMs());
                break;
            case Action.ActionType.RESET_CONNECTION_WITH_CURRENT_WIFI:
                action = new ResetConnectionWithCurrentWifi(context, executor, scheduledExecutorService, networkActionLayer, actionRequest.getActionTimeOutMs());
                break;
            case Action.ActionType.TOGGLE_WIFI:
                action = new ToggleWifi(context, executor, scheduledExecutorService, networkActionLayer, actionRequest.getActionTimeOutMs());
                break;
            case Action.ActionType.TURN_WIFI_ON:
                action = new TurnWifiOn(context, executor, scheduledExecutorService, networkActionLayer, actionRequest.getActionTimeOutMs());
                break;
            case Action.ActionType.TURN_WIFI_OFF:
                action = new TurnWifiOff(context, executor, scheduledExecutorService, networkActionLayer, actionRequest.getActionTimeOutMs());
                break;
            case Action.ActionType.CANCEL_BANDWIDTH_TESTS:
                action = new CancelBandwidthTests(context, executor, scheduledExecutorService, networkActionLayer, actionRequest.getActionTimeOutMs());
                break;
            case Action.ActionType.PERFORM_PING_TEST:
                action = new PerformPingTest(context, executor, scheduledExecutorService, networkActionLayer, actionRequest.getIpAddressListToPing(), actionRequest.getActionTimeOutMs());
                break;
            case Action.ActionType.PERFORM_PING_FOR_DHCP_INFO:
                action = new PerformPingTestForDHCPInfo(context, executor, scheduledExecutorService, networkActionLayer, actionRequest.getActionTimeOutMs());
                break;
            case Action.ActionType.GET_OVERALL_NETWORK_INFO:
                action = new GetOverallNetworkInfo(context, executor, scheduledExecutorService, networkActionLayer, actionRequest.getActionTimeOutMs());
                break;
            default:
                ServiceLog.e("UNKNOWN Action Type: " + actionRequest.getActionType() + " Not submitting action");
                break;
        }
        //Submit action and return the action
        if (action != null) {
            submitAction(action);
        }
        return action;
    }

    public ArrayList<Integer> getListOfPendingActions() {
        ArrayList<Integer> actionList = new ArrayList<>();
        for (Action futureAction: actionArrayDeque) {
            actionList.add(futureAction.getActionType());
        }
        return actionList;
    }

    void cancelPendingActions() {
        Action currentlyRunningAction = actionArrayDeque.peek();
        for (Action futureAction: actionArrayDeque) {
            if (futureAction != null && currentlyRunningAction != futureAction) {
                futureAction.cancelAction();
            }
        }
    }

    void cancelActionOfType(@Action.ActionType int actionType) {
        Action currentlyRunningAction = actionArrayDeque.peek();
        for (Action futureAction: actionArrayDeque) {
            if (futureAction != null && futureAction.getActionType() == actionType) {
                futureAction.cancelAction();
            }
        }
    }

    int numberOfPendingActions() {
        return actionArrayDeque.size();
    }

    //private stuff
    private void startFutureAction(final FutureAction futureAction) {
        if (futureAction != null) {
            ServiceLog.v("Starting future action " + futureAction.getName());
            futureAction.post();
            if (actionCallback != null) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        actionCallback.actionStarted(futureAction);
                    }
                });
            }
            processFutureActionResults(futureAction);
        }
    }

    private void startObservableAction(final ObservableAction observableAction) {
        if (observableAction != null) {
            ServiceLog.v("Starting observable action " + observableAction.getName());
            observableAction.start();
            if (actionCallback != null) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        actionCallback.actionStarted(observableAction);
                    }
                });
            }
            processObservableActionResults(observableAction);
        }
    }

    private void submitAction(Action action) {
        if (action instanceof FutureAction) {
            startFutureAction((FutureAction)addAction(action));
        } else if (action instanceof ObservableAction) {
            startObservableAction((ObservableAction)addAction(action));
        }
    }


    synchronized private Action addAction(Action action) {
        ServiceLog.v("AL: Queueing action " + action.getName());
        actionArrayDeque.addLast(action);
        if (!action.shouldBlockOnOtherActions() || actionArrayDeque.size() == 1) { //First element -- only one action at a time
            ServiceLog.v("Non blocking action or Queue size is 1 so returning action " + action.getName());
            return action;
        }
        ServiceLog.v("AL: Queue size is: " + actionArrayDeque.size() + " and blocking action so not starting right now " + action.getName());
        return null;
    }

    synchronized private void finishAction(final Action action, final ActionResult result) {
        ServiceLog.v("Removing action " + action.getName());
        boolean isActionHeadOfQueue = false;
        if (action.equals(actionArrayDeque.peek())) {
            //this action is top of the queue
            isActionHeadOfQueue = true;
        }
        actionArrayDeque.remove(action);
        if (actionCallback != null) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    actionCallback.actionCompleted(action, result);
                }
            });
        }

        if (isActionHeadOfQueue) {
            Action nextActionToProcess = actionArrayDeque.peek();
            if (nextActionToProcess != null && nextActionToProcess.shouldBlockOnOtherActions()) {
                ServiceLog.v("Starting pending action " + action.getName());
                if (action instanceof FutureAction) {
                    startFutureAction((FutureAction)nextActionToProcess);
                } else if (action instanceof ObservableAction) {
                    startObservableAction((ObservableAction)action);
                }
            }
        }
    }


    private void processFutureActionResults(final FutureAction futureAction) {
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
                    finishAction(futureAction, result);
                }
            }
        }, executor);
    }

    private void processObservableActionResults(final ObservableAction observableAction) {
        if (observableAction == null || observableAction.getObservable() == null) {
            ServiceLog.e("AL: Returning since observable action or getobservable is null");
            return;
        }
        observableAction.getObservable()
                .subscribeOn(Schedulers.from(executor))
                .subscribeWith(new DefaultObserver() {
            @Override
            public void onNext(Object o) {
                ServiceLog.v("AL: OnNext");
            }

            @Override
            public void onError(Throwable e) {
                //finish the action here
                ServiceLog.e("AL: OnError");
                finishAction(observableAction, null);
            }

            @Override
            public void onComplete() {
                //finish it here too
                ServiceLog.v("AL: OnComplete");
                ActionResult actionResult = observableAction.getFinalResult();
                finishAction(observableAction, actionResult);
            }
        });
    }

}
