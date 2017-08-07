package com.inceptai.wifimonitoringservice.actionlibrary.actions;

import android.content.Context;

import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.NetworkActionLayer;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;


/**
 * Created by vivek on 7/5/17.
 */



public abstract class ObservableAction extends Action {
    private ObservableAction uponCompletion;
    private ObservableAction uponSuccessfulCompletion;
    private Observable observable;

    ObservableAction(@ActionType int actionType,
                     Context context,
                     Executor executor,
                     ScheduledExecutorService scheduledExecutorService,
                     NetworkActionLayer networkActionLayer,
                     long actionTimeOutMs) {
        super(actionType, context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        addCompletionWork();
    }

    public abstract void start();

    public abstract String getName();

    public void setObservable(Observable observable) {
        this.observable = observable;
        if (observable == null) {
            return;
        }
        observable.subscribeOn(Schedulers.from(executor))
                .timeout(actionTimeOutMs, TimeUnit.MILLISECONDS)
                .subscribeWith(new DisposableObserver() {
                    @Override
                    public void onNext(Object o) {
                    }

                    @Override
                    public void onError(Throwable e) {
                        //finish the action here
                    }

                    @Override
                    public void onComplete() {
                        //finish it here too
                    }
                });
    }

    public void uponCompletion(ObservableAction observableAction) {
        uponCompletion = observableAction;
    }

    public void uponSuccessfulCompletion(ObservableAction observableAction) {
        uponSuccessfulCompletion = observableAction;
    }


    public void cancelAction() {
        //Cancel the observable here
        if (observable != null) {
            observable.doOnDispose(new io.reactivex.functions.Action() {
                @Override
                public void run() throws Exception {
                    if (uponSuccessfulCompletion != null) {
                        uponSuccessfulCompletion.cancelAction();
                    }
                    if (uponCompletion != null) {
                        uponCompletion.cancelAction();
                    }
                }
            });
        }
    }


    public Observable getObservable() {
        return observable;
    }

    private void addCompletionWork() {
        //Not sure how to sequence observables
//        getObservable().subscribeWith(new DisposableObserver() {
//            @Override
//            public void onNext(Object o) {
//            }
//
//            @Override
//            public void onError(Throwable e) {
//
//            }
//
//            @Override
//            public void onComplete() {
//
//            }
//        });
    }
}
