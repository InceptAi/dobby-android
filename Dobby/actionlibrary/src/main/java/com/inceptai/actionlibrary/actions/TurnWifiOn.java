package com.inceptai.actionlibrary.actions;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.actionlibrary.ActionResult;
import com.inceptai.actionlibrary.ActionThreadPool;
import com.inceptai.actionlibrary.NetworkLayer.NetworkLayer;
import com.inceptai.actionlibrary.R;

/**
 * Created by vivek on 7/5/17.
 */

public class TurnWifiOn extends FutureAction {

    public TurnWifiOn(Context context, ActionThreadPool threadpool, NetworkLayer networkLayer, long timeOut) {
        super(context, threadpool, networkLayer, timeOut);
    }

    @Override
    public void post() {
        setFuture(networkLayer.turnWifiOn());
    }

    @Override
    protected String getName() {
        return context.getString(R.string.turn_wifi_on);
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
