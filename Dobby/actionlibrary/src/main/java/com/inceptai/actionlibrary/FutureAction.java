package com.inceptai.actionlibrary;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.inceptai.actionlibrary.NetworkLayer.NetworkLayer;

import java.util.concurrent.ExecutionException;

import static com.inceptai.actionlibrary.ActionResult.ActionResultCodes.FAILED_TO_START;
import static com.inceptai.actionlibrary.ActionResult.ActionResultCodes.GENERAL_ERROR;
import static com.inceptai.actionlibrary.ActionResult.ActionResultCodes.SUCCESS;

/**
 * Created by vivek on 7/5/17.
 */



public abstract class FutureAction {
    private static final String TAG = "ActionService";
    private ActionThreadPool threadpool;
    private FutureAction uponCompletion;
    private SettableFuture<ActionResult> settableFuture;
    private long timeOut;
    Context context;
    NetworkLayer networkLayer;

    FutureAction(Context context, ActionThreadPool threadpool, NetworkLayer networkLayer, long timeOut) {
        this.context = context;
        this.threadpool = threadpool;
        this.timeOut = timeOut;
        settableFuture = SettableFuture.create();
        this.networkLayer = networkLayer;
        addCompletionWork();
    }

    public abstract void post();

    protected abstract String getName();

    public void uponCompletion(FutureAction futureAction) {
        uponCompletion = futureAction;
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
