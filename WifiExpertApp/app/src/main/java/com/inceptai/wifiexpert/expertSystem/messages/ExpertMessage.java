package com.inceptai.wifiexpert.expertSystem.messages;

import android.support.annotation.IntDef;

import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.Action;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * Created by vivek on 8/7/17.
 */

public class ExpertMessage {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ExpertMessageType.FREE_FORM, ExpertMessageType.STRUCTURED_LIST,
            ExpertMessageType.ACTION_STARTED, ExpertMessageType.ACTION_FINISHED})
    public @interface ExpertMessageType {
        int FREE_FORM = 0;
        int STRUCTURED_LIST = 1;
        int ACTION_STARTED = 2;
        int ACTION_FINISHED = 3;
    }

    private String message;
    private Action expertAction;
    private ActionResult actionResult;
    private List<StructuredUserResponse> userResponseOptionsToShow;
    @ExpertMessageType private int expertMessageType;

    private ExpertMessage(@ExpertMessageType int expertMessageType,
                          String message, Action action,
                          ActionResult actionResult,
                          List<StructuredUserResponse> userResponseOptionsToShow) {
        this.expertMessageType = expertMessageType;
        this.message = message;
        this.actionResult = actionResult;
        this.expertAction = action;
        this.userResponseOptionsToShow = userResponseOptionsToShow;
    }

    /**
     * Factory constructor to create an instance
     */
    public static ExpertMessage create(Action action) {
        return new ExpertMessage(ExpertMessageType.ACTION_STARTED, action.getName() + " Started", action, null, null);
    }

    public static ExpertMessage create(Action action, ActionResult actionResult) {
        return new ExpertMessage(ExpertMessageType.ACTION_FINISHED, action.getName() + " Finished", action, actionResult, null);
    }

    public String getMessage() {
        return message;
    }

    public Action getExpertAction() {
        return expertAction;
    }

    public ActionResult getActionResult() {
        return actionResult;
    }

    public List<StructuredUserResponse> getUserResponseOptionsToShow() {
        return userResponseOptionsToShow;
    }

    public int getExpertMessageType() {
        return expertMessageType;
    }
}
