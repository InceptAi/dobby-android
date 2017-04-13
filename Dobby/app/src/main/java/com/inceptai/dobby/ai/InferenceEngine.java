package com.inceptai.dobby.ai;

import android.util.Log;

import com.inceptai.dobby.speedtest.BandwithTestCodes;

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

    private static final String APIAI_ACTION_DIAGNOSE_SLOW_INTERNET = "diagnose-slow-internet-action";

    private static final String PERFORM_BW_TEST_RETURN_RESULT = "perform-bw-test-return-result";

    private static final String APIAI_ACTION_SI_STARTING_INTENT_NO =
            "slow-internet-starting-intent.slow-internet-starting-intent-no";

    private static final String APIAI_ACTION_SI_STARTING_INTENT_CANCEL =
            "slow-internet-starting-intent.slow-internet-starting-intent-cancel";

    private static final String APIAI_ACTION_SI_STARTING_INTENT_LATER =
            "slow-internet-starting-intent.slow-internet-starting-intent-later";

    private static final String APIAI_ACTION_SI_STARTING_INTENT_YES_YES_CANCEL =
            "slow-internet-starting-intent.slow-internet-starting-intent-yes.slow-internet-starting-intent-yes-cancel";

    private static final String APIAI_ACTION_SI_STARTING_INTENT_YES_YES_LATER =
            "slow-internet-starting-intent.slow-internet-starting-intent-yes.slow-internet-starting-intent-yes-later";

    private static final String APIAI_ACTION_SI_STARTING_INTENT_YES_YES_NO =
            "slow-internet-starting-intent.slow-internet-starting-intent-yes.slow-internet-starting-intent-yes-no";

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

    InferenceEngine(ScheduledExecutorService scheduledExecutorService, ActionListener actionListener) {
        bandwidthTestState = STATE_BANDWIDTH_TEST_NONE;
        this.scheduledExecutorService = scheduledExecutorService;
        this.actionListener = actionListener;
    }

    public Action interpretApiAiResult(Result result) {
        // Show user response if any.
        String response = result.getFulfillment().getSpeech();
        if (response == null || response.isEmpty()) {
            response = CANNED_RESPONSE;
        }

        @Action.ActionType int action = Action.ActionType.ACTION_TYPE_NONE;
        String apiAiAction = result.getAction();
        if (APIAI_ACTION_DIAGNOSE_SLOW_INTERNET.equals(apiAiAction) || PERFORM_BW_TEST_RETURN_RESULT.equals(apiAiAction)) {
            action = Action.ActionType.ACTION_TYPE_BANDWIDTH_TEST;
            updateBandwidthState(STATE_BANDWIDTH_TEST_REQUESTED);
        } else if((apiAiAction.contains("cancel") || apiAiAction.contains("later") || apiAiAction.contains("no")) && apiAiAction.contains("test")) {
            // TODO: Remove this hack.
        /*
                (APIAI_ACTION_SI_STARTING_INTENT_CANCEL.equals(apiAiAction) ||
                APIAI_ACTION_SI_STARTING_INTENT_LATER.equals(apiAiAction) ||
                APIAI_ACTION_SI_STARTING_INTENT_NO.equals(apiAiAction) ||
                APIAI_ACTION_SI_STARTING_INTENT_YES_YES_CANCEL.equals(apiAiAction) ||
                APIAI_ACTION_SI_STARTING_INTENT_YES_YES_LATER.equals(apiAiAction) ||
                APIAI_ACTION_SI_STARTING_INTENT_YES_YES_NO.equals(apiAiAction))
         */
            action = Action.ActionType.ACTION_TYPE_CANCEL_BANDWIDTH_TEST;
            updateBandwidthState(STATE_BANDWIDTH_TEST_CANCELLED);
        }
        previousAction = new Action(response, action);
        return previousAction;
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
        if (!bandwidthCheckFuture.isDone()) {
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
