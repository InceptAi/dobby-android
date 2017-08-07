package com.inceptai.wifimonitoringservice;

import android.content.Context;

import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.NetworkActionLayer;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.Action;
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
import com.inceptai.wifimonitoringservice.actionlibrary.actions.GetWifiInfo;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.IterateAndConnectToBestNetwork;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.IterateAndRepairWifiNetwork;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.ObservableAction;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.PerformBandwidthTest;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.PerformConnectivityTest;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.ResetConnectionWithCurrentWifi;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.ToggleWifi;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.TurnWifiOff;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.TurnWifiOn;
import com.inceptai.wifimonitoringservice.utils.ServiceLog;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import io.reactivex.observers.DisposableObserver;
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

    public ActionLibrary(Context context, ServiceThreadPool serviceThreadPool) {
        this.context = context;
        this.executor = serviceThreadPool.getExecutor();
        this.scheduledExecutorService = serviceThreadPool.getScheduledExecutorService();
        networkActionLayer = new NetworkActionLayer(context, serviceThreadPool);
        actionArrayDeque = new ArrayDeque<>();
    }

    public void cleanup() {
        networkActionLayer.cleanup();
        actionArrayDeque.clear();
    }


    public Action takeAction(ActionRequest actionRequest) {
        Action action = null;
        switch(actionRequest.getActionType()) {
            case Action.ActionType.CHECK_IF_5GHz_IS_SUPPORTED:
                action = new CheckIf5GHzIsSupported(context, executor, scheduledExecutorService, networkActionLayer, actionRequest.getActionTimeOutMs());
                break;
            case Action.ActionType.CONNECT_AND_TEST_GIVEN_WIFI_NETWORK:
                action = new ConnectAndTestGivenWifiNetwork(context, executor, scheduledExecutorService, networkActionLayer, actionRequest.getActionTimeOutMs(), actionRequest.getNetworkId());
                break;
            case Action.ActionType.CONNECT_TO_BEST_CONFIGURED_NETWORK:
                action = new ConnectToBestConfiguredNetwork(context, executor, scheduledExecutorService, networkActionLayer, actionRequest.getActionTimeOutMs());
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

    int numberOfPendingActions() {
        return actionArrayDeque.size();
    }

    //private stuff
    private void startFutureAction(FutureAction futureAction) {
        if (futureAction != null) {
            futureAction.post();
            processFutureActionResults(futureAction);
        }
    }

    private void startObservableAction(ObservableAction observableAction) {
        if (observableAction != null) {
            observableAction.start();
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
        actionArrayDeque.addLast(action);
        if (actionArrayDeque.size() == 1) { //First element -- only one action at a time
            return action;
        }
        return null;
    }

    synchronized private void finishAction(Action action) {
        actionArrayDeque.remove(action);
        Action nextActionToProcess = actionArrayDeque.peek();
        if (action instanceof FutureAction) {
            startFutureAction((FutureAction)nextActionToProcess);
        } else if (action instanceof ObservableAction) {
            startObservableAction((ObservableAction)action);
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
                    finishAction(futureAction);
                }
            }
        }, executor);
    }

    private void processObservableActionResults(final ObservableAction observableAction) {
        if (observableAction == null) {
            return;
        }
        observableAction.getObservable()
                .subscribeOn(Schedulers.from(executor))
                .subscribeWith(new DisposableObserver() {
            @Override
            public void onNext(Object o) {
            }

            @Override
            public void onError(Throwable e) {
                //finish the action here
                finishAction(observableAction);
            }

            @Override
            public void onComplete() {
                //finish it here too
                finishAction(observableAction);
            }
        });
    }

}
