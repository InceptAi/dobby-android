package com.inceptai.wifimonitoringservice.actionlibrary.actions;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.wifimonitoringservice.R;
import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.NetworkActionLayer;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.speedtest.SpeedTestConfig;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.wifi.WifiNetworkOverview;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by vivek on 7/5/17.
 */

public class GetOverallNetworkInfo extends FutureAction {
    public GetOverallNetworkInfo(Context context,
                                 Executor executor,
                                 ScheduledExecutorService scheduledExecutorService,
                                 NetworkActionLayer networkActionLayer,
                                 long actionTimeOutMs) {
        super(ActionType.GET_OVERALL_NETWORK_INFO, context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
    }

    @Override
    public void post() {
        setFuture(networkActionLayer.getSpeedTestConfig());
    }

    @Override
    public String getName() {
        return context.getString(R.string.get_overall_network_info);
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
        WifiNetworkOverview wifiNetworkOverview = new WifiNetworkOverview();
        if (ActionResult.isSuccessful(result)) {
            SpeedTestConfig speedTestConfig = (SpeedTestConfig)result.getPayload();
            if (speedTestConfig != null && speedTestConfig.clientConfig != null) {
                wifiNetworkOverview.setExternalIP(speedTestConfig.clientConfig.isp);
                wifiNetworkOverview.setExternalIP(speedTestConfig.clientConfig.ip);
            }
        }
        WifiInfo wifiInfo = networkActionLayer.getWifiInfoSync();
        if (wifiInfo != null) {
            wifiNetworkOverview.setSsid(wifiInfo.getSSID());
            wifiNetworkOverview.setSignal(wifiInfo.getRssi());
        }
//        @ActionResult.ActionResultCodes int returnCode;
//        if (result != null) {
//            returnCode = result.getStatus();
//        } else {
//            returnCode = ActionResult.ActionResultCodes.EXCEPTION;
//        }
        super.setResult(new ActionResult(ActionResult.ActionResultCodes.SUCCESS, wifiNetworkOverview));
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
