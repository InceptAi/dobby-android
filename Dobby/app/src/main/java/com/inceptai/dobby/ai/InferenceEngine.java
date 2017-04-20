package com.inceptai.dobby.ai;

import android.support.annotation.IntDef;
import android.util.Log;

import com.inceptai.dobby.connectivity.ConnectivityAnalyzer;
import com.inceptai.dobby.model.DobbyWifiInfo;
import com.inceptai.dobby.model.IPLayerInfo;
import com.inceptai.dobby.model.PingStats;
import com.inceptai.dobby.speedtest.BandwithTestCodes;
import com.inceptai.dobby.ui.GraphData;
import com.inceptai.dobby.utils.Utils;
import com.inceptai.dobby.wifi.WifiState;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * Inference engine consumes actions from the NLU engine (ApiAi or local) and creates Actions.
 * It also consumes network metrics as a result of Actions such as bandwidth tests, etc, to further
 * decide the course of action.
 */

public class InferenceEngine {
    private static final String CANNED_RESPONSE = "We are working on it.";

    private static final int STATE_BANDWIDTH_TEST_NONE = 0;
    private static final int STATE_BANDWIDTH_TEST_REQUESTED = 1;
    private static final int STATE_BANDWIDTH_TEST_RUNNING = 2;
    private static final int STATE_BANDWIDTH_TEST_SUCCESS = 3;
    private static final int STATE_BANDWIDTH_TEST_FAILED = 4;
    private static final int STATE_BANDWIDTH_TEST_CANCELLED = 4;

    private Action previousAction = Action.ACTION_NONE;
    private int bandwidthTestState; /* state of the bandwidth test */
    private ScheduledExecutorService scheduledExecutorService;
    private ScheduledFuture<?> bandwidthCheckFuture;
    private ActionListener actionListener;
    private long lastBandwidthUpdateTimestampMs = 0;
    private MetricsDb metricsDb;
    private PossibleConditions currentConditions = PossibleConditions.NOOP_CONDITION;

    public interface ActionListener {
        void takeAction(Action action);
    }

    @IntDef({
            Goal.GOAL_DIAGNOSE_SLOW_INTERNET,
            Goal.GOAL_DIAGNOSE_WIFI_ISSUES,
            Goal.GOAL_DIAGNOSE_JITTERY_STREAMING})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Goal {
        int GOAL_DIAGNOSE_SLOW_INTERNET = 1;
        int GOAL_DIAGNOSE_WIFI_ISSUES = 2;
        int GOAL_DIAGNOSE_JITTERY_STREAMING = 3;
    }

    InferenceEngine(ScheduledExecutorService scheduledExecutorService, ActionListener actionListener) {
        bandwidthTestState = STATE_BANDWIDTH_TEST_NONE;
        this.scheduledExecutorService = scheduledExecutorService;
        this.actionListener = actionListener;
        metricsDb = new MetricsDb();
    }

    public Action addGoal(@Goal int goal) {
        return new Action(Utils.EMPTY_STRING, Action.ActionType.ACTION_TYPE_BANDWIDTH_PING_WIFI_TESTS);
    }

    private String testModeToString(@BandwithTestCodes.TestMode int testMode) {
        String testModeString = "UNKNOWN";
        if (testMode == BandwithTestCodes.TestMode.DOWNLOAD) {
            testModeString = "DOWNLOAD";
        } else if (testMode == BandwithTestCodes.TestMode.UPLOAD) {
            testModeString = "UPLOAD";
        }
        return testModeString;
    }

    public void notifyWifiState(WifiState wifiState, @WifiState.WifiLinkMode int wifiLinkMode,
                                @ConnectivityAnalyzer.WifiConnectivityMode int wifiConnectivityMode) {
        HashMap<Integer, WifiState.ChannelInfo> channelMap = wifiState.getChannelInfoMap();
        DobbyWifiInfo wifiInfo = wifiState.getLinkInfo();
        DataInterpreter.WifiGrade wifiGrade = DataInterpreter.interpret(channelMap, wifiInfo, wifiLinkMode, wifiConnectivityMode);
        PossibleConditions conditions = InferenceMap.getPossibleConditionsFor(wifiGrade);
    }

    public void notifyPingStats(HashMap<String, PingStats> pingStatsMap, IPLayerInfo ipLayerInfo) {
    }

    // Bandwidth test notifications:

    public void notifyBandwidthTestStart(@BandwithTestCodes.TestMode int testMode) {
        if (testMode == BandwithTestCodes.TestMode.UPLOAD) {
            metricsDb.clearUploadMbps();
        }
    }

    public void notifyBandwidthTestProgress(@BandwithTestCodes.TestMode int testMode, double bandwidth) {
        long currentTs = System.currentTimeMillis();
        if ((currentTs - lastBandwidthUpdateTimestampMs) > 500L) {
            sendResponseOnlyAction(testModeToString(testMode) + " Current Bandwidth: " + String.format("%.2f", bandwidth / 1000000) + " Mbps");
            lastBandwidthUpdateTimestampMs = currentTs;
        }
    }

    public void notifyBandwidthTestResult(@BandwithTestCodes.TestMode int testMode,
                                          double bandwidth) {
        sendResponseOnlyAction(testModeToString(testMode) + " Overall Bandwidth = " + String.format("%.2f", bandwidth / 1000000) + " Mbps");
        lastBandwidthUpdateTimestampMs = 0;
        if (testMode == BandwithTestCodes.TestMode.UPLOAD) {
            metricsDb.reportUploadMbps(bandwidth * 1.0e-6);
        } else if (testMode == BandwithTestCodes.TestMode.DOWNLOAD) {
            metricsDb.reportDownloadMbps(bandwidth * 1.0e-6);
        }
        if (metricsDb.hasValidDownload() && metricsDb.hasValidUpload()) {
            DataInterpreter.BandwidthGrade bandwidthGrade = DataInterpreter.interpret(metricsDb.getDownloadMbps(), metricsDb.getUploadMbps());
            PossibleConditions conditions = InferenceMap.getPossibleConditionsFor(bandwidthGrade);
            //  TODO: Merge conditions with currentConditions.
        }
    }

    public void cleanup() {
        if (bandwidthCheckFuture!= null && !bandwidthCheckFuture.isDone()) {
            bandwidthCheckFuture.cancel(true);
            bandwidthCheckFuture = null;
        }
        previousAction = Action.ACTION_NONE;
    }

    private void updateBandwidthState(int toState) {
        if (bandwidthCheckFuture != null) {
            bandwidthCheckFuture.cancel(true);
        }
        bandwidthCheckFuture = null;
        if (toState == STATE_BANDWIDTH_TEST_REQUESTED ||
                toState == STATE_BANDWIDTH_TEST_RUNNING) {
            bandwidthCheckFuture = scheduledExecutorService.schedule(new Runnable() {
                @Override
                public void run() {
                    bandwidthTestStateCheck();
                }
            }, 100, TimeUnit.MILLISECONDS);
        }
    }

    private void bandwidthTestStateCheck() {
        // Timeouts etc.
    }

    private void sendResponseOnlyAction(String response) {
        if (actionListener == null) {
            Log.w(TAG, "Attempting to send action to non-existent listener");
            return;
        }
        if (response == null || response.isEmpty()) {
            response = CANNED_RESPONSE;
        }
        actionListener.takeAction(new Action(response, Action.ActionType.ACTION_TYPE_NONE));
    }
}
