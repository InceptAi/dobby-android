package com.inceptai.actionlibrary;

import android.content.Context;

import com.inceptai.actionlibrary.NetworkLayer.NetworkLayer;
import com.inceptai.actionlibrary.actions.FutureAction;
import com.inceptai.actionlibrary.actions.ToggleWifi;
import com.inceptai.actionlibrary.actions.TurnWifiOff;
import com.inceptai.actionlibrary.actions.TurnWifiOn;

/**
 * Created by vivek on 7/6/17.
 */

public class ActionLibrary {
    private NetworkLayer networkLayer;
    private ActionThreadPool actionThreadPool;
    private Context context;

    public ActionLibrary(Context context) {
        this.context = context;
        actionThreadPool = new ActionThreadPool();
        networkLayer = new NetworkLayer(context, actionThreadPool);
    }

    public FutureAction turnWifiOn(long timeOut) {
        FutureAction futureAction = new TurnWifiOn(context, actionThreadPool, networkLayer, timeOut);
        futureAction.post();
        return futureAction;
    }

    public FutureAction turnWifiOff(long timeOut) {
        FutureAction futureAction = new TurnWifiOff(context, actionThreadPool, networkLayer, timeOut);
        futureAction.post();
        return futureAction;
    }

    public FutureAction toggleWifi(long timeOut) {
        FutureAction futureAction = new ToggleWifi(context, actionThreadPool, networkLayer, timeOut);
        futureAction.post();
        return futureAction;
    }

}
