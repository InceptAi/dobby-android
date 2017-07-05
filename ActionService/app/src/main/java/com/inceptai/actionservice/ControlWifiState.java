package com.inceptai.actionservice;

import android.support.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Created by vivek on 7/5/17.
 */

public class ControlWifiState extends Action {

    ControlWifiState(ActionThreadPool threadpool) {
        super(threadpool);
    }

    @Override
    public void post(long timeOut) {

    }

    @Override
    public void post() {
        super.post();
    }

    @Override
    protected String getName() {
        return null;
    }

    @Override
    public void uponCompletion(Action operation) {
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
