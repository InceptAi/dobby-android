package com.inceptai.actionservice;

import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;

import java.util.concurrent.ExecutionException;

import static com.inceptai.actionservice.ActionResult.ActionResultCodes.FAILED_TO_START;
import static com.inceptai.actionservice.ActionResult.ActionResultCodes.GENERAL_ERROR;
import static com.inceptai.actionservice.ActionResult.ActionResultCodes.SUCCESS;

/**
 * Created by vivek on 7/5/17.
 */



public abstract class Action {
    public static final String TAG = "ActionService";
    private ActionThreadPool threadpool;
    private Action uponCompletion;
    private SettableFuture<ActionResult> settableFuture;

    Action(ActionThreadPool threadpool) {
        this.threadpool = threadpool;
        settableFuture = SettableFuture.create();
        addCompletionWork();
    }

    public abstract void post(long timeOut);

    public void post() {
        post(0);
    }

    protected abstract String getName();

    public void uponCompletion(Action operation) {
        uponCompletion = operation;
    }

    protected void setFuture(@Nullable final ListenableFuture<?> future) {
        if (future == null) {
            setResult(new ActionResult(FAILED_TO_START,
                    ActionResult.actionResultCodeToString(FAILED_TO_START)));
            return;
        }
        future.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    setResult(new ActionResult(SUCCESS, ActionResult.actionResultCodeToString(SUCCESS), future.get()));
                } catch (InterruptedException | ExecutionException e) {
                    Log.w("", "Exception getting result for:" + getName() +
                            " e = " + e.getStackTrace().toString());
                    setResult(new ActionResult(GENERAL_ERROR, e.getStackTrace().toString()));
                }
            }
        }, threadpool.getExecutor());
    }

    protected void setResult(ActionResult result) {
        settableFuture.set(result);
    }

    public ListenableFuture<ActionResult> getFuture() {
        return settableFuture;
    }

    private void  addCompletionWork() {
        settableFuture.addListener(new Runnable() {
            @Override
            public void run() {
                if (uponCompletion != null) {
                    Log.v(TAG, "CO: Running upon completion for " + getName());
                    uponCompletion.post();
                }
            }
        }, threadpool.getExecutor());
    }
}
