package com.inceptai.dobby.ai;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.dobby.DobbyThreadpool;

/**
 * Created by arunesh on 4/19/17.
 */

public abstract class ComposableOperation {

    private DobbyThreadpool threadpool;
    private ComposableOperation uponCompletion;

    ComposableOperation(DobbyThreadpool threadpool) {
        this.threadpool = threadpool;
    }

    public abstract void post();
    protected abstract String getName();

    public void uponCompletion(ComposableOperation operation) {
        uponCompletion = operation;
    }

    /**
     * Derived class should set the Future object as soon as it has one.
     * @param future
     */
    protected void setFuture(ListenableFuture<?> future) {
        future.addListener(new Runnable() {
            @Override
            public void run() {
                if (uponCompletion != null) {
                    uponCompletion.post();
                }
            }
        }, threadpool.getExecutor());
    }

//    public void postAfter(ComposableOperation operation) {
//        if (operation.getFuture().isDone()) {
//            post();
//        } else {
//            operation.getFuture().addListener(new Runnable() {
//                @Override
//                public void run() {
//                    post();
//                }
//            }, threadpool.getExecutor());
//        }
//    }
}
