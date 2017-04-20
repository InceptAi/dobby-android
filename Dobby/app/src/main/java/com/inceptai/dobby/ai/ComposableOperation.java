package com.inceptai.dobby.ai;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.dobby.DobbyThreadpool;

/**
 * Created by arunesh on 4/19/17.
 */

public abstract class ComposableOperation {

    private DobbyThreadpool threadpool;
    private ListenableFuture<?> future;

    ComposableOperation(DobbyThreadpool threadpool) {
        this.threadpool = threadpool;
    }

    public void post() {
        future = threadpool.getListeningExecutorService().submit(new Runnable() {
            @Override
            public void run() {
                performOperation();
            }
        });
    }

    protected abstract void performOperation();

    protected abstract String getName();

    protected ListenableFuture<?> getFuture() {
        return future;
    }

    public void postAfter(ComposableOperation operation) {
        if (operation.getFuture().isDone()) {
            post();
        } else {
            operation.getFuture().addListener(new Runnable() {
                @Override
                public void run() {
                    post();
                }
            }, threadpool.getExecutor());
        }
    }
}
