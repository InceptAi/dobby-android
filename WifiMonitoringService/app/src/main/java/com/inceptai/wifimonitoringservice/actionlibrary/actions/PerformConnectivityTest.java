package com.inceptai.wifimonitoringservice.actionlibrary.actions;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.wifimonitoringservice.R;
import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.NetworkActionLayer;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by vivek on 7/5/17.
 */

public class PerformConnectivityTest extends FutureAction {
    private final boolean PERFORM_TEST_ONLY_ON_ACTIVE_NETWORK = false;
    private int maxTests;
    private long gapBetweenChecksMs;
    public PerformConnectivityTest(Context context,
                                   Executor executor,
                                   ScheduledExecutorService scheduledExecutorService,
                                   NetworkActionLayer networkActionLayer,
                                   long actionTimeOutMs) {
        super(ActionType.PERFORM_CONNECTIVITY_TEST, context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        maxTests = 10;
        gapBetweenChecksMs = 300;
    }

    public PerformConnectivityTest(Context context,
                                   Executor executor,
                                   ScheduledExecutorService scheduledExecutorService,
                                   NetworkActionLayer networkActionLayer,
                                   long actionTimeOutMs,
                                   int maxTests,
                                   long gapBetweenChecksMs) {
        super(ActionType.PERFORM_CONNECTIVITY_TEST, context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        this.maxTests = maxTests;
        this.gapBetweenChecksMs = gapBetweenChecksMs;
    }


    @Override
    public void post() {
        setFuture(networkActionLayer.connectivityTest(PERFORM_TEST_ONLY_ON_ACTIVE_NETWORK, maxTests, gapBetweenChecksMs));
    }

    @Override
    public String getName() {
        return context.getString(R.string.perform_connectivity_test);
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
