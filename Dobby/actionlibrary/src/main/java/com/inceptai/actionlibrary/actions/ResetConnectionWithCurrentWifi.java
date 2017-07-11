package com.inceptai.actionlibrary.actions;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.actionlibrary.ActionResult;
import com.inceptai.actionlibrary.NetworkLayer.NetworkActionLayer;
import com.inceptai.actionlibrary.R;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by vivek on 7/5/17.
 */

public class ResetConnectionWithCurrentWifi extends FutureAction {

    public ResetConnectionWithCurrentWifi(Context context,
                                          Executor executor,
                                          ScheduledExecutorService scheduledExecutorService,
                                          NetworkActionLayer networkActionLayer,
                                          long actionTimeOutMs) {
        super(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
    }

    @Override
    public void post() {
        setFuture(networkActionLayer.resetConnectionToActiveWifi());
    }

    @Override
    public String getName() {
        return context.getString(R.string.reset_connection_to_current_wifi);
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
