package com.inceptai.actionlibrary;

import android.content.Context;

import com.inceptai.actionlibrary.NetworkLayer.NetworkActionLayer;
import com.inceptai.actionlibrary.actions.CheckIf5GHzIsSupported;
import com.inceptai.actionlibrary.actions.ConnectToBestConfiguredNetworkIfAvailable;
import com.inceptai.actionlibrary.actions.ConnectWithGivenWifiNetwork;
import com.inceptai.actionlibrary.actions.DisconnectFromCurrentWifi;
import com.inceptai.actionlibrary.actions.ForgetWifiNetwork;
import com.inceptai.actionlibrary.actions.FutureAction;
import com.inceptai.actionlibrary.actions.GetBestConfiguredNetwork;
import com.inceptai.actionlibrary.actions.GetConfiguredNetworks;
import com.inceptai.actionlibrary.actions.GetDHCPInfo;
import com.inceptai.actionlibrary.actions.GetNearbyWifiNetworks;
import com.inceptai.actionlibrary.actions.GetWifiInfo;
import com.inceptai.actionlibrary.actions.PerformConnectivityTest;
import com.inceptai.actionlibrary.actions.RepairWifiNetwork;
import com.inceptai.actionlibrary.actions.ResetConnectionWithCurrentWifi;
import com.inceptai.actionlibrary.actions.ToggleWifi;
import com.inceptai.actionlibrary.actions.TurnWifiOff;
import com.inceptai.actionlibrary.actions.TurnWifiOn;

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

    public ActionLibrary(Context context, Executor executor, ScheduledExecutorService scheduledExecutorService) {
        this.context = context;
        this.executor = executor;
        this.scheduledExecutorService = scheduledExecutorService;
        networkActionLayer = new NetworkActionLayer(context, executor, scheduledExecutorService);
    }

    public FutureAction turnWifiOn(long actionTimeOutMs) {
        FutureAction futureAction = new TurnWifiOn(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        futureAction.post();
        return futureAction;
    }

    public FutureAction turnWifiOff(long actionTimeOutMs) {
        FutureAction futureAction = new TurnWifiOff(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        futureAction.post();
        return futureAction;
    }

    public FutureAction toggleWifi(long actionTimeOutMs) {
        FutureAction futureAction = new ToggleWifi(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        futureAction.post();
        return futureAction;
    }

    public FutureAction checkIf5GHzSupported(long actionTimeOutMs) {
        FutureAction futureAction = new CheckIf5GHzIsSupported(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        futureAction.post();
        return futureAction;
    }

    public FutureAction disconnect(long actionTimeOutMs) {
        FutureAction futureAction = new DisconnectFromCurrentWifi(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        futureAction.post();
        return futureAction;
    }

    public FutureAction connectWithWifiNetwork(int networkId, long actionTimeOutMs) {
        FutureAction futureAction = new ConnectWithGivenWifiNetwork(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs, networkId);
        futureAction.post();
        return futureAction;
    }

    public FutureAction forgetWifiNetwork(int networkId, long actionTimeOutMs) {
        FutureAction futureAction = new ForgetWifiNetwork(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs, networkId);
        futureAction.post();
        return futureAction;
    }

    public FutureAction getBestConfiguredNetwork(long actionTimeOutMs) {
        FutureAction futureAction = new GetBestConfiguredNetwork(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        futureAction.post();
        return futureAction;
    }

    public FutureAction getConfiguredNetworks(long actionTimeOutMs) {
        FutureAction futureAction = new GetConfiguredNetworks(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        futureAction.post();
        return futureAction;
    }

    public FutureAction getDhcpInfo(long actionTimeOutMs) {
        FutureAction futureAction = new GetDHCPInfo(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        futureAction.post();
        return futureAction;
    }

    public FutureAction getWifiInfo(long actionTimeOutMs) {
        FutureAction futureAction = new GetWifiInfo(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        futureAction.post();
        return futureAction;
    }

    public FutureAction getNearbyWifiNetworks(long actionTimeOutMs) {
        FutureAction futureAction = new GetNearbyWifiNetworks(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        futureAction.post();
        return futureAction;
    }

    public FutureAction connectToBestWifi(long actionTimeOutMs) {
        FutureAction futureAction = new ConnectToBestConfiguredNetworkIfAvailable(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        futureAction.post();
        return futureAction;
    }

    public FutureAction resetConnection(long actionTimeOutMs) {
        FutureAction futureAction = new ResetConnectionWithCurrentWifi(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        futureAction.post();
        return futureAction;
    }

    public FutureAction repairWifiNetwork(long actionTimeOutMs) {
        FutureAction futureAction = new RepairWifiNetwork(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        futureAction.post();
        return futureAction;
    }

    public FutureAction performConnectivityTest(long actionTimeOutMs) {
        FutureAction futureAction = new PerformConnectivityTest(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        futureAction.post();
        return futureAction;
    }

}
