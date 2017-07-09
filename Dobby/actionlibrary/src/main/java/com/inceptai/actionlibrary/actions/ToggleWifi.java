package com.inceptai.actionlibrary.actions;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.actionlibrary.ActionResult;
import com.inceptai.actionlibrary.ActionThreadPool;
import com.inceptai.actionlibrary.NetworkLayer.NetworkActionLayer;
import com.inceptai.actionlibrary.R;
import com.inceptai.actionlibrary.utils.ActionLog;

import java.util.concurrent.ExecutionException;

/**
 * Created by vivek on 7/5/17.
 */

public class ToggleWifi extends FutureAction {

    public ToggleWifi(Context context, ActionThreadPool actionThreadPool, NetworkActionLayer networkActionLayer, long actionTimeOutMs) {
        super(context, actionThreadPool, networkActionLayer, actionTimeOutMs);
    }

    @Override
    public void post() {
        final FutureAction turnWifiOff = new TurnWifiOff(context, actionThreadPool, networkActionLayer, actionTimeOutMs);
        final FutureAction turnWifiOn = new TurnWifiOn(context, actionThreadPool, networkActionLayer, actionTimeOutMs);
        turnWifiOff.uponCompletion(turnWifiOn);
        //setFuture(turnWifiOn.getFuture());
        turnWifiOn.getFuture().addListener(new Runnable() {
            @Override
            public void run() {
                ActionResult actionResult = null;
                try {
                    actionResult = turnWifiOn.getFuture().get();
                    setResult(actionResult);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace(System.out);
                    ActionLog.w("ActionTaker: Exception getting wifi results: " + e.toString());
                    setResult(new ActionResult(ActionResult.ActionResultCodes.EXCEPTION, e.toString()));
                }
            }
        }, actionThreadPool.getExecutor());
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
