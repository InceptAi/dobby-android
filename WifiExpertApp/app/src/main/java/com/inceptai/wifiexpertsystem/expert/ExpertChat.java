package com.inceptai.wifiexpertsystem.expert;

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
    public static final int MSG_TYPE_META_SEND_MESSAGE_TO_EXPERT_FOR_HELP = 5005;


    //Neo stuff
    public static final int MSG_TYPE_META_NEO_SERVICE_READY = 6001;
    public static final int MSG_TYPE_META_NEO_SERVICE_STOPPED_BY_USER = 6002;
    public static final int MSG_TYPE_META_NEO_SERVICE_STOPPED_BY_EXPERT = 6003;
    public static final int MSG_TYPE_META_NEO_SERVICE_STOPPED_DUE_TO_ERRORS = 6004;
    public static final int MSG_TYPE_META_NEO_SERVICE_OVERLAY_PERMISSION_DENIED = 6005;
    public static final int MSG_TYPE_META_NEO_SERVICE_OVERLAY_PERMISSION_GRANTED = 6006;
    public static final int MSG_TYPE_META_NEO_ACCESSIBILITY_PERMISSION_NOT_GRANTED_WITHIN_TIMEOUT = 6007;



    String id;
    private String text;
    private int messageType;
    private String timestamp;
    private long utcTimestampMs;
    private boolean isAutoGenerated;
    private long chatNumber;
    private boolean isMessageFresh;


    public ExpertChat() {
        id = EMPTY_STRING;
        text = EMPTY_STRING;
        messageType = MSG_TYPE_UNKNOWN;
        isAutoGenerated = false;
        chatNumber = 0;
        isMessageFresh = false;
        computeTimestamp();
    }

    public ExpertChat(String text, int messageType, boolean isAutoGenerated, long chatNumber) {
        this.text = text;
        this.messageType = messageType;
        this.isAutoGenerated = isAutoGenerated;
        this.chatNumber = chatNumber;
        computeTimestamp();
        utcTimestampMs = System.currentTimeMillis();
        isMessageFresh = false;
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

    public boolean isAutoGenerated() {
        return isAutoGenerated;
    }

    public void setAutoGenerated(boolean autoGenerated) {
        isAutoGenerated = autoGenerated;
    }

    public long getChatNumber() {
        return chatNumber;
    }

    public void setChatNumber(long chatNumber) {
        this.chatNumber = chatNumber;
    }

    public boolean isMessageFresh() {
        return isMessageFresh;
    }

    public void setMessageFresh(boolean messageFresh) {
        isMessageFresh = messageFresh;
    }
}