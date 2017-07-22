package com.inceptai.wifimonitoringservice;

import android.content.Context;

import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.NetworkActionLayer;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.CheckIf5GHzIsSupported;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.ConnectToBestConfiguredNetwork;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.ConnectToBestConfiguredNetworkIfAvailable;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.ConnectWithGivenWifiNetwork;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.DisconnectFromCurrentWifi;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.ForgetWifiNetwork;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.FutureAction;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.GetBestConfiguredNetwork;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.GetConfiguredNetworks;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.GetDHCPInfo;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.GetNearbyWifiNetworks;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.GetWifiInfo;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.IterateAndRepairWifiNetwork;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.PerformConnectivityTest;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.RepairWifiNetwork;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.ResetConnectionWithCurrentWifi;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.ToggleWifi;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.TurnWifiOff;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.TurnWifiOn;
import com.inceptai.wifimonitoringservice.utils.ServiceLog;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by Vivek on 7/6/17.
 */

public class ActionLibrary {
    private NetworkActionLayer networkActionLayer;
    private Context context;
    private Executor executor;
    private ScheduledExecutorService scheduledExecutorService;
    private ArrayDeque<FutureAction> futureActionArrayDeque;

    public ActionLibrary(Context context, Executor executor, ScheduledExecutorService scheduledExecutorService) {
        this.context = context;
        this.executor = executor;
        this.scheduledExecutorService = scheduledExecutorService;
        networkActionLayer = new NetworkActionLayer(context, executor, scheduledExecutorService);
        futureActionArrayDeque = new ArrayDeque<>();
    }

    public void cleanup() {
        networkActionLayer.cleanup();
        futureActionArrayDeque.clear();
    }

    public FutureAction turnWifiOn(long actionTimeOutMs) {
        FutureAction futureAction = new TurnWifiOn(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        submitAction(futureAction);
        return futureAction;
    }

    public FutureAction turnWifiOff(long actionTimeOutMs) {
        FutureAction futureAction = new TurnWifiOff(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        submitAction(futureAction);
        return futureAction;
    }

    public FutureAction toggleWifi(long actionTimeOutMs) {
        FutureAction futureAction = new ToggleWifi(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        submitAction(futureAction);
        return futureAction;
    }

    public FutureAction checkIf5GHzSupported(long actionTimeOutMs) {
        FutureAction futureAction = new CheckIf5GHzIsSupported(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        submitAction(futureAction);
        return futureAction;
    }

    public FutureAction disconnect(long actionTimeOutMs) {
        FutureAction futureAction = new DisconnectFromCurrentWifi(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        submitAction(futureAction);
        return futureAction;
    }

    public FutureAction connectWithWifiNetwork(int networkId, long actionTimeOutMs) {
        FutureAction futureAction = new ConnectWithGivenWifiNetwork(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs, networkId);
        submitAction(futureAction);
        return futureAction;
    }

    public FutureAction forgetWifiNetwork(int networkId, long actionTimeOutMs) {
        FutureAction futureAction = new ForgetWifiNetwork(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs, networkId);
        submitAction(futureAction);
        return futureAction;
    }

    public FutureAction getBestConfiguredNetwork(long actionTimeOutMs) {
        FutureAction futureAction = new GetBestConfiguredNetwork(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs, null);
        submitAction(futureAction);
        return futureAction;
    }

    public FutureAction getConfiguredNetworks(long actionTimeOutMs) {
        FutureAction futureAction = new GetConfiguredNetworks(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        submitAction(futureAction);
        return futureAction;
    }

    public FutureAction getDhcpInfo(long actionTimeOutMs) {
        FutureAction futureAction = new GetDHCPInfo(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        submitAction(futureAction);
        return futureAction;
    }

    public FutureAction getWifiInfo(long actionTimeOutMs) {
        FutureAction futureAction = new GetWifiInfo(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        submitAction(futureAction);
        return futureAction;
    }

    public FutureAction getNearbyWifiNetworks(long actionTimeOutMs) {
        FutureAction futureAction = new GetNearbyWifiNetworks(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        submitAction(futureAction);
        return futureAction;
    }

    public FutureAction connectToBestWifi(long actionTimeOutMs) {
        FutureAction futureAction = new ConnectToBestConfiguredNetworkIfAvailable(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        submitAction(futureAction);
        return futureAction;
    }

    public FutureAction connectToBestWifi(long actionTimeOutMs, List<String> offlineRouterIds) {
        FutureAction futureAction = new ConnectToBestConfiguredNetwork(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs, offlineRouterIds);
        submitAction(futureAction);
        return futureAction;
    }

    public FutureAction resetConnection(long actionTimeOutMs) {
        FutureAction futureAction = new ResetConnectionWithCurrentWifi(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        submitAction(futureAction);
        return futureAction;
    }

    public FutureAction repairWifiNetwork(long actionTimeOutMs) {
        FutureAction futureAction = new RepairWifiNetwork(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        submitAction(futureAction);
        return futureAction;
    }

    public FutureAction iterateAndRepairWifiNetwork(long actionTimeOutMs) {
        FutureAction futureAction = new IterateAndRepairWifiNetwork(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        submitAction(futureAction);
        return futureAction;
    }

    public FutureAction performConnectivityTest(long actionTimeOutMs) {
        FutureAction futureAction = new PerformConnectivityTest(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        submitAction(futureAction);
        return futureAction;
    }

    public ArrayList<String> getListOfPendingActions() {
        ArrayList<String> actionList = new ArrayList<>();
        for (FutureAction futureAction: futureActionArrayDeque) {
            actionList.add(futureAction.getActionType());
        }
        return actionList;
    }

    void cancelPendingActions() {
        FutureAction currentlyRunningAction = futureActionArrayDeque.peek();
        for (FutureAction futureAction: futureActionArrayDeque) {
            if (futureAction != null && currentlyRunningAction != futureAction) {
                futureAction.cancelAction();
            }
        }
    }

    int numberOfPendingActions() {
        return futureActionArrayDeque.size();
    }

    //private stuff
    private void postAndWaitForResults(FutureAction futureAction) {
        if (futureAction != null) {
            futureAction.post();
            processResultsWhenAvailable(futureAction);
        }
    }

    private void submitAction(FutureAction futureAction) {
        postAndWaitForResults(addAction(futureAction));
    }


    synchronized private FutureAction addAction(FutureAction futureAction) {
        futureActionArrayDeque.addLast(futureAction);
        if (futureActionArrayDeque.size() == 1) { //First element -- only one action at a time
            return futureAction;
        }
        return null;
    }

    synchronized private void finishAction(FutureAction futureAction) {
        futureActionArrayDeque.remove(futureAction);
        postAndWaitForResults(futureActionArrayDeque.peek());
    }


    private void processResultsWhenAvailable(final FutureAction futureAction) {
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

}
