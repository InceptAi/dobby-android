package com.inceptai.expertchat;

/**
 * Created by arunesh on 5/23/17.
 */

public class ExpertChat {
    private static final String EMPTY_STRING = "";
    public static final int MSG_TYPE_EXPERT_TEXT = 1001;
    public static final int MSG_TYPE_USER_TEXT = 1002;
    public static final int MSG_TYPE_UNKNOWN = 1;

    String id;
    String text;
    int messageType;

    public ExpertChat() {
        id = EMPTY_STRING;
        text = EMPTY_STRING;
        messageType = MSG_TYPE_UNKNOWN;
    }

    public ExpertChat(String text, int messageType) {
        this.text = text;
        this.messageType = messageType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isUserChat() {
        return messageType == MSG_TYPE_USER_TEXT;
    }

    public boolean isExpertChat() {
        return messageType == MSG_TYPE_EXPERT_TEXT;
    }
}
