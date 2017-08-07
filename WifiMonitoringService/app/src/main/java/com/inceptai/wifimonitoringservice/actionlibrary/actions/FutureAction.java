package com.inceptai.wifimonitoringservice.actionlibrary.actions;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.NetworkActionLayer;
import com.inceptai.wifimonitoringservice.utils.ServiceLog;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.inceptai.wifimonitoringservice.actionlibrary.ActionResult.ActionResultCodes.EXCEPTION;
import static com.inceptai.wifimonitoringservice.actionlibrary.ActionResult.ActionResultCodes.FAILED_TO_START;
import static com.inceptai.wifimonitoringservice.actionlibrary.ActionResult.ActionResultCodes.SUCCESS;


/**
 * Created by vivek on 7/5/17.
 */



public abstract class FutureAction extends Action {
    private FutureAction uponCompletion;
    private FutureAction uponSuccessfulCompletion;
    private SettableFuture<ActionResult> settableFuture;

    FutureAction(@ActionType int actionType,
                 Context context,
                 Executor executor,
                 ScheduledExecutorService scheduledExecutorService,
                 NetworkActionLayer networkActionLayer,
                 long actionTimeOutMs) {
        super(actionType, context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        settableFuture = SettableFuture.create();
        addCompletionWork();
    }

    public abstract void post();

    public abstract String getName();

    public void uponCompletion(FutureAction futureAction) {
        uponCompletion = futureAction;
    }

    public void uponSuccessfulCompletion(FutureAction futureAction) {
        uponSuccessfulCompletion = futureAction;
    }

    public void cancelAction() {
        settableFuture.cancel(true);
        if (uponSuccessfulCompletion != null) {
            uponSuccessfulCompletion.cancelAction();
        }
        if (uponCompletion != null) {
            uponCompletion.cancelAction();
        }
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
                    ServiceLog.v("FutureAction: timeout for action " + getName());
                    setResult(new ActionResult(ActionResult.ActionResultCodes.TIMED_OUT));
                    future.cancel(true);
                } catch (CancellationException e) {
                    ServiceLog.v("Exception while cancelling task.");
                }
            }
        }, actionTimeOutMs, TimeUnit.MILLISECONDS);

        future.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ServiceLog.v("FutureAction: Setting result for action with name " + getName());
                    setResult(new ActionResult(SUCCESS, ActionResult.actionResultCodeToString(SUCCESS), future.get()));
                } catch (InterruptedException | ExecutionException | CancellationException e) {
                    ServiceLog.e("Exception getting result for:" + getName() +
                            " e = " + e.toString());
                    setResult(new ActionResult(EXCEPTION, e.toString()));
                } finally {
                    timeOutFuture.cancel(true);
                }
            }
        }, executor);
    }

    protected void setResult(ActionResult result) {
        actionResult = result;
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
                    ServiceLog.v("CO: Running upon completion for " + getName());
                    uponCompletion.post();
                }
                //Add upon success
                if (uponSuccessfulCompletion != null) {
                    if (ActionResult.isSuccessful(actionResult)) {
                        ServiceLog.v("CO: Running upon successful completion for " + getName());
                        uponSuccessfulCompletion.post();
                    } else {
                        uponSuccessfulCompletion.setResult(new ActionResult(ActionResult.ActionResultCodes.FAILED_TO_START));
                    }
                }
            }
        }, executor);
    }

    public SettableFuture<ActionResult> getSettableFuture() {
        return settableFuture;
    }
}
