package com.inceptai.wifiexpertsystem.analytics;

import android.os.Bundle;

/**
 * Provides an API for logging analytics events.
 */
abstract class ExpertChatAnalytics {

    //purely User related responses
    //User events
    private static final String USER_ENTERED_APP_FIRST_TIME_EVER = "user_enter_app_first_time";
    private static final String USER_ENTERED_CHAT_FIRST_TIME_EVER = "user_enter_chat_first_time";
    private static final String USER_ENTERED_APP = "user_enter_app";
    private static final String USER_ENTERED_CHAT = "user_enter_chat";
    private static final String USER_DROPPED_OUT = "user_dropped_out";

    //Bot actions
    private static final String BOT_SENT_MESSAGE_TO_USER = "bot_sent_message_to_user";
    private static final String USER_INTERACTED_WITH_BOT = "user_interacted_with_bot";
    private static final String USER_INTERACTED_VIA_TEXT = "user_interact_via_text";
    private static final String USER_INTERACTED_VIA_ACTION = "user_interact_via_action";
    private static final String ASKED_FOR_BOT_FEEDBACK = "ask_for_bot_feedback";
    private static final String BOT_WAS_HELPFUL = "bot_helpful";
    private static final String BOT_RESOLVED_ISSUE = "bot_resolved_issue";
    private static final String BOT_WAS_NOT_HELPFUL = "bot_not_helpful";
    private static final String BOT_DID_NOT_RESOLVE_ISSUE = "bot_did_not_resolve_issue";

    //Expert actions
    private static final String USER_ASKED_FOR_EXPERT = "user_asked_for_expert";
    private static final String USER_SENT_MESSAGE = "user_sent_message";
    private static final String USER_SENT_MESSAGE_TO_EXPERT = "user_sent_message_to_expert";
    private static final String USER_SENT_MESSAGE_TO_BOT = "user_sent_message_to_bot";
    private static final String EXPERT_SENT_MESSAGE_TO_USER = "expert_sent_message_to_user";
    private static final String EXPERT_ENTERED_CHAT_MESSAGE_1MIN = "expert_enter_chat_1_min";
    private static final String EXPERT_ENTERED_CHAT_MESSAGE_5MIN = "expert_enter_chat_5_min";
    private static final String EXPERT_ENTERED_CHAT_MESSAGE_30MIN = "expert_enter_chat_30_min";
    private static final String EXPERT_ENTERED_CHAT_MESSAGE_60MIN = "expert_enter_chat_60_min";
    private static final String EXPERT_ENTERED_CHAT_MESSAGE_12HOURS = "expert_enter_chat_12_hours";
    private static final String USER_INTERACTED_WITH_EXPERT = "user_interacted_with_expert";
    private static final String ASKED_FOR_EXPERT_FEEDBACK = "ask_for_bot_feedback";
    private static final String EXPERT_WAS_HELPFUL = "bot_helpful";
    private static final String EXPERT_RESOLVED_ISSUE = "bot_resolved_issue";
    private static final String EXPERT_WAS_NOT_HELPFUL = "bot_not_helpful";
    private static final String EXPERT_DID_NOT_RESOLVE_ISSUE = "bot_did_not_resolve_issue";

    //Notifications stuff
    private static final String EXPERT_CHAT_NOTIFICATION_SHOWN = "chat_notif_shown";
    private static final String EXPERT_CHAT_NOTIFICATION_CONSUMED = "chat_notif_consumed";

    private AnalyticsBackend analyticsBackend;

    public ExpertChatAnalytics(AnalyticsBackend analyticsBackend) {
        this.analyticsBackend = analyticsBackend;
    }

    public void userEnteredAppFirstTimeEver() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(USER_ENTERED_APP_FIRST_TIME_EVER, bundle);
    }

    public void userEnteredChatFirstTimeEver() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(USER_ENTERED_CHAT_FIRST_TIME_EVER, bundle);
    }

    public void userEnteredApp() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(USER_ENTERED_APP, bundle);
    }

    public void userEnteredChat() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(USER_ENTERED_CHAT, bundle);
    }

    public void userDroppedOut() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(USER_DROPPED_OUT, bundle);
    }

    //Bot related
    public void botSentMessageToUser() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(BOT_SENT_MESSAGE_TO_USER, bundle);
    }

    public void userInteractedWithBot() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(USER_INTERACTED_WITH_BOT, bundle);
    }

    public void userInteractedViaText() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(USER_INTERACTED_VIA_TEXT, bundle);
    }

    public void userInteractedViaAction() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(USER_INTERACTED_VIA_ACTION, bundle);
    }

    public void askedForBotFeedback() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(ASKED_FOR_BOT_FEEDBACK, bundle);
    }

    public void botWasHelpful() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(BOT_WAS_HELPFUL, bundle);
    }

    public void botResolvedIssue() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(BOT_RESOLVED_ISSUE, bundle);
    }

    public void botWasNotHelpful() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(BOT_WAS_NOT_HELPFUL, bundle);
    }

    public void botDidNotResolveIssue() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(BOT_DID_NOT_RESOLVE_ISSUE, bundle);
    }

    //expert related
    public void userAskedForExpert() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(USER_ASKED_FOR_EXPERT, bundle);
    }


    public void userSentMessage() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(USER_SENT_MESSAGE, bundle);
    }

    public void userSentMessageToExpert() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(USER_SENT_MESSAGE_TO_EXPERT, bundle);
    }

    public void userSentMessageToBot() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(USER_SENT_MESSAGE_TO_BOT, bundle);
    }

    public void expertSentMessageToUser() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(EXPERT_SENT_MESSAGE_TO_USER, bundle);
    }


    public void expertEnteredChatWithin1MinOfFirstUserMessage() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(EXPERT_ENTERED_CHAT_MESSAGE_1MIN, bundle);
    }

    public void expertEnteredChatWithin5MinOfFirstUserMessage() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(EXPERT_ENTERED_CHAT_MESSAGE_5MIN, bundle);
    }

    public void expertEnteredChatWithin30MinOfFirstUserMessage() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(EXPERT_ENTERED_CHAT_MESSAGE_30MIN, bundle);
    }

    public void expertEnteredChatWithin60MinOfFirstUserMessage() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(EXPERT_ENTERED_CHAT_MESSAGE_60MIN, bundle);
    }

    public void expertEnteredChatWithin12HourOfFirstUserMessage() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(EXPERT_ENTERED_CHAT_MESSAGE_12HOURS, bundle);
    }

    public void userInteractedWithExpert() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(USER_INTERACTED_WITH_EXPERT, bundle);
    }

    public void askedForExpertFeedback() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(ASKED_FOR_EXPERT_FEEDBACK, bundle);
    }

    public void expertWasHelpful() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(EXPERT_WAS_HELPFUL, bundle);
    }

    public void expertResolvedIssue() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(EXPERT_RESOLVED_ISSUE, bundle);
    }

    public void expertWasNotHelpful() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(EXPERT_WAS_NOT_HELPFUL, bundle);
    }

    public void expertDidNotResolveIssue() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(EXPERT_DID_NOT_RESOLVE_ISSUE, bundle);
    }

    public void expertChatNotificationShown() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(EXPERT_CHAT_NOTIFICATION_SHOWN, bundle);
    }

    public void expertChatNotificationConsumed() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(EXPERT_CHAT_NOTIFICATION_CONSUMED, bundle);
    }

}