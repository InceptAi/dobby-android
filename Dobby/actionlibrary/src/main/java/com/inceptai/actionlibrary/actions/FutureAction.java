package com.inceptai.actionlibrary.actions;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.inceptai.actionlibrary.ActionResult;
import com.inceptai.actionlibrary.NetworkLayer.NetworkActionLayer;
import com.inceptai.actionlibrary.utils.ActionLog;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.inceptai.actionlibrary.ActionResult.ActionResultCodes.EXCEPTION;
import static com.inceptai.actionlibrary.ActionResult.ActionResultCodes.FAILED_TO_START;
import static com.inceptai.actionlibrary.ActionResult.ActionResultCodes.SUCCESS;


/**
 * Created by vivek on 7/5/17.
 */



public abstract class FutureAction {
    private static final String TAG = "ActionService";
    private FutureAction uponCompletion;
    private SettableFuture<ActionResult> settableFuture;
    Executor executor;
    ScheduledExecutorService scheduledExecutorService;
    long actionTimeOutMs;
    Context context;
    NetworkActionLayer networkActionLayer;

    FutureAction(Context context,
                 Executor executor,
                 ScheduledExecutorService scheduledExecutorService,
                 NetworkActionLayer networkActionLayer,
                 long actionTimeOutMs) {
        this.context = context;
        this.executor = executor;
        this.scheduledExecutorService = scheduledExecutorService;
        this.actionTimeOutMs = actionTimeOutMs;
        settableFuture = SettableFuture.create();
        this.networkActionLayer = networkActionLayer;
        addCompletionWork();
    }

    public abstract void post();

    public abstract String getName();

    public void uponCompletion(FutureAction futureAction) {
        uponCompletion = futureAction;
    }

    protected void setFuture(@Nullable final ListenableFuture<?> future) {
        if (future == null) {
            setResult(new ActionResult(FAILED_TO_START,
                    ActionResult.actionResultCodeToString(FAILED_TO_START)));
            return;
        }

        final ScheduledFuture<?> timeOutFuture = scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                try {
                    ActionLog.v("FutureAction: timeout for action " + getName());
                    setResult(new ActionResult(ActionResult.ActionResultCodes.TIMED_OUT));
                    future.cancel(true);
                } catch (CancellationException e) {
                    ActionLog.v("Exception while cancelling task.");
                }
            }
        }, actionTimeOutMs, TimeUnit.MILLISECONDS);

        future.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ActionLog.v("FutureAction: Setting result for action with name " + getName());
                    setResult(new ActionResult(SUCCESS, ActionResult.actionResultCodeToString(SUCCESS), future.get()));
                } catch (InterruptedException | ExecutionException e) {
                    Log.w("", "Exception getting result for:" + getName() +
                            " e = " + e.toString());
                    setResult(new ActionResult(EXCEPTION, e.toString()));
                } finally {
                    timeOutFuture.cancel(true);
                }
            }
        }, executor);
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
        }, executor);
    }

    public SettableFuture<ActionResult> getSettableFuture() {
        return settableFuture;
    }
}
