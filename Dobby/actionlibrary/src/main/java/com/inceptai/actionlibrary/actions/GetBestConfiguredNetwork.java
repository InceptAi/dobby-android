package com.inceptai.actionlibrary.actions;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.actionlibrary.ActionResult;
import com.inceptai.actionlibrary.NetworkLayer.NetworkActionLayer;
import com.inceptai.actionlibrary.R;
import com.inceptai.actionlibrary.utils.ActionLog;
import com.inceptai.actionlibrary.utils.ActionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by vivek on 7/5/17.
 */

public class GetBestConfiguredNetwork extends FutureAction {

    private List<ScanResult> scanResultList;
    private List<WifiConfiguration> wifiConfigurationList;
    private WifiConfiguration bestAvailableWifiConfiguration;

    public GetBestConfiguredNetwork(Context context,
                                    Executor executor,
                                    ScheduledExecutorService scheduledExecutorService,
                                    NetworkActionLayer networkActionLayer,
                                    long actionTimeOutMs) {
        super(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
    }

    @Override
    public void post() {
        final FutureAction getConfiguredNetworkListAction = new GetConfiguredNetworks(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        final FutureAction getNearbyNetworksAction = new GetNearbyWifiNetworks(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);

        getConfiguredNetworkListAction.getFuture().addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    setWifiConfigurationList(getConfiguredNetworkListAction.getFuture().get());
                    computeBestConfiguredNetwork();
                }catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace(System.out);
                    ActionLog.w("ActionTaker: Exception getting wifi results: " + e.toString());
                    setResult(new ActionResult(ActionResult.ActionResultCodes.EXCEPTION, e.toString()));
                }
            }
        }, executor);

        getNearbyNetworksAction.getFuture().addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    setScanResults(getNearbyNetworksAction.getFuture().get());
                    computeBestConfiguredNetwork();
                }catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace(System.out);
                    ActionLog.w("ActionTaker: Exception getting wifi results: " + e.toString());
                    setResult(new ActionResult(ActionResult.ActionResultCodes.EXCEPTION, e.toString()));
                }
            }
        }, executor);

        getConfiguredNetworkListAction.post();
        getNearbyNetworksAction.post();
    }

    @Override
    public String getName() {
        return context.getString(R.string.get_best_wifi_network_to_connect);
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

    synchronized private void setWifiConfigurationList(ActionResult actionResult) {
        if (actionResult != null) {
            wifiConfigurationList = (List<WifiConfiguration>) actionResult.getPayload();
        } else {
            wifiConfigurationList = new ArrayList<>();
        }
    }

    synchronized private void setScanResults(ActionResult actionResult) {
        if (actionResult != null) {
            scanResultList = (List<ScanResult>) actionResult.getPayload();
        } else {
            scanResultList = new ArrayList<>();
        }
    }

    synchronized private void computeBestConfiguredNetwork() {
        if (scanResultList != null && wifiConfigurationList != null) {
            //We have both, compute best wifi now
            bestAvailableWifiConfiguration = ActionUtils.findBestConfiguredNetworkFromScanResult(wifiConfigurationList, scanResultList);
            setResult((new ActionResult(ActionResult.ActionResultCodes.SUCCESS, bestAvailableWifiConfiguration)));
            clearResults();
        }
    }

    private void clearResults() {
        scanResultList = null;
        wifiConfigurationList = null;
        bestAvailableWifiConfiguration = null;
    }

}
