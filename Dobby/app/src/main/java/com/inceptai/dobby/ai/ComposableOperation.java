package com.inceptai.dobby.ai;

import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.inceptai.dobby.DobbyThreadpool;

import static com.inceptai.dobby.DobbyApplication.TAG;
import static com.inceptai.dobby.ai.OperationResult.SUCCESS;

/**
 * Created by arunesh on 4/19/17.
 */

public abstract class ComposableOperation {
    private DobbyThreadpool threadpool;
    private ComposableOperation uponCompletion;
    private SettableFuture<OperationResult> settableFuture;

    ComposableOperation(DobbyThreadpool threadpool) {
        this.threadpool = threadpool;
        settableFuture = SettableFuture.create();
        addCompletionWork();
    }

    public abstract void post();
    protected abstract String getName();

    public void uponCompletion(ComposableOperation operation) {
        uponCompletion = operation;
    }

    protected void setFuture(@Nullable final ListenableFuture<?> future) {
        if (future == null) {
            setResult(new OperationResult(OperationResult.FAILED_TO_START));
            return;
        }
        future.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    setResult(new OperationResult(SUCCESS, future.get()));
                } catch (Exception e) {
                    Log.w(TAG, "Exception getting result for:" + getName());
                }
            }
        }, threadpool.getExecutor());
    }

    protected void setResult(OperationResult result) {
        settableFuture.set(result);
    }

    public ListenableFuture<OperationResult> getFuture() {
        return settableFuture;
    }

    private void addCompletionWork() {
        settableFuture.addListener(new Runnable() {
            @Override
            public void run() {
                if (uponCompletion != null) {
                    uponCompletion.post();
                }
            }
        }, threadpool.getExecutor());
    }
}
