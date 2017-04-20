package com.inceptai.dobby.ai;

import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.inceptai.dobby.DobbyThreadpool;

import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * Created by arunesh on 4/19/17.
 */

public abstract class ComposableOperation<T> {
    private DobbyThreadpool threadpool;
    private ComposableOperation uponCompletion;
    private SettableFuture<T> settableFuture;

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

    protected void setFuture(final ListenableFuture<T> future) {
        future.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    setResult(future.get());
                } catch (Exception e) {
                    Log.w(TAG, "Exception getting result for:" + getName());
                }
            }
        }, threadpool.getExecutor());
    }

    private void setResult(T result) {
        settableFuture.set(result);
    }

    public ListenableFuture<T> getFuture() {
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
