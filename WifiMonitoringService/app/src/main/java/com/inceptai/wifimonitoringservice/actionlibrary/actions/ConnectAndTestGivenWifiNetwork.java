package com.inceptai.wifimonitoringservice.actionlibrary.actions;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.wifimonitoringservice.R;
import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.NetworkActionLayer;
import com.inceptai.wifimonitoringservice.utils.ServiceLog;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by vivek on 7/5/17.
 */

public class ConnectAndTestGivenWifiNetwork extends FutureAction {
    private int networkId;
    private long gapBetweenChecksMs;
    private int maxConnectivityChecks;

    public ConnectAndTestGivenWifiNetwork(Context context,
                                          Executor executor,
                                          ScheduledExecutorService scheduledExecutorService,
                                          NetworkActionLayer networkActionLayer,
                                          long actionTimeOutMs,
                                          int networkId,
                                          int maxConnectivityChecks,
                                          long gapBetweenChecksMs) {
        super(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        this.networkId = networkId;
        this.maxConnectivityChecks = maxConnectivityChecks;
        this.gapBetweenChecksMs = gapBetweenChecksMs;
    }

    @Override
    public void post() {
        final FutureAction connectToWifiAction = new ConnectWithGivenWifiNetwork(context,
                executor, scheduledExecutorService, networkActionLayer,
                actionTimeOutMs, networkId);
        final FutureAction performConnectivityCheckAction = new PerformConnectivityTest(context,
                executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs,
                maxConnectivityChecks, gapBetweenChecksMs);
        connectToWifiAction.uponSuccessfulCompletion(performConnectivityCheckAction);
        performConnectivityCheckAction.getFuture().addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    setResult(performConnectivityCheckAction.getFuture().get());
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace(System.out);
                    ServiceLog.w("ActionTaker: Exception getting wifi results: " + e.toString());
                    setResult(new ActionResult(ActionResult.ActionResultCodes.EXCEPTION, e.toString()));
                }
            }
        }, executor);
        connectToWifiAction.post();
    }

    @Override
    public String getName() {
        return context.getString(R.string.connect_and_test_given_wifi);
    }

    @Override
    public void uponCompletion(FutureAction operation) {
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
