package com.inceptai.actionlibrary.actions;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.actionlibrary.ActionResult;
import com.inceptai.actionlibrary.NetworkLayer.NetworkActionLayer;
import com.inceptai.actionlibrary.R;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by vivek on 7/5/17.
 */

public class PerformConnectivityTest extends FutureAction {
    private final boolean PERFORM_TEST_ONLY_ON_ACTIVE_NETWORK = false;
    public PerformConnectivityTest(Context context,
                                   Executor executor,
                                   ScheduledExecutorService scheduledExecutorService,
                                   NetworkActionLayer networkActionLayer,
                                   long actionTimeOutMs) {
        super(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
    }

    @Override
    public void post() {
        setFuture(networkActionLayer.connectivityTest(PERFORM_TEST_ONLY_ON_ACTIVE_NETWORK));
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
