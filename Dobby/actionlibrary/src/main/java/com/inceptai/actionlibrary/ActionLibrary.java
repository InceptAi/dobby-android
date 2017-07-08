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
import com.inceptai.actionlibrary.actions.RepairWifiNetwork;
import com.inceptai.actionlibrary.actions.ResetConnectionWithCurrentWifi;
import com.inceptai.actionlibrary.actions.ToggleWifi;
import com.inceptai.actionlibrary.actions.TurnWifiOff;
import com.inceptai.actionlibrary.actions.TurnWifiOn;

/**
 * Created by Vivek on 7/6/17.
 */

public class ActionLibrary {
    private NetworkActionLayer networkActionLayer;
    private ActionThreadPool actionThreadPool;
    private Context context;

    public ActionLibrary(Context context) {
        this.context = context;
        actionThreadPool = new ActionThreadPool();
        networkActionLayer = new NetworkActionLayer(context, actionThreadPool);
    }

    public FutureAction turnWifiOn(long timeOut) {
        FutureAction futureAction = new TurnWifiOn(context, actionThreadPool, networkActionLayer, timeOut);
        futureAction.post();
        return futureAction;
    }

    public FutureAction turnWifiOff(long timeOut) {
        FutureAction futureAction = new TurnWifiOff(context, actionThreadPool, networkActionLayer, timeOut);
        futureAction.post();
        return futureAction;
    }

    public FutureAction toggleWifi(long timeOut) {
        FutureAction futureAction = new ToggleWifi(context, actionThreadPool, networkActionLayer, timeOut);
        futureAction.post();
        return futureAction;
    }

    public FutureAction checkIf5GHzSupported(long timeOut) {
        FutureAction futureAction = new CheckIf5GHzIsSupported(context, actionThreadPool, networkActionLayer, timeOut);
        futureAction.post();
        return futureAction;
    }

    public FutureAction disconnect(long timeOut) {
        FutureAction futureAction = new DisconnectFromCurrentWifi(context, actionThreadPool, networkActionLayer, timeOut);
        futureAction.post();
        return futureAction;
    }

    public FutureAction connectWithWifiNetwork(int networkId, long timeOut) {
        FutureAction futureAction = new ConnectWithGivenWifiNetwork(context, actionThreadPool, networkActionLayer, timeOut, networkId);
        futureAction.post();
        return futureAction;
    }

    public FutureAction forgetWifiNetwork(int networkId, long timeOut) {
        FutureAction futureAction = new ForgetWifiNetwork(context, actionThreadPool, networkActionLayer, timeOut, networkId);
        futureAction.post();
        return futureAction;
    }

    public FutureAction getBestConfiguredNetwork(long timeOut) {
        FutureAction futureAction = new GetBestConfiguredNetwork(context, actionThreadPool, networkActionLayer, timeOut);
        futureAction.post();
        return futureAction;
    }

    public FutureAction getConfiguredNetworks(long timeOut) {
        FutureAction futureAction = new GetConfiguredNetworks(context, actionThreadPool, networkActionLayer, timeOut);
        futureAction.post();
        return futureAction;
    }

    public FutureAction getDhcpInfo(long timeOut) {
        FutureAction futureAction = new GetDHCPInfo(context, actionThreadPool, networkActionLayer, timeOut);
        futureAction.post();
        return futureAction;
    }

    public FutureAction getWifiInfo(long timeOut) {
        FutureAction futureAction = new GetWifiInfo(context, actionThreadPool, networkActionLayer, timeOut);
        futureAction.post();
        return futureAction;
    }

    public FutureAction getNearbyWifiNetworks(long timeOut) {
        FutureAction futureAction = new GetNearbyWifiNetworks(context, actionThreadPool, networkActionLayer, timeOut);
        futureAction.post();
        return futureAction;
    }

    public FutureAction connectToBestWifi(long timeOut) {
        FutureAction futureAction = new ConnectToBestConfiguredNetworkIfAvailable(context, actionThreadPool, networkActionLayer, timeOut);
        futureAction.post();
        return futureAction;
    }

    public FutureAction resetConnection(long timeOut) {
        FutureAction futureAction = new ResetConnectionWithCurrentWifi(context, actionThreadPool, networkActionLayer, timeOut);
        futureAction.post();
        return futureAction;
    }

    public FutureAction repairWifiNetwork(long timeOut) {
        FutureAction futureAction = new RepairWifiNetwork(context, actionThreadPool, networkActionLayer, timeOut);
        futureAction.post();
        return futureAction;
    }

}
