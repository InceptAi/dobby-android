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

public class ToggleWifi extends FutureAction {

    public ToggleWifi(Context context, ActionThreadPool threadpool, NetworkActionLayer networkActionLayer, long actionTimeOutMs) {
        super(context, threadpool, networkActionLayer, actionTimeOutMs);
    }

    @Override
    public void post() {
        FutureAction turnWifiOff = new TurnWifiOff(context, actionThreadPool, networkActionLayer, actionTimeOutMs);
        FutureAction turnWifiOn = new TurnWifiOn(context, actionThreadPool, networkActionLayer, actionTimeOutMs);
        turnWifiOff.uponCompletion(turnWifiOn);
        setFuture(turnWifiOn.getSettableFuture());
        turnWifiOff.post();
    }

    @Override
    public String getName() {
        return context.getString(R.string.toggle_wifi_off_and_on);
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
