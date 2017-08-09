package com.inceptai.wifiexpertsystem.expert;


public class ExpertChatUtil {
    private static final long TWENTY_MINS_IN_MS = 20 * 60 * 1000L;

    private ExpertChatUtil() {}

    public static boolean isEventType(ExpertChat expertChat) {
        return expertChat.getMessageType() == ExpertChat.MSG_TYPE_META_USER_LEFT ||
                expertChat.getMessageType() == ExpertChat.MSG_TYPE_META_USER_ENTERED;
    }

    public static String getDisplayStringForEvent(ExpertChat expertChat) {
        switch (expertChat.getMessageType()) {
            case ExpertChat.MSG_TYPE_META_USER_ENTERED:
                return "User entered chat.";
            case ExpertChat.MSG_TYPE_META_USER_LEFT:
                return "User left chat.";
        }
        return "Unknown event";
    }

    public static boolean isDisplayableMessageType(ExpertChat expertChat) {
        return (expertChat.getMessageType() == ExpertChat.MSG_TYPE_GENERAL_MESSAGE ||
                expertChat.getMessageType() == ExpertChat.MSG_TYPE_USER_TEXT ||
                expertChat.getMessageType() == ExpertChat.MSG_TYPE_EXPERT_TEXT ||
                expertChat.getMessageType() == ExpertChat.MSG_TYPE_BOT_TEXT);
    }

    public static boolean isMessageFresh(ExpertChat expertChat, long lastChatNumberProcessed) {
        return(expertChat.getChatNumber() > lastChatNumberProcessed);
    }

    public static boolean isChatInHumanMode(long lastHumanMessageTimestampMs) {
        return (System.currentTimeMillis() - lastHumanMessageTimestampMs < TWENTY_MINS_IN_MS);
    }
}