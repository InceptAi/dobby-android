package com.inceptai.dobby.ai;

import android.support.annotation.IntDef;
import android.util.Log;

import com.inceptai.dobby.speedtest.BandwithTestCodes;
import com.inceptai.dobby.utils.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import ai.api.model.Result;

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
    }

    public Action addGoal(@Goal int goal) {
        return new Action(Utils.EMPTY_STRING, Action.ActionType.ACTION_TYPE_BANDWIDTH_PING_WIFI_TESTS);
    }

    private String testModeToString(@BandwithTestCodes.BandwidthTestMode int testMode) {
        String testModeString = "UNKNOWN";
        if (testMode == BandwithTestCodes.BandwidthTestMode.DOWNLOAD) {
            testModeString = "DOWNLOAD";
        } else if (testMode == BandwithTestCodes.BandwidthTestMode.UPLOAD) {
            testModeString = "UPLOAD";
        }
        return testModeString;
    }

    // Bandwidth test notifications:

    public void notifyBandwidthTestStart(@BandwithTestCodes.BandwidthTestMode int testMode) {

    }

    public void notifyBandwidthTestProgress(@BandwithTestCodes.BandwidthTestMode int testMode, double bandwidth) {
        long currentTs = System.currentTimeMillis();
        if ((currentTs - lastBandwidthUpdateTimestampMs) > 500L) {
            sendResponseOnlyAction(testModeToString(testMode) + " Current Bandwidth: " + String.format("%.2f", bandwidth / 1000000) + " Mbps");
            lastBandwidthUpdateTimestampMs = currentTs;
        }
    }

    public void notifyBandwidthTestResult(@BandwithTestCodes.BandwidthTestMode int testMode,
                                          double bandwidth) {
        sendResponseOnlyAction(testModeToString(testMode) + " Overall Bandwidth = " + String.format("%.2f", bandwidth / 1000000) + " Mbps");
        lastBandwidthUpdateTimestampMs = 0;
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
