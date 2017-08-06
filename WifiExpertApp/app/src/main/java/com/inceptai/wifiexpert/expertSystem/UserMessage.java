package com.inceptai.wifiexpert.expertSystem;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by vivek on 8/4/17.
 */

public class UserMessage {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({UserMessageType.FREE_FORM, UserMessageType.STRUCTURED,
            UserMessageType.FORM_INPUT})
    public @interface UserMessageType {
        int FREE_FORM = 0;
        int STRUCTURED = 1;
        int FORM_INPUT = 2;
    }

    @UserMessageType
    private int messageType;
    private String message;
    private StructuredUserResponse structuredUserResponse;

    public UserMessage(String message) {
        this.messageType = UserMessageType.FREE_FORM;
        this.message = message;
    }

    public UserMessage(StructuredUserResponse structuredUserResponse) {
        this.messageType = UserMessageType.STRUCTURED;
        this.structuredUserResponse = structuredUserResponse;
        this.message = StructuredUserResponse.getStringForResponseType(structuredUserResponse.getResponse());
    }

    public int getMessageType() {
        return messageType;
    }

    public String getMessage() {
        return message;
    }

    public StructuredUserResponse getStructuredUserResponse() {
        return structuredUserResponse;
    }
}
