package com.inceptai.dobby.ai;

import ai.api.model.Result;

/**
 * Inference engine consumes actions from the NLU engine (ApiAi or local) and creates Actions.
 * It also consumes network metrics as a result of Actions such as bandwidth tests, etc, to further
 * decide the course of action.
 */

public class InferenceEngine {
    private static final String CANNED_RESPONSE = "We are working on it.";

    public Action interpretApiAiResult(Result result) {
        // Show user response if any.
        String response = result.getFulfillment().getSpeech();
        if (response == null || response.isEmpty()) {
            response = CANNED_RESPONSE;
        }

        Action action = new Action(response, Action.ActionType.ACTION_NONE);
        return action;
    }
}
