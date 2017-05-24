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
            ActionType.ACTION_TYPE_WIFI_CHECK,
            ActionType.ACTION_TYPE_CANCEL_BANDWIDTH_TEST,
            ActionType.ACTION_TYPE_DIAGNOSE_SLOW_INTERNET,
            ActionType.ACTION_TYPE_BANDWIDTH_PING_WIFI_TESTS,
            ActionType.ACTION_TYPE_SHOW_SHORT_SUGGESTION,
            ActionType.ACTION_TYPE_SHOW_LONG_SUGGESTION,
            ActionType.ACTION_TYPE_UNKNOWN,
            ActionType.ACTION_TYPE_WELCOME,
            ActionType.ACTION_TYPE_DEFAULT_FALLBACK,
            ActionType.ACTION_TYPE_ASK_FOR_LONG_SUGGESTION,
            ActionType.ACTION_TYPE_SHOW_WIFI_ANALYSIS,
            ActionType.ACTION_TYPE_LIST_DOBBY_FUNCTIONS,
            ActionType.ACTION_TYPE_ASK_FOR_BW_TESTS
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ActionType {
        int ACTION_TYPE_NONE = 0;
        int ACTION_TYPE_BANDWIDTH_TEST = 1;
        int ACTION_TYPE_WIFI_CHECK = 2;
        int ACTION_TYPE_CANCEL_BANDWIDTH_TEST = 3;
        int ACTION_TYPE_DIAGNOSE_SLOW_INTERNET = 4;
        int ACTION_TYPE_BANDWIDTH_PING_WIFI_TESTS = 5;
        int ACTION_TYPE_SHOW_SHORT_SUGGESTION = 6;
        int ACTION_TYPE_SHOW_LONG_SUGGESTION = 7;
        int ACTION_TYPE_UNKNOWN = 8;
        int ACTION_TYPE_WELCOME = 9;
        int ACTION_TYPE_DEFAULT_FALLBACK = 10;
        int ACTION_TYPE_ASK_FOR_LONG_SUGGESTION = 11;
        int ACTION_TYPE_SHOW_WIFI_ANALYSIS = 12;
        int ACTION_TYPE_LIST_DOBBY_FUNCTIONS = 13;
        int ACTION_TYPE_ASK_FOR_BW_TESTS = 14;
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
