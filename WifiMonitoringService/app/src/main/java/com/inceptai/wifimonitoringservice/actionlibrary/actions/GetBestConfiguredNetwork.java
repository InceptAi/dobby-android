package com.inceptai.wifimonitoringservice.actionlibrary.actions;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.wifimonitoringservice.R;
import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.NetworkActionLayer;
import com.inceptai.wifimonitoringservice.actionlibrary.utils.ActionUtils;
import com.inceptai.wifimonitoringservice.utils.ServiceLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
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
    private List<String> offlineRouterIds;

    public GetBestConfiguredNetwork(Context context,
                                    Executor executor,
                                    ScheduledExecutorService scheduledExecutorService,
                                    NetworkActionLayer networkActionLayer,
                                    long actionTimeOutMs,
                                    @Nullable List<String> offlineRouterIds) {
        super(ActionType.GET_BEST_CONFIGURED_NETWORK, context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        this.offlineRouterIds = offlineRouterIds;
    }

    public GetBestConfiguredNetwork(Context context,
                                    Executor executor,
                                    ScheduledExecutorService scheduledExecutorService,
                                    NetworkActionLayer networkActionLayer,
                                    long actionTimeOutMs) {
        super(ActionType.GET_BEST_CONFIGURED_NETWORK, context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        this.offlineRouterIds = new ArrayList<>();
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
                }catch (InterruptedException | ExecutionException | CancellationException e) {
                    e.printStackTrace(System.out);
                    ServiceLog.w("ActionTaker: Exception getting wifi results: " + e.toString());
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
                }catch (InterruptedException | ExecutionException | CancellationException e) {
                    e.printStackTrace(System.out);
                    ServiceLog.w("ActionTaker: Exception getting wifi results: " + e.toString());
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
        List<WifiConfiguration> prunedWifiConfigurationList = new ArrayList<>();

        if (actionResult != null) {
            wifiConfigurationList = (List<WifiConfiguration>) actionResult.getPayload();
        }

        //Get Configured networks can return null so need to handle this.
        if (wifiConfigurationList == null) {
            wifiConfigurationList = new ArrayList<>();
        }

        if (offlineRouterIds != null && offlineRouterIds.size() > 0 && wifiConfigurationList.size() > 0) {
            for (WifiConfiguration wifiConfiguration: wifiConfigurationList) {
                if (!ActionUtils.checkIfWifiConfigurationMatchesListOfRouterIds(wifiConfiguration, offlineRouterIds)) {
                    prunedWifiConfigurationList.add(wifiConfiguration);
                }
            }
            wifiConfigurationList = prunedWifiConfigurationList;
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

    synchronized private void clearResults() {
        scanResultList = null;
        wifiConfigurationList = null;
        bestAvailableWifiConfiguration = null;
    }

    @Override
    public boolean shouldBlockOnOtherActions() {
        return true;
    }

}
