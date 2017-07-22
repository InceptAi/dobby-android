package com.inceptai.wifimonitoringservice.actionlibrary.actions;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.wifimonitoringservice.R;
import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.NetworkActionLayer;
import com.inceptai.wifimonitoringservice.utils.ServiceLog;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by vivek on 7/5/17.
 */

public class ToggleWifi extends FutureAction {

    public ToggleWifi(Context context,
                      Executor executor,
                      ScheduledExecutorService scheduledExecutorService,
                      NetworkActionLayer networkActionLayer,
                      long actionTimeOutMs) {
        super(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
    }

    @Override
    public void post() {
        final FutureAction turnWifiOff = new TurnWifiOff(context, executor,  scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        final FutureAction turnWifiOn = new TurnWifiOn(context, executor,  scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        turnWifiOff.uponCompletion(turnWifiOn);
        //setFuture(turnWifiOn.getFuture());
        turnWifiOn.getFuture().addListener(new Runnable() {
            @Override
            public void run() {
                ActionResult actionResult = null;
                try {
                    actionResult = turnWifiOn.getFuture().get();
                    setResult(actionResult);
                } catch (InterruptedException | ExecutionException | CancellationException e) {
                    e.printStackTrace(System.out);
                    ServiceLog.w("ActionTaker: Exception getting wifi results: " + e.toString());
                    setResult(new ActionResult(ActionResult.ActionResultCodes.EXCEPTION, e.toString()));
                }
            }
        }, executor);
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
