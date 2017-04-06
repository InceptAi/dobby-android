package com.inceptai.dobby.ai;

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

    private Action previousAction;

    public Action interpretApiAiResult(Result result) {
        // Show user response if any.
        String response = result.getFulfillment().getSpeech();
        if (response == null || response.isEmpty()) {
            response = CANNED_RESPONSE;
        }

        String apiAiAction = result.getAction();

        previousAction = new Action(response, Action.ActionType.ACTION_NONE);
        return previousAction;
    }


}
