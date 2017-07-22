package com.inceptai.wifimonitoringservice.actionlibrary.actions;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.wifimonitoringservice.R;
import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ConnectivityTester;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.NetworkActionLayer;
import com.inceptai.wifimonitoringservice.utils.ServiceLog;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import static com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ConnectivityTester.WifiConnectivityMode.UNKNOWN;

/**
 * Created by vivek on 7/5/17.
 */

public class IterateAndConnectToBestNetwork extends FutureAction {
    private int maxNetworksToIterate;
    private List<WifiConfiguration> wifiConfigurationListToTry;
    private int maxConnectivityChecks;
    private long gapBetweenChecksMs;
    private int currentWifiConfigurationIndex;
    private ActionResult connectivityResultOfLastNetwork;

    public IterateAndConnectToBestNetwork(Context context,
                                          Executor executor,
                                          ScheduledExecutorService scheduledExecutorService,
                                          NetworkActionLayer networkActionLayer,
                                          long actionTimeOutMs,
                                          int maxNetworksToIterate,
                                          int maxConnectivityChecks,
                                          long gapBetweenChecksMs) {
        super(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        this.maxNetworksToIterate = maxNetworksToIterate;
        this.maxConnectivityChecks = maxConnectivityChecks;
        this.gapBetweenChecksMs = gapBetweenChecksMs;
        currentWifiConfigurationIndex = 0;
    }

    public IterateAndConnectToBestNetwork(Context context,
                                          Executor executor,
                                          ScheduledExecutorService scheduledExecutorService,
                                          NetworkActionLayer networkActionLayer,
                                          long actionTimeOutMs) {
        super(context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        this.maxNetworksToIterate = 2;
        this.maxConnectivityChecks = 10;
        this.gapBetweenChecksMs = 300;
        currentWifiConfigurationIndex = 0;
    }


    @Override
    public void post() {
        final FutureAction getBestConfiguredNetworksListAction = new GetBestConfiguredNetworks(context, executor,
                scheduledExecutorService, networkActionLayer, actionTimeOutMs, maxNetworksToIterate);
        getBestConfiguredNetworksListAction.getFuture().addListener(new Runnable() {
            @Override
            public void run() {
                ActionResult actionResult = null;
                List<WifiConfiguration> wifiConfigurationList;
                try {
                    actionResult = getBestConfiguredNetworksListAction.getFuture().get();
                    if (ActionResult.isSuccessful(actionResult)) {
                        wifiConfigurationListToTry = (List<WifiConfiguration>) actionResult.getPayload();
                        if (wifiConfigurationListToTry.isEmpty()) {
                            setResult(new ActionResult(ActionResult.ActionResultCodes.FAILED_TO_COMPLETE, "No networks with Internet available"));
                        } else {
                            connectAndTestGivenNetwork(getNextNetworkToTry());
                        }
                    } else {
                        setResult(new ActionResult(ActionResult.ActionResultCodes.FAILED_TO_COMPLETE, "Unable to get network list"));
                    }
                } catch (InterruptedException | ExecutionException | CancellationException e) {
                    e.printStackTrace(System.out);
                    ServiceLog.w("ActionTaker: Exception getting wifi results: " + e.toString());
                    setResult(new ActionResult(ActionResult.ActionResultCodes.EXCEPTION, e.toString()));
                }
            }
        }, executor);
        getBestConfiguredNetworksListAction.post();
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

    private WifiConfiguration getNextNetworkToTry() {
        if (wifiConfigurationListToTry == null || wifiConfigurationListToTry.size() <= currentWifiConfigurationIndex) {
            return null;
        }
        return wifiConfigurationListToTry.get(currentWifiConfigurationIndex++);
    }

    private boolean isLastNetworkInList() {
        return  (wifiConfigurationListToTry != null && wifiConfigurationListToTry.size() == currentWifiConfigurationIndex);
    }

    private void connectAndTestGivenNetwork(WifiConfiguration wifiConfiguration) {

        if (wifiConfiguration == null) {
            return;
        }

        if (wifiConfiguration.SSID != null) {
            ServiceLog.v("Trying to connect and test network: " + wifiConfiguration.SSID);
        }

        final FutureAction connectAndTestGivenNetworkAction = new ConnectAndTestGivenWifiNetwork(context,
                executor, scheduledExecutorService, networkActionLayer,
                actionTimeOutMs, wifiConfiguration.networkId, maxConnectivityChecks, gapBetweenChecksMs);
        connectAndTestGivenNetworkAction.getFuture().addListener(new Runnable() {
            @Override
            public void run() {
                @ConnectivityTester.WifiConnectivityMode int modeAfterConnection = UNKNOWN;
                ActionResult actionResult = null;
                try {
                    actionResult = connectAndTestGivenNetworkAction.getFuture().get();
                    if (actionResult != null && actionResult.getPayload() != null) {
                        @ConnectivityTester.WifiConnectivityMode int modeInPayload = (int)actionResult.getPayload();
                        modeAfterConnection = modeInPayload;
                    }
                    if ((ActionResult.isSuccessful(actionResult) && ConnectivityTester.isOnline(modeAfterConnection)) || isLastNetworkInList()) {
                        setResult(actionResult);
                    } else {
                        connectAndTestGivenNetwork(getNextNetworkToTry());
                    }
                } catch (InterruptedException | ExecutionException | CancellationException e) {
                    e.printStackTrace(System.out);
                    ServiceLog.w("ActionTaker: Exception getting wifi results: " + e.toString());
                    connectAndTestGivenNetwork(getNextNetworkToTry());
                }
            }
        }, executor);
        connectAndTestGivenNetworkAction.post();
    }
}
