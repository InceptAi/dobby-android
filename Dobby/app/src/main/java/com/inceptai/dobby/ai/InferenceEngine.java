package com.inceptai.dobby.ai;

import android.util.Log;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import static com.inceptai.dobby.DobbyApplication.TAG;

import ai.api.model.Result;

/**
 * Inference engine consumes actions from the NLU engine (ApiAi or local) and creates Actions.
 * It also consumes network metrics as a result of Actions such as bandwidth tests, etc, to further
 * decide the course of action.
 */

public class InferenceEngine {
    private static final String CANNED_RESPONSE = "We are working on it.";

    private static final String APIAI_ACTION_DIAGNOSE_SLOW_INTERNET = "diagnose-slow-internet-action";

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
    private static final int STATE_BANDWIDTH_TEST_STARTING = 2;
    private static final int STATE_BANDWIDTH_TEST_SUCCESS = 3;
    private static final int STATE_BANDWIDTH_TEST_FAILED = 4;

    private Action previousAction;
    private int bandwidthTestState; /* state of the bandwidth test */
    private ScheduledExecutorService scheduledExecutorService;
    private ScheduledFuture<?> bandwidthCheckFuture;
    private ActionListener actionListener;
    private int lastPercentDigit = 0;

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

        @Action.ActionType int action = Action.ActionType.ACTION_NONE;
        String apiAiAction = result.getAction();
        if (APIAI_ACTION_DIAGNOSE_SLOW_INTERNET.equals(apiAiAction)) {
            action = Action.ActionType.ACTION_BANDWIDTH_TEST;
            updateBandwidthState(STATE_BANDWIDTH_TEST_REQUESTED);
        }

        previousAction = new Action(response, action);
        return previousAction;
    }

    public void notifyBandwidthTestProgress(float percent, double bandwidth) {
        int digit = (int) percent / 10;
        if (digit > lastPercentDigit) {
            sendResponseOnlyAction("Percent: " + (int) percent + " bandwidth: " + String.valueOf((int) bandwidth / 1000) + " Kbps.");
            lastPercentDigit = digit;
        }
    }

    public void notifyBandwidthTestResult(double bandwidth) {
        sendResponseOnlyAction("Bandwidth = " + String.valueOf((int) bandwidth / 1000) + " Kbps.");
        lastPercentDigit = 0;
    }

    private void updateBandwidthState(int toState) {
        if (bandwidthCheckFuture != null) {
            bandwidthCheckFuture.cancel(true);
        }
        bandwidthCheckFuture = null;
        if (toState == STATE_BANDWIDTH_TEST_REQUESTED ||
                toState == STATE_BANDWIDTH_TEST_STARTING) {
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
        actionListener.takeAction(new Action(response, Action.ActionType.ACTION_NONE));
    }
}
