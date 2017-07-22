package com.inceptai.wifimonitoringservice.actionlibrary.actions;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.wifimonitoringservice.R;
import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.NetworkActionLayer;
import com.inceptai.wifimonitoringservice.utils.ServiceLog;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by vivek on 7/5/17.
 */

public class ConnectToBestConfiguredNetwork extends FutureAction {
    private WifiConfiguration bestAvailableWifiConfiguration;
    private List<String> offlineRouterIDs;

    public ConnectToBestConfiguredNetwork(Context context,
                                          Executor executor,
                                          ScheduledExecutorService scheduledExecutorService,
                                          NetworkActionLayer networkActionLayer,
                                          long actionTimeOutMs,
                                          List<String> offlineRouterIDs) {
        super(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        this.offlineRouterIDs = offlineRouterIDs;
    }

    @Override
    public void post() {
        final FutureAction getBestConfiguredNetworkAction = new GetBestConfiguredNetwork(context,
                executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs, offlineRouterIDs);
        getBestConfiguredNetworkAction.getFuture().addListener(new Runnable() {
            @Override
            public void run() {
                ActionResult actionResult = null;
                try {
                    actionResult = getBestConfiguredNetworkAction.getFuture().get();
                    if (actionResult != null) {
                        bestAvailableWifiConfiguration = (WifiConfiguration)actionResult.getPayload();
                    }
                    if (bestAvailableWifiConfiguration != null) {
                        final FutureAction connectToBestNetworkAction = new ConnectWithGivenWifiNetwork(context,
                                executor, scheduledExecutorService, networkActionLayer,
                                actionTimeOutMs, bestAvailableWifiConfiguration.networkId);
                        setFuture(connectToBestNetworkAction.getFuture());
                        connectToBestNetworkAction.post();
                    } else {
                        setResult(new ActionResult(ActionResult.ActionResultCodes.GENERAL_ERROR, "No configured network found in scan"));
                    }
                }catch (InterruptedException | ExecutionException | CancellationException e) {
                    e.printStackTrace(System.out);
                    ServiceLog.w("ActionTaker: Exception getting wifi results: " + e.toString());
                    setResult(new ActionResult(ActionResult.ActionResultCodes.EXCEPTION, e.toString()));
                }
            }
        }, executor);
        getBestConfiguredNetworkAction.post();
    }

    @Override
    public String getName() {
        return context.getString(R.string.connect_to_best_wifi);
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
