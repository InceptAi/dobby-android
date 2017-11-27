package com.inceptai.dobby.ai;

import android.support.annotation.IntDef;

import com.inceptai.dobby.utils.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_NONE;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_WELCOME;

/**
 * Represents an action (to be taken or in progress) by the InferenceEngine.
 */

public class Action {
    public static final Action ACTION_NONE = new Action(Utils.EMPTY_STRING, ACTION_TYPE_NONE);

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
            ActionType.ACTION_TYPE_ASK_FOR_BW_TESTS,
            ActionType.ACTION_TYPE_ASK_FOR_FEEDBACK,
            ActionType.ACTION_TYPE_POSITIVE_FEEDBACK,
            ActionType.ACTION_TYPE_NEGATIVE_FEEDBACK,
            ActionType.ACTION_TYPE_NO_FEEDBACK,
            ActionType.ACTION_TYPE_UNSTRUCTURED_FEEDBACK,
            ActionType.ACTION_TYPE_USER_ASKS_FOR_HUMAN_EXPERT,
            ActionType.ACTION_TYPE_CONTACT_HUMAN_EXPERT,
            ActionType.ACTION_TYPE_RUN_TESTS_FOR_EXPERT,
            ActionType.ACTION_TYPE_CANCEL_TESTS_FOR_EXPERT,
            ActionType.ACTION_TYPE_ASK_FOR_RESUMING_EXPERT_CHAT,
            ActionType.ACTION_TYPE_SET_CHAT_TO_BOT_MODE,
            ActionType.ACTION_TYPE_REPAIR_WIFI,
            ActionType.ACTION_TYPE_CANCEL_REPAIR_WIFI,
            ActionType.ACTION_TYPE_TURN_OFF_WIFI_SERVICE,
            ActionType.ACTION_TYPE_TURN_ON_WIFI_SERVICE,
            ActionType.ACTION_TYPE_USER_SAYS_YES_TO_REPAIR_RECOMMENDATION,
            ActionType.ACTION_TYPE_ASK_FOR_WIFI_REPAIR,
            ActionType.ACTION_TYPE_ASK_USER_FOR_RATING_THE_APP,
            ActionType.ACTION_TYPE_USER_SAYS_YES_TO_RATING_THE_APP,
            ActionType.ACTION_TYPE_USER_SAYS_NO_TO_RATING_THE_APP,
            ActionType.ACTION_TYPE_USER_SAYS_LATER_TO_RATING_THE_APP,
            ActionType.ACTION_TYPE_KEEP_WIFI_ON_DURING_SLEEP,
            ActionType.ACTION_TYPE_ASK_FOR_KEEPING_WIFI_ON_DURING_SLEEP,
            ActionType.ACTION_TYPE_RESET_NETWORK_SETTINGS,
            ActionType.ACTION_TYPE_TOGGLE_AIRPLANE_MODE,
            ActionType.ACTION_TYPE_ASK_FOR_RESETTING_WIFI_NETWORK_SETTINGS,
            ActionType.ACTION_TYPE_ASK_USER_FOR_ACCESSIBILITY_PERMISSION,
            ActionType.ACTION_TYPE_USER_SAYS_YES_TO_ACCESSIBILITY_PERMISSION,
            ActionType.ACTION_TYPE_USER_SAYS_NO_TO_ACCESSIBILITY_PERMISSION,
            ActionType.ACTION_TYPE_USER_SAYS_YES_TO_RESETTING_NETWORK_SETTING
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
        int ACTION_TYPE_ASK_FOR_FEEDBACK = 15;
        int ACTION_TYPE_POSITIVE_FEEDBACK = 16;
        int ACTION_TYPE_NEGATIVE_FEEDBACK = 17;
        int ACTION_TYPE_NO_FEEDBACK = 18;
        int ACTION_TYPE_UNSTRUCTURED_FEEDBACK = 19;
        int ACTION_TYPE_USER_ASKS_FOR_HUMAN_EXPERT = 20;
        int ACTION_TYPE_CONTACT_HUMAN_EXPERT = 21;
        int ACTION_TYPE_RUN_TESTS_FOR_EXPERT = 22;
        int ACTION_TYPE_CANCEL_TESTS_FOR_EXPERT = 23;
        int ACTION_TYPE_ASK_FOR_RESUMING_EXPERT_CHAT = 24;
        int ACTION_TYPE_SET_CHAT_TO_BOT_MODE = 25;
        int ACTION_TYPE_REPAIR_WIFI = 26;
        int ACTION_TYPE_CANCEL_REPAIR_WIFI = 27;
        int ACTION_TYPE_TURN_ON_WIFI_SERVICE = 28;
        int ACTION_TYPE_TURN_OFF_WIFI_SERVICE = 29;
        int ACTION_TYPE_USER_SAYS_YES_TO_REPAIR_RECOMMENDATION = 30;
        int ACTION_TYPE_ASK_FOR_WIFI_REPAIR = 31;
        int ACTION_TYPE_ASK_USER_FOR_RATING_THE_APP = 32;
        int ACTION_TYPE_USER_SAYS_YES_TO_RATING_THE_APP = 33;
        int ACTION_TYPE_USER_SAYS_NO_TO_RATING_THE_APP = 34;
        int ACTION_TYPE_USER_SAYS_LATER_TO_RATING_THE_APP = 35;
        int ACTION_TYPE_ASK_FOR_RESETTING_WIFI_NETWORK_SETTINGS = 36;
        int ACTION_TYPE_KEEP_WIFI_ON_DURING_SLEEP = 37;
        int ACTION_TYPE_RESET_NETWORK_SETTINGS = 38;
        int ACTION_TYPE_TOGGLE_AIRPLANE_MODE = 39;
        int ACTION_TYPE_ASK_FOR_KEEPING_WIFI_ON_DURING_SLEEP = 40;
        int ACTION_TYPE_ASK_USER_FOR_ACCESSIBILITY_PERMISSION = 41;
        int ACTION_TYPE_USER_SAYS_YES_TO_ACCESSIBILITY_PERMISSION = 42;
        int ACTION_TYPE_USER_SAYS_NO_TO_ACCESSIBILITY_PERMISSION = 43;
        int ACTION_TYPE_USER_SAYS_YES_TO_RESETTING_NETWORK_SETTING = 44;
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

    public static boolean isNonEssentialAction(Action action) {
        @ActionType int actionType = action.getAction();
        return  (actionType == ACTION_TYPE_NONE || actionType == ACTION_TYPE_WELCOME ||
                actionType == ActionType.ACTION_TYPE_DEFAULT_FALLBACK);
    }
}
