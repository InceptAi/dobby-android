package com.inceptai.dobby.analytics;

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
    private static final String USER_INTERACTED_WITH_BOT = "user_interacted_with_bot";
    private static final String USER_INTERACTED_WITH_BOT_VIA_TEXT = "user_interact_with_bot_text";
    private static final String USER_INTERACTED_WITH_BOT_VIA_ACTION = "user_interact_with_bot_action";
    private static final String ASKED_FOR_BOT_FEEDBACK = "ask_for_bot_feedback";
    private static final String BOT_WAS_HELPFUL = "bot_helpful";
    private static final String BOT_RESOLVED_ISSUE = "bot_resolved_issue";
    private static final String BOT_WAS_NOT_HELPFUL = "bot_not_helpful";
    private static final String BOT_DID_NOT_RESOLVE_ISSUE = "bot_did_not_resolve_issue";


    private static final String USER_TYPED_MESSAGE_TO_EXPERT = "user_typed_message_to_expert";
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

    private AnalyticsBackend analyticsBackend;

    public ExpertChatAnalytics(AnalyticsBackend analyticsBackend) {
        this.analyticsBackend = analyticsBackend;
    }

    void userEnteredAppFirstTimeEver() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(USER_ENTERED_APP_FIRST_TIME_EVER, bundle);
    }

    void userEnteredChatFirstTimeEver() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(USER_ENTERED_CHAT_FIRST_TIME_EVER, bundle);
    }

    void userEnteredApp() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(USER_ENTERED_APP, bundle);
    }

    void userEnteredChat() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(USER_ENTERED_CHAT, bundle);
    }

    void userDroppedOut() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(USER_DROPPED_OUT, bundle);
    }

    //Bot related
    void userInteractedWithBot() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(USER_INTERACTED_WITH_BOT, bundle);
    }

    void userInteractedWithBotViaText() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(USER_INTERACTED_WITH_BOT_VIA_TEXT, bundle);
    }

    void userInteractedWithBotViaAction() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(USER_INTERACTED_WITH_BOT_VIA_ACTION, bundle);
    }

    void askedForBotFeedback() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(ASKED_FOR_BOT_FEEDBACK, bundle);
    }

    void botWasHelpful() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(BOT_WAS_HELPFUL, bundle);
    }

    void botResolvedIssue() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(BOT_RESOLVED_ISSUE, bundle);
    }

    void botWasNotHelpful() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(BOT_WAS_NOT_HELPFUL, bundle);
    }

    void botDidNotResolveIssue() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(BOT_DID_NOT_RESOLVE_ISSUE, bundle);
    }

    //expert related
    void userTypedMessageToExpert() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(USER_TYPED_MESSAGE_TO_EXPERT, bundle);
    }

    void expertEnteredChatWithin1MinOfFirstUserMessage() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(EXPERT_ENTERED_CHAT_MESSAGE_1MIN, bundle);
    }

    void expertEnteredChatWithin5MinOfFirstUserMessage() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(EXPERT_ENTERED_CHAT_MESSAGE_5MIN, bundle);
    }

    void expertEnteredChatWithin30MinOfFirstUserMessage() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(EXPERT_ENTERED_CHAT_MESSAGE_30MIN, bundle);
    }

    void expertEnteredChatWithin60MinOfFirstUserMessage() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(EXPERT_ENTERED_CHAT_MESSAGE_60MIN, bundle);
    }

    void expertEnteredChatWithin12HourOfFirstUserMessage() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(EXPERT_ENTERED_CHAT_MESSAGE_12HOURS, bundle);
    }

    void userInteractedWithExpert() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(USER_INTERACTED_WITH_EXPERT, bundle);
    }

    void askedForExpertFeedback() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(ASKED_FOR_EXPERT_FEEDBACK, bundle);
    }

    void expertWasHelpful() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(EXPERT_WAS_HELPFUL, bundle);
    }

    void expertResolvedIssue() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(EXPERT_RESOLVED_ISSUE, bundle);
    }

    void expertWasNotHelpful() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(EXPERT_WAS_NOT_HELPFUL, bundle);
    }

    void expertDidNotResolveIssue() {
        Bundle bundle = new Bundle();
        analyticsBackend.logEvent(EXPERT_DID_NOT_RESOLVE_ISSUE, bundle);
    }


}