package com.inceptai.actionlibrary.actions;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.actionlibrary.ActionResult;
import com.inceptai.actionlibrary.ActionThreadPool;
import com.inceptai.actionlibrary.NetworkLayer.NetworkActionLayer;
import com.inceptai.actionlibrary.R;

/**
 * Created by vivek on 7/5/17.
 */

public class RepairWifiNetwork extends FutureAction {

    public RepairWifiNetwork(Context context, ActionThreadPool threadpool, NetworkActionLayer networkActionLayer, long actionTimeOutMs) {
        super(context, threadpool, networkActionLayer, actionTimeOutMs);
    }

    @Override
    public void post() {
        FutureAction toggleWifiAction = new ToggleWifi(context, actionThreadPool, networkActionLayer, actionTimeOutMs);
        FutureAction connectToBestWifiNetwork = new ConnectToBestConfiguredNetworkIfAvailable(context, actionThreadPool, networkActionLayer, actionTimeOutMs);
        FutureAction getWifiInfoAction = new GetWifiInfo(context, actionThreadPool, networkActionLayer, actionTimeOutMs);
        toggleWifiAction.uponCompletion(connectToBestWifiNetwork);
        connectToBestWifiNetwork.uponCompletion(getWifiInfoAction);
        setFuture(getWifiInfoAction.getSettableFuture());
        toggleWifiAction.post();
    }

    @Override
    public String getName() {
        return context.getString(R.string.repair_wifi_network);
    }

    @Override
    public void uponCompletion(FutureAction operation) {
        super.uponCompletion(operation);
    }

    @Override
    protected void setFuture(@Nullable ListenableFuture<?> future) {
        super.setFuture(future);
    }

    @Override
    protected void setResult(ActionResult result) {
        super.setResult(result);
    }

    @Override
    public ListenableFuture<ActionResult> getFuture() {
        return super.getFuture();
    }
}
