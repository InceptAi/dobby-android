package com.inceptai.actionservice;

/**
 * Created by vivek on 7/5/17.
 */


import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Represents the result of a Action. One of the possible values of T above.
 */
public class ActionResult {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ActionResultCodes.SUCCESS, ActionResultCodes.IN_PROGRESS,
            ActionResultCodes.FAILED_TO_START,
            ActionResultCodes.TIMED_OUT, ActionResultCodes.GENERAL_ERROR})
    public @interface ActionResultCodes {
        int SUCCESS = 0;
        int IN_PROGRESS = 1;
        int FAILED_TO_START = 2;
        int TIMED_OUT = 3;
        int GENERAL_ERROR = 4;
    }

    public static String actionResultCodeToString(@ActionResultCodes int code) {
        switch (code) {
            case ActionResultCodes.SUCCESS:
                return "SUCCESS";
            case ActionResultCodes.IN_PROGRESS:
                return "IN_PROGRESS";
            case ActionResultCodes.FAILED_TO_START:
                return "FAILED_TO_START";
            case ActionResultCodes.TIMED_OUT:
                return "TIMED_OUT";
            case ActionResultCodes.GENERAL_ERROR:
                return "GENERAL_ERROR";
            default:
                return "UNKNOWN";
        }
    }

    private @ActionResultCodes int status;
    private String errorString;
    private Object payload;

    public ActionResult(int status, String errorString) {
        this.status = status;
        this.errorString = errorString;
    }

    public ActionResult(int status, String errorString, Object payload) {
        this.status = status;
        this.errorString = errorString;
        this.payload = payload;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public int getStatus() {
        return status;
    }
}
