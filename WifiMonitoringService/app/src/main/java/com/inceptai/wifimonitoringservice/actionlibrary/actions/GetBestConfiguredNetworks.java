package com.inceptai.wifimonitoringservice.actionlibrary.actions;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.wifimonitoringservice.R;
import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.NetworkActionLayer;
import com.inceptai.wifimonitoringservice.actionlibrary.utils.ActionLog;
import com.inceptai.wifimonitoringservice.actionlibrary.utils.ActionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by vivek on 7/5/17.
 */

public class GetBestConfiguredNetworks extends FutureAction {

    private List<ScanResult> scanResultList;
    private List<WifiConfiguration> wifiConfigurationList;
    private List<WifiConfiguration> bestAvailableWifiConfigurations;
    private int numberOfNetworksToReturn;

    public GetBestConfiguredNetworks(Context context,
                                     Executor executor,
                                     ScheduledExecutorService scheduledExecutorService,
                                     NetworkActionLayer networkActionLayer,
                                     long actionTimeOutMs,
                                     int numberOfNetworksToReturn) {
        super(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        this.numberOfNetworksToReturn = numberOfNetworksToReturn;
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
                    computeBestConfiguredNetworks();
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
                    computeBestConfiguredNetworks();
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

    synchronized private void computeBestConfiguredNetworks() {
        if (scanResultList != null && wifiConfigurationList != null) {
            //We have both, compute best wifi now
            bestAvailableWifiConfigurations = new ArrayList<>();
            for (int i=0; i < numberOfNetworksToReturn && wifiConfigurationList.size() > 0; i++) {
                WifiConfiguration nextBestNetwork =  ActionUtils.findBestConfiguredNetworkFromScanResult(wifiConfigurationList, scanResultList);
                if (nextBestNetwork == null) {
                    break;
                }
                bestAvailableWifiConfigurations.add(nextBestNetwork);
                wifiConfigurationList.remove(nextBestNetwork);
            }
            setResult((new ActionResult(ActionResult.ActionResultCodes.SUCCESS, bestAvailableWifiConfigurations)));
            clearResults();
        }
    }

    private void clearResults() {
        scanResultList = null;
        wifiConfigurationList = null;
        bestAvailableWifiConfigurations = null;
    }

}