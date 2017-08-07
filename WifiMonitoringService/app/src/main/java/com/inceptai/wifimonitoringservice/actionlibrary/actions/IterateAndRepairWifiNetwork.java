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
        super(ActionType.ITERATE_AND_REPAIR_WIFI_NETWORK, context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
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
                    resultToReturn = new RepairResult(SUCCESS, repairSummary, wifiInfo, RepairResult.SUCCESSFUL);
                    ServiceLog.v("In iterateAndRepairConnection: returning online + success ");
                } else {
                    repairSummary = Utils.userReadableRepairSummary(false, true, wifiInfo);
                    resultToReturn = new RepairResult(FAILED_TO_COMPLETE, repairSummary, wifiInfo, RepairResult.NO_NETWORK_WITH_ONLINE_CONNECTIVITY_MODE);
                    ServiceLog.v("In iterateAndRepairConnection: returning offline + failed to complete ");
                }
            } else {
                repairSummary = "Found known networks but unable to connect to a network with Internet. Manually connect to a good network and see if it works.";
                resultToReturn = new RepairResult(FAILED_TO_COMPLETE, repairSummary, null, RepairResult.UNABLE_TO_CONNECT_TO_ANY_NETWORK);
                ServiceLog.v("In iterateAndRepairConnection: returning general error " + repairSummary);
            }
        } else if (ActionResult.failedToComplete(iterateAndConnectResult)) {
            repairSummary = "Unable to find good networks to connect. Manually connect to a good network and see if it works.";
            resultToReturn = new RepairResult(FAILED_TO_COMPLETE, repairSummary, null, RepairResult.NO_NEARBY_CONFIGURED_NETWORKS);
            ServiceLog.v("In iterateAndRepairConnection: returning general error " + repairSummary);
        } else if (ActionResult.failedToStart(iterateAndConnectResult)) {
            repairSummary = Utils.userReadableRepairSummary(false, false, null);
            resultToReturn = new RepairResult(iterateAndConnectResult.getStatus(), repairSummary, null, RepairResult.UNABLE_TO_TOGGLE_WIFI);
            ServiceLog.v("In iterateAndRepairConnection: returning: " + repairSummary);
        } else if (iterateAndConnectResult != null) {
            resultToReturn = new RepairResult(iterateAndConnectResult.getStatus(),
                    "Unable to toggle and connect to a good network", null, RepairResult.UNKNOWN);
            ServiceLog.v("In iterateAndRepairConnection: returning: " + iterateAndConnectResult.getStatus() + " ,  " + iterateAndConnectResult.getStatusString());
        } else {
            //Timed out
            repairSummary = Utils.userReadableRepairSummary(false, false, null);
            resultToReturn = new RepairResult(TIMED_OUT, repairSummary, null, RepairResult.TIMED_OUT);
            ServiceLog.v("In iterateAndRepairConnection: returning GENERAL error 2");
        }
        return resultToReturn;
    }

    public class RepairResult extends ActionResult {
        //private static final String NO_CONFIGURED_NETWORKS = "NO_NETWORKS_FOUND";
        //private static final String NO_NEARBY_NETWORKS = "NO_NEARBY_NETWORKS_FOUND";
        public static final String NO_NEARBY_CONFIGURED_NETWORKS = "NO_NEARBY_CONFIGURED_NETWORKS_FOUND";
        public static final String UNABLE_TO_CONNECT_TO_ANY_NETWORK = "UNABLE_TO_CONNECT_TO_ANY_NETWORK";
        public static final String NO_NETWORK_WITH_ONLINE_CONNECTIVITY_MODE = "NO_NETWORK_WITH_ONLINE_CONNECTIVITY_MODE";
        public static final String UNABLE_TO_TOGGLE_WIFI = "UNABLE_TO_TOGGLE_WIFI";
        public static final String SUCCESSFUL = "SUCCESSFUL";
        //private static final String FAILED_TO_COMPLETE = "FAILED_TO_COMPLETE";
        public static final String TIMED_OUT = "TIMED_OUT";
        public static final String UNKNOWN = "UNKNOWN";

        private String repairFailureReason = Utils.EMPTY_STRING;

        public String getRepairFailureReason() {
            return repairFailureReason;
        }

        RepairResult(@ActionResultCodes int status, String statusString, WifiInfo wifiInfo, String repairFailureReason) {
            super(status, statusString, wifiInfo);
            this.repairFailureReason = repairFailureReason;
        }
    }
}
