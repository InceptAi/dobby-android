package com.inceptai.dobby.ai;

import android.support.annotation.IntDef;

import com.inceptai.dobby.utils.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents an action (to be taken or in progress) by the InferenceEngine.
 */

public class Action {
    public static final Action ACTION_NONE = new Action(Utils.EMPTY_STRING, ActionType.ACTION_TYPE_NONE);

    @IntDef({ActionType.ACTION_TYPE_BANDWIDTH_TEST,
            ActionType.ACTION_TYPE_NONE,
            ActionType.ACTION_TYPE_WIFI_SCAN,
            ActionType.ACTION_TYPE_CANCEL_BANDWIDTH_TEST,
            ActionType.ACTION_TYPE_DIAGNOSE_SLOW_INTERNET,
            ActionType.ACTION_TYPE_BANDWIDTH_PING_WIFI_TESTS
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ActionType {
        int ACTION_TYPE_NONE = 0;
        int ACTION_TYPE_BANDWIDTH_TEST = 1;
        int ACTION_TYPE_WIFI_SCAN = 2;
        int ACTION_TYPE_CANCEL_BANDWIDTH_TEST = 3;
        int ACTION_TYPE_DIAGNOSE_SLOW_INTERNET = 4;
        int ACTION_TYPE_BANDWIDTH_PING_WIFI_TESTS = 5;
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
