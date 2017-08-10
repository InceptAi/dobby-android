package com.inceptai.wifimonitoringservice.actionlibrary.actions;

import android.content.Context;
import android.net.DhcpInfo;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.wifimonitoringservice.R;
import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.NetworkActionLayer;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ping.IPLayerInfo;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ping.PingStats;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by vivek on 7/5/17.
 */

public class PerformPingTestForDHCPInfo extends FutureAction {
    private IPLayerInfo ipLayerInfo;
    private DhcpInfo dhcpInfo;
    public PerformPingTestForDHCPInfo(Context context,
                                      Executor executor,
                                      ScheduledExecutorService scheduledExecutorService,
                                      NetworkActionLayer networkActionLayer,
                                      long actionTimeOutMs) {
        super(ActionType.PERFORM_PING_FOR_DHCP_INFO, context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
    }

    @Override
    public void post() {
        final FutureAction getDHCPInfoAction = new GetDHCPInfo(context,
                executor, scheduledExecutorService, networkActionLayer,
                actionTimeOutMs);
        getDHCPInfoAction.getFuture().addListener(new Runnable() {
            @Override
            public void run() {
                ActionResult dhcpInfoResult;
                try {
                    dhcpInfoResult = getDHCPInfoAction.getFuture().get();
                    if (ActionResult.isSuccessful(dhcpInfoResult)) {
                        dhcpInfo = (DhcpInfo)dhcpInfoResult.getPayload();
                        if (dhcpInfo != null) {
                            ipLayerInfo = new IPLayerInfo(dhcpInfo);
                            final List<String> ipAddressList = ipLayerInfo.getIPAddressList();
                            final FutureAction performPingTestAction = new PerformPingTest(context,
                                    executor, scheduledExecutorService,
                                    networkActionLayer, ipAddressList,
                                    actionTimeOutMs);
                            setFuture(performPingTestAction.getFuture());
                            performPingTestAction.post();
                        } else {
                            setResult(new ActionResult(ActionResult.ActionResultCodes.FAILED_TO_START, "DHCP Info is null"));
                        }
                    }
                } catch (InterruptedException | ExecutionException | CancellationException e) {
                    setResult(new ActionResult(ActionResult.ActionResultCodes.EXCEPTION, e.toString()));
                }
            }
        }, executor);
        getDHCPInfoAction.post();
    }

    @Override
    public String getName() {
        return context.getString(R.string.perform_ping_test_for_dhcp_info);
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
        ActionResult pingResult = null;
        if (ActionResult.isSuccessful(result)) {
            pingResult = (ActionResult)result.getPayload();
        }
        ActionResult annotatedPingResult = postProcessPingResult(pingResult);
        super.setResult(annotatedPingResult);
    }

    @Override
    public ListenableFuture<ActionResult> getFuture() {
        return super.getFuture();
    }

    @Override
    public boolean shouldBlockOnOtherActions() {
        return false;
    }

    private ActionResult postProcessPingResult(ActionResult pingResult) {
        if (pingResult == null) {
            return new ActionResult(ActionResult.ActionResultCodes.EXCEPTION);
        }
        ActionResult resultToReturn;
        if (ActionResult.isSuccessful(pingResult)) {
            HashMap<String, PingStats> pingStatsHashMap = (HashMap<String, PingStats>)pingResult.getPayload();
            if (pingStatsHashMap != null) {
                for (Map.Entry<String, PingStats> entry : pingStatsHashMap.entrySet()) {
                    String ipAddress = entry.getKey();
                    PingStats pingStats = entry.getValue();
                    if (ipLayerInfo != null) {
                        pingStats.ipAddressType = ipLayerInfo.getTypeForAddress(ipAddress);
                    } else {
                        pingStats.ipAddressType = PingStats.IPAddressType.UNKNOWN;
                    }
                }
                resultToReturn = new ActionResult(ActionResult.ActionResultCodes.SUCCESS, pingStatsHashMap);
            } else {
                resultToReturn = new ActionResult(pingResult.getStatus(), pingResult.getStatusString());
            }
        } else {
            resultToReturn = new ActionResult(pingResult.getStatus());
        }
        return resultToReturn;
    }


}
