package com.inceptai.wifimonitoringservice.actionlibrary.actions;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.wifimonitoringservice.R;
import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.NetworkActionLayer;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by vivek on 7/5/17.
 */

public class PerformPingTest extends FutureAction {
    private static final long DEFAULT_PING_TIMEOUT_MS = 5000;
    private static final int DEFAULT_NUMBER_OF_PINGS = 5;

    private int numPings;
    private long pingTimeOutMs;
    private List<String> ipAddressList;
    public PerformPingTest(Context context,
                           Executor executor,
                           ScheduledExecutorService scheduledExecutorService,
                           NetworkActionLayer networkActionLayer,
                           List<String> ipAddressList,
                           long actionTimeOutMs) {
        super(ActionType.PERFORM_PING_TEST, context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        numPings = DEFAULT_NUMBER_OF_PINGS;
        pingTimeOutMs = DEFAULT_PING_TIMEOUT_MS;
        this.ipAddressList = ipAddressList;
    }

    public PerformPingTest(Context context,
                           Executor executor,
                           ScheduledExecutorService scheduledExecutorService,
                           NetworkActionLayer networkActionLayer,
                           long actionTimeOutMs,
                           int numPings,
                           long pingTimeOutMs,
                           List<String> ipAddressList) {
        super(ActionType.PERFORM_PING_TEST, context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        this.numPings = numPings;
        this.pingTimeOutMs = pingTimeOutMs;
        this.ipAddressList = ipAddressList;
    }


    @Override
    public void post() {
        setFuture(networkActionLayer.pingTest(ipAddressList, numPings, pingTimeOutMs));
    }

    @Override
    public String getName() {
        return context.getString(R.string.perform_ping_test);
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
        return false;
    }
}
