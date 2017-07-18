package com.inceptai.wifimonitoringservice.actionlibrary.actions;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.wifimonitoringservice.R;
import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.NetworkActionLayer;
import com.inceptai.wifimonitoringservice.actionlibrary.utils.ActionLog;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by vivek on 7/5/17.
 */

public class RepairWifiNetwork extends FutureAction {

    public RepairWifiNetwork(Context context,
                             Executor executor,
                             ScheduledExecutorService scheduledExecutorService,
                             NetworkActionLayer networkActionLayer,
                             long actionTimeOutMs) {
        super(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
    }

    @Override
    public void post() {
        final FutureAction toggleWifiAction = new ToggleWifi(context, executor,
                scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        final FutureAction connectToBestWifiNetwork =
                new ConnectToBestConfiguredNetworkIfAvailable(context, executor,
                        scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        final FutureAction connectivityAction = new PerformConnectivityTest(context, executor,
                scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        toggleWifiAction.uponCompletion(connectToBestWifiNetwork);
        connectToBestWifiNetwork.uponCompletion(connectivityAction);
        //setFuture(connectivityAction.getFuture());
        connectivityAction.getFuture().addListener(new Runnable() {
            @Override
            public void run() {
                ActionResult actionResult = null;
                try {
                    actionResult = connectivityAction.getFuture().get();
                    setResult(actionResult);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace(System.out);
                    ActionLog.w("ActionTaker: Exception getting wifi results: " + e.toString());
                    setResult(new ActionResult(ActionResult.ActionResultCodes.EXCEPTION, e.toString()));
                }
            }
        }, executor);
        toggleWifiAction.post();
    }

    @Override
    public String getName() {
        return context.getString(R.string.repair_wifi_network);
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
