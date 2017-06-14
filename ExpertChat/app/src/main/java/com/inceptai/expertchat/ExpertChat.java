package com.inceptai.expertchat;


import java.util.Date;

/**
 * Created by arunesh on 5/23/17.
 */

public class ExpertChat {
    private static final String EMPTY_STRING = "";
    public static final int MSG_TYPE_EXPERT_TEXT = 1001;
    public static final int MSG_TYPE_USER_TEXT = 1002;
    public static final int MSG_TYPE_UNKNOWN = 1;

    public static final int MSG_TYPE_META_USER_LEFT = 5001;


    String id;
    String text;
    int messageType;
    String timestamp;


    public ExpertChat() {
        id = EMPTY_STRING;
        text = EMPTY_STRING;
        messageType = MSG_TYPE_UNKNOWN;
        computeTimestamp();
    }

    public ExpertChat(String text, int messageType) {
        this.text = text;
        this.messageType = messageType;
        computeTimestamp();
    }

    private void computeTimestamp() {
        Date date = new Date();
        timestamp = date.toString();
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

    public static boolean isUserChat(ExpertChat expertChat) {
        return expertChat.messageType == MSG_TYPE_USER_TEXT;
    }

    public static boolean isExpertChat(ExpertChat expertChat) {
        return expertChat.messageType == MSG_TYPE_EXPERT_TEXT;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public int getMessageType() {
        return messageType;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
