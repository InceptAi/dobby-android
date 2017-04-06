package com.inceptai.dobby.ai;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents an action (to be taken or in progress) by the InferenceEngine.
 */

public class Action {

    @IntDef({ActionType.ACTION_BANDWIDTH_TEST, ActionType.ACTION_NONE, ActionType.ACTION_WIFI_SCAN})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ActionType {
        int ACTION_NONE = 0;
        int ACTION_BANDWIDTH_TEST = 1;
        int ACTION_WIFI_SCAN = 2;
    }

    /* User response to be shown, null for no response. */
    private String userResponse;

    @ActionType
    private int action;

    public String getUserResponse() {
        return userResponse;
    }

    @ActionType
    public int getAction() {
        return action;
    }

    public Action(String userResponse, @ActionType  int action) {
        this.userResponse = userResponse;
        this.action = action;
    }
}
