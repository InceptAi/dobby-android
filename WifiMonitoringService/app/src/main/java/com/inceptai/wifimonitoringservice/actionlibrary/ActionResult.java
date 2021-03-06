package com.inceptai.wifimonitoringservice.actionlibrary;

/**
 * Created by vivek on 7/5/17.
 */


import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.inceptai.wifimonitoringservice.actionlibrary.ActionResult.ActionResultCodes.EXCEPTION;
import static com.inceptai.wifimonitoringservice.actionlibrary.ActionResult.ActionResultCodes.FAILED_TO_COMPLETE;
import static com.inceptai.wifimonitoringservice.actionlibrary.ActionResult.ActionResultCodes.FAILED_TO_START;
import static com.inceptai.wifimonitoringservice.actionlibrary.ActionResult.ActionResultCodes.SUCCESS;
import static com.inceptai.wifimonitoringservice.actionlibrary.ActionResult.ActionResultCodes.TIMED_OUT;

/**
 * Represents the result of a FutureAction. One of the possible values of T above.
 */
public class ActionResult {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SUCCESS,
            ActionResultCodes.IN_PROGRESS,
            ActionResultCodes.FAILED_TO_START,
            ActionResultCodes.TIMED_OUT,
            ActionResultCodes.GENERAL_ERROR,
            ActionResultCodes.EXCEPTION,
            ActionResultCodes.FAILED_TO_COMPLETE,
            ActionResultCodes.UNKNOWN})
    public @interface ActionResultCodes {
        int SUCCESS = 0;
        int IN_PROGRESS = 1;
        int FAILED_TO_START = 2;
        int TIMED_OUT = 3;
        int GENERAL_ERROR = 4;
        int EXCEPTION = 5;
        int FAILED_TO_COMPLETE = 6;
        int UNKNOWN = 100;
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
            case ActionResultCodes.EXCEPTION:
                return "EXCEPTION";
            default:
                return "UNKNOWN";
        }
    }

    private @ActionResultCodes int status;
    private String statusString;
    private Object payload;

    public ActionResult(@ActionResultCodes  int status) {
        this.status = status;
        this.statusString = ActionResult.actionResultCodeToString(status);
    }

    public ActionResult(@ActionResultCodes  int status, String statusString) {
        this.status = status;
        this.statusString = statusString;
    }

    public ActionResult(@ActionResultCodes  int status, Object payload) {
        this.status = status;
        this.statusString = ActionResult.actionResultCodeToString(status);
        this.payload = payload;
    }

    public ActionResult(@ActionResultCodes int status, String statusString, Object payload) {
        this.status = status;
        this.statusString = statusString;
        this.payload = payload;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    @ActionResultCodes
    public int getStatus() {
        return status;
    }

    public String getStatusString() {
        return statusString;
    }

    public static boolean isSuccessful(ActionResult actionResult) {
        return  (actionResult != null && actionResult.getStatus() == SUCCESS);
    }

    public static boolean failedToComplete(ActionResult actionResult) {
        return  (actionResult != null && actionResult.getStatus() == FAILED_TO_COMPLETE);
    }

    public static boolean failedToStart(ActionResult actionResult) {
        return  (actionResult != null && actionResult.getStatus() == FAILED_TO_START);
    }

    public static boolean timedOut(ActionResult actionResult) {
        return  (actionResult != null && actionResult.getStatus() == TIMED_OUT);
    }

    public static boolean exceptionWhileRunning(ActionResult actionResult) {
        return  (actionResult != null && actionResult.getStatus() == EXCEPTION);
    }

}
