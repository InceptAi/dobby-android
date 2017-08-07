package com.inceptai.wifimonitoringservice.actionlibrary.actions;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.wifimonitoringservice.R;
import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.NetworkActionLayer;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by vivek on 7/5/17.
 */

public class TurnWifiOff extends FutureAction {

    public TurnWifiOff(Context context,
                       Executor executor,
                       ScheduledExecutorService scheduledExecutorService,
                       NetworkActionLayer networkActionLayer,
                       long actionTimeOutMs) {
        super(ActionType.TURN_WIFI_OFF, context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);;
    }

    @Override
    public void post() {
        setFuture(networkActionLayer.turnWifiOff());
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
