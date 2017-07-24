package com.inceptai.wifimonitoringservice.actionlibrary.actions;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.wifimonitoringservice.R;
import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ConnectivityTester;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.NetworkActionLayer;
import com.inceptai.wifimonitoringservice.utils.ServiceLog;
import com.inceptai.wifimonitoringservice.utils.Utils;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import static com.inceptai.wifimonitoringservice.actionlibrary.ActionResult.ActionResultCodes.FAILED_TO_COMPLETE;
import static com.inceptai.wifimonitoringservice.actionlibrary.ActionResult.ActionResultCodes.GENERAL_ERROR;
import static com.inceptai.wifimonitoringservice.actionlibrary.ActionResult.ActionResultCodes.SUCCESS;
import static com.inceptai.wifimonitoringservice.actionlibrary.ActionResult.ActionResultCodes.TIMED_OUT;

/**
 * Created by vivek on 7/5/17.
 */

public class IterateAndRepairWifiNetwork extends FutureAction {
    private static final int MAX_CONNECTIVITY_TRIES = 10;
    private static final long GAP_BETWEEN_TRIES_MS = 1000;
    private static final int MAX_NETWORKS_TO_ITERATE = 2;
    public IterateAndRepairWifiNetwork(Context context,
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
        final FutureAction iterateAndConnectToBestWifiNetwork =
                new IterateAndConnectToBestNetwork(context, executor,
                        scheduledExecutorService, networkActionLayer, actionTimeOutMs,
                        MAX_NETWORKS_TO_ITERATE, MAX_CONNECTIVITY_TRIES, GAP_BETWEEN_TRIES_MS);
        toggleWifiAction.uponSuccessfulCompletion(iterateAndConnectToBestWifiNetwork);
        setFuture(iterateAndConnectToBestWifiNetwork.getFuture());
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
        ActionResult connectResult = null;
        if (result != null && result.getPayload() != null) {
            connectResult = (ActionResult)result.getPayload();
        }
        ActionResult finalRepairResult = processIterateAndConnectResult(connectResult);
        super.setResult(finalRepairResult);
    }

    @Override
    public ListenableFuture<ActionResult> getFuture() {
        return super.getFuture();
    }

    private ActionResult processIterateAndConnectResult(ActionResult iterateAndConnectResult) {
        @ConnectivityTester.WifiConnectivityMode int modeAfterRepair;
        String repairSummary;
        ActionResult resultToReturn;
        WifiInfo wifiInfo = null;
        if (ActionResult.isSuccessful(iterateAndConnectResult)) {
            //noinspection ResourceType
            modeAfterRepair = (int)iterateAndConnectResult.getPayload();
            ServiceLog.v("In iterateAndRepairConnection: modeAfterRepair: " + modeAfterRepair);
            wifiInfo = networkActionLayer.getWifiInfoSync();
            if (ConnectivityTester.isConnected(modeAfterRepair)) {
                //Connected
                if (ConnectivityTester.isOnline(modeAfterRepair)) {
                    repairSummary = Utils.userReadableRepairSummary(true, true, wifiInfo);
                    resultToReturn = new ActionResult(SUCCESS, repairSummary, wifiInfo);
                    ServiceLog.v("In iterateAndRepairConnection: returning online + success ");
                } else {
                    repairSummary = Utils.userReadableRepairSummary(false, true, wifiInfo);
                    resultToReturn = new ActionResult(FAILED_TO_COMPLETE, repairSummary, wifiInfo);
                    ServiceLog.v("In iterateAndRepairConnection: returning offline + failed to complete ");
                }
            } else {
                repairSummary = "Found known networks but unable to connect to a network with Internet. Manually connect to a good network and see if it works.";
                resultToReturn = new ActionResult(FAILED_TO_COMPLETE, repairSummary, null);
                ServiceLog.v("In iterateAndRepairConnection: returning general error " + repairSummary);
            }
        } else if (ActionResult.failedToComplete(iterateAndConnectResult)) {
            repairSummary = "Unable to find good networks to connect. Manually connect to a good network and see if it works.";
            resultToReturn = new ActionResult(FAILED_TO_COMPLETE, repairSummary, null);
            ServiceLog.v("In iterateAndRepairConnection: returning general error " + repairSummary);
        } else if (ActionResult.failedToStart(iterateAndConnectResult)) {
            repairSummary = Utils.userReadableRepairSummary(false, false, null);
            resultToReturn = new ActionResult(iterateAndConnectResult.getStatus(), repairSummary, null);
            ServiceLog.v("In iterateAndRepairConnection: returning: " + repairSummary);
        } else if (iterateAndConnectResult != null) {
            resultToReturn = new ActionResult(iterateAndConnectResult.getStatus(),
                    "Unable to toggle and connect to a good network", null);
            ServiceLog.v("In iterateAndRepairConnection: returning: " + iterateAndConnectResult.getStatus() + " ,  " + iterateAndConnectResult.getStatusString());
        } else {
            //Timed out
            repairSummary = Utils.userReadableRepairSummary(false, false, null);
            resultToReturn = new ActionResult(TIMED_OUT, repairSummary);
            ServiceLog.v("In iterateAndRepairConnection: returning GENERAL error 2");
        }
        return resultToReturn;
    }
}
