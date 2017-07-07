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

public class ForgetWifiNetwork extends FutureAction {

    private int networkId;

    public ForgetWifiNetwork(Context context, ActionThreadPool threadpool, NetworkActionLayer networkActionLayer, long timeOut, int networkId) {
        super(context, threadpool, networkActionLayer, timeOut);
        this.networkId = networkId;
    }

    @Override
    public void post() {
        setFuture(networkActionLayer.forgetWifiNetwork(networkId));
    }

    @Override
    public String getName() {
        return context.getString(R.string.turn_wifi_off);
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
