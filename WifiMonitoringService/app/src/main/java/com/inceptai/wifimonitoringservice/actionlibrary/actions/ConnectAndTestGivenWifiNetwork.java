package com.inceptai.wifimonitoringservice.actionlibrary.actions;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.wifimonitoringservice.R;
import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.NetworkActionLayer;
import com.inceptai.wifimonitoringservice.utils.ServiceLog;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by vivek on 7/5/17.
 */

public class ConnectAndTestGivenWifiNetwork extends FutureAction {
    private static final int DEFAULT_MAX_NETWORKS_TO_ITERATE = 4;
    private static final int DEFAULT_MAX_CONNECTIVITY_CHECKS = 10;
    private static final int DEFAULT_GAP_BETWEEN_CHECKS_MS = 300;
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
        super(ActionType.CONNECT_AND_TEST_GIVEN_WIFI_NETWORK, context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        this.networkId = networkId;
        this.maxConnectivityChecks = maxConnectivityChecks;
        this.gapBetweenChecksMs = gapBetweenChecksMs;
    }

    public ConnectAndTestGivenWifiNetwork(Context context,
                                          Executor executor,
                                          ScheduledExecutorService scheduledExecutorService,
                                          NetworkActionLayer networkActionLayer,
                                          long actionTimeOutMs,
                                          int networkId) {
        super(ActionType.CONNECT_AND_TEST_GIVEN_WIFI_NETWORK, context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        this.networkId = networkId;
        this.maxConnectivityChecks = DEFAULT_MAX_CONNECTIVITY_CHECKS;
        this.gapBetweenChecksMs = DEFAULT_GAP_BETWEEN_CHECKS_MS;
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
                } catch (InterruptedException | ExecutionException | CancellationException e) {
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

    @Override
    public boolean shouldBlockOnOtherActions() {
        return true;
    }
}
