package com.inceptai.expertchat;


import java.util.Date;

/**
 * Created by arunesh on 5/23/17.
 */

public class ExpertChat {
    private static final String EMPTY_STRING = "";
    public static final int MSG_TYPE_EXPERT_TEXT = 1001;
    public static final int MSG_TYPE_USER_TEXT = 1002;
    public static final int MSG_TYPE_BOT_TEXT = 1003;
    public static final String SPECIAL_MESSAGE_PREFIX = "#";


    // General messages include ETA and welcome messages.

    public static final int MSG_TYPE_GENERAL_MESSAGE = 1004;
    public static final int MSG_TYPE_UNKNOWN = 1;

    public static final int MSG_TYPE_META_USER_LEFT = 5001;
    public static final int MSG_TYPE_META_USER_ENTERED = 5002;
    public static final int MSG_TYPE_META_ACTION_STARTED = 5003;
    public static final int MSG_TYPE_META_ACTION_COMPLETED = 5004;

    public static final long TWENTY_DAYS_IN_MS = 24 * 60 * 60 * 1000 * 20;


    String id;
    String text;
    int messageType;
    String timestamp;
    long utcTimestampMs;


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
        utcTimestampMs = System.currentTimeMillis() + TWENTY_DAYS_IN_MS;
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

    public int getMessageType() {
        return messageType;
    }

    public static boolean isUserChat(ExpertChat expertChat) {
        return expertChat.messageType == MSG_TYPE_USER_TEXT;
    }

    public static boolean isExpertChat(ExpertChat expertChat) {
        return expertChat.messageType == MSG_TYPE_EXPERT_TEXT;
    }

    public static boolean isBotChat(ExpertChat expertChat) {
        return expertChat.messageType == MSG_TYPE_BOT_TEXT;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public long getUtcTimestampMs() {
        return utcTimestampMs;
    }

    public void setUtcTimestampMs(long utcTimestampMs) {
        this.utcTimestampMs = utcTimestampMs;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isHashtagMessage() {
        return text != null && !text.isEmpty() && text.startsWith("#");
    }

}
