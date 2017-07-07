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

public class ResetConnectionWithCurrentWifi extends FutureAction {

    public ResetConnectionWithCurrentWifi(Context context, ActionThreadPool threadPool, NetworkActionLayer networkActionLayer, long timeOut) {
        super(context, threadPool, networkActionLayer, timeOut);
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
