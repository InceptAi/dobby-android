package com.inceptai.dobby.expert;


public class ExpertChatUtil {

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
}