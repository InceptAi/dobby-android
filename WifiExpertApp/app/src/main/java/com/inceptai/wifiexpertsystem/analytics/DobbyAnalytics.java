package com.inceptai.wifiexpertsystem.analytics;

/**
 * Created by vivek on 7/3/17.
 */

import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.inceptai.wifiexpertsystem.DobbyApplication;
import com.inceptai.wifiexpertsystem.utils.Utils;

import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Provides an API for logging analytics events.
 */
@Singleton
public class DobbyAnalytics extends ExpertChatAnalytics {
    private static final String RUN_TESTS_ITEM = "run_tests";
    private static final String BRIEF_SUGGESTIONS_ITEM = "brief_suggestions";

    private static final String FAB_CLICKED_CONTENT = "fab_clicked";
    private static final String AUTO_CONTENT = "auto_content";
    private static final String MORE_SUGGESTION_CLICKED = "more_suggestions_clicked";
    private static final String ABOUT_DIALOG_SHOWN = "about_dialog_shown";
    private static final String FEEDBACK_DIALOG_SHOWN = "feedback_dialog_shown";
    private static final String TESTS_CANCELLED = "tests_cancelled";

    private static final String PARAM_SUGGESTION_TEXT = "suggestion_text";
    private static final String PARAM_SUGGESTION_TITLE = "suggestion_title";
    private static final String PARAM_GRADE_TEXT = "grade_text";
    private static final String PARAM_GRADE_JSON = "grade_json";
    private static final String FEEDBACK_UNSTRUCTURED = "feedback_unstructured";
    private static final String PARAM_UID = "user_id";
    private static final String PARAM_LATITUDE = "user_lat";
    private static final String PARAM_LONGITUDE = "user_lon";
    private static final String PARAM_ETA = "expert_chat_eta";


    private static final String BANDWIDTH_GRADE_EVENT = "bandwidth_grade";
    private static final String WIFI_GRADE_EVENT = "wifi_grade";
    private static final String PING_GRADE_EVENT = "ping_grade";



    //Unique Wifi Expert events
    private static final String WIFI_EXPERT_FRAGMENT_ENTERED = "expert_opened";
    private static final String WIFI_EXPERT_RUN_TESTS_BUTTON_CLICKED = "expert_run_tests";
    private static final String WIFI_EXPERT_CHECK_WIFI_BUTTON_CLICKED = "expert_check_wifi";
    private static final String WIFI_EXPERT_MORE_DETAILS_BUTTON_CLICKED = "expert_more_details";
    private static final String WIFI_EXPERT_SLOW_INTERNET_BUTTON_CLICKED = "expert_more_details";
    private static final String WIFI_EXPERT_DECLINE_RUNNING_FULL_BANDWIDTH_TESTS_CLICKED = "expert_decline_tests";
    private static final String WIFI_EXPERT_ACCEPT_RUNNING_FULL_BANDWIDTH_TESTS_CLICKED = "expert_accept_tests";
    private static final String WIFI_EXPERT_CANCEL_BANDWIDTH_TEST_BUTTON_CLICKED = "expert_cancel_tests";
    private static final String WIFI_EXPERT_CONTACT_EXPERT_BUTTON_CLICKED = "contact_expert_clicked";

    private static final String EXPERT_CHAT_CONTINUE_BUTTON_CLICKED = "first_time_chat_continue";


    //Expert tagged events
    private static final String EXPERT_SAYS_ISSUE_RESOLVED = "expert_issue_resolved";
    private static final String EXPERT_SAYS_ISSUE_UNRESOLVED = "expert_issue_unresolved";
    private static final String EXPERT_SAYS_MORE_DATA_NEEDED = "expert_more_data_needed";
    private static final String EXPERT_SAYS_USER_DROPPED_OFF = "expert_says_user_dropped";
    private static final String EXPERT_SAYS_GOOD_INFERENCING = "expert_says_good_infer";
    private static final String EXPERT_SAYS_BAD_INFERENCING = "expert_says_bad_infer";
    private static final String EXPERT_SAYS_INFERENCING_CAN_BE_BETTER = "expert_says_infer_can_be_better";


    /*
    Contact expert click event.
    Share results event.
    Notification shown for expert chat event.
    Notification consumed.
     */
    //Expert button events
    private static final String CONTACT_EXPERT_BUTTON_CLICKED = "contact_expert_event";
    private static final String SHARE_RESULT_BUTTON_CLICKED = "share_result_event";
    private static final String EXPERT_FEEDBACK_BUTTON_CLICKED = "feedback_button_clicked";
    private static final String SHOW_ETA_TO_USER = "show_eta_to_user";
    private static final String ONBOARDING_FINISH_CLICKED = "onboarding_finish_click";
    private static final String ONBOARDING_SKIP_CLICKED = "onboarding_skip_click";
    private static final String ONBOARDING_NEXT_CLICKED = "onboarding_next_click";
    private static final String ONBOARDING_SHOWN = "onboarding_shown";




    //Actions triggered
    private static final String ACTION_TYPE_BANDWIDTH_TEST_TAKEN = "action_bw_test";
    private static final String ACTION_TYPE_WIFI_CHECK_TAKEN = "action_wifi_check";
    private static final String ACTION_TYPE_CANCEL_BANDWIDTH_TEST_TAKEN = "action_cancel_bw_test";
    private static final String ACTION_TYPE_DIAGNOSE_SLOW_INTERNET_TAKEN = "action_diagnose_slow_internet";
    private static final String ACTION_TYPE_BANDWIDTH_PING_WIFI_TESTS_TAKEN = "action_bw_ping_wifi_tests";
    private static final String ACTION_TYPE_SHOW_SHORT_SUGGESTION_TAKEN = "action_showing_short_suggestion";
    private static final String ACTION_TYPE_WELCOME_TAKEN = "action_welcome";
    private static final String ACTION_TYPE_DEFAULT_FALLBACK_TAKEN = "action_default_fallback";
    private static final String ACTION_TYPE_SHOWING_LONG_SUGGESTION_TAKEN = "action_showing_long_suggestion";
    private static final String ACTION_TYPE_SHOW_WIFI_ANALYSIS_TAKEN = "action_show_wifi_analysis";
    private static final String ACTION_TYPE_LIST_DOBBY_FUNCTIONS_TAKEN = "action_list_dobby_functions";
    private static final String ACTION_TYPE_ASK_FOR_BW_TESTS_TAKEN = "ask_bw_tests_after_wifi_check";
    private static final String ACTION_TYPE_ASK_FOR_DETAILED_SUGGESTIONS = "action_ask_for_showing_details";
    private static final String ACTION_TYPE_DECLINE_DETAILED_SUGGESTIONS = "action_decline_details";
    private static final String ACTION_TYPE_WIFI_CARD_SHOWN = "action_wifi_card_shown";
    private static final String ACTION_TYPE_BANDWIDTH_CARD_SHOWN = "action_bw_card_shown";

    //Feedback events
    private static final String ACTION_TYPE_ASK_FOR_FEEDBACK_AFTER_LONG_SUGGESTION = "ask_feedback_long_sugg";
    private static final String ACTION_TYPE_POSITIVE_FEEDBACK_AFTER_LONG_SUGGESTION = "positive_feedback_long_sugg";
    private static final String ACTION_TYPE_NEGATIVE_FEEDBACK_AFTER_LONG_SUGGESTION = "negative_feedback_long_sugg";
    private static final String ACTION_TYPE_NO_FEEDBACK_AFTER_LONG_SUGGESTION = "no_feedback_long_sugg";
    private static final String ACTION_TYPE_UNSTRUCTURED_FEEDBACK_AFTER_LONG_SUGGESTION = "unstructured_feedback_long_sugg";

    private static final String ACTION_TYPE_ASK_FOR_FEEDBACK_AFTER_SHORT_SUGGESTION = "ask_feedback_long_sugg";
    private static final String ACTION_TYPE_POSITIVE_FEEDBACK_AFTER_SHORT_SUGGESTION = "positive_feedback_long_sugg";
    private static final String ACTION_TYPE_NEGATIVE_FEEDBACK_AFTER_SHORT_SUGGESTION = "negative_feedback_long_sugg";
    private static final String ACTION_TYPE_NO_FEEDBACK_AFTER_SHORT_SUGGESTION = "no_feedback_long_sugg";
    private static final String ACTION_TYPE_UNSTRUCTURED_FEEDBACK_AFTER_SHORT_SUGGESTION = "unstructured_feedback_long_sugg";

    private static final String ACTION_TYPE_ASK_FOR_FEEDBACK_AFTER_WIFI_CHECK = "ask_feedback_wifi_check";
    private static final String ACTION_TYPE_POSITIVE_FEEDBACK_AFTER_WIFI_CHECK = "positive_feedback_wifi_check";
    private static final String ACTION_TYPE_NEGATIVE_FEEDBACK_AFTER_WIFI_CHECK = "negative_feedback_wifi_check";
    private static final String ACTION_TYPE_NO_FEEDBACK_AFTER_WIFI_CHECK = "no_feedback_wifi_check";
    private static final String ACTION_TYPE_UNSTRUCTURED_FEEDBACK_AFTER_WIFI_CHECK = "unstructured_feedback_wifi_check";


    private static final String DAILY_HEARTBEAT_EVENT = "daily_heartbeat_event";


    //service related
    private static final String WIFI_SERVICE_STARTED = "wifi_service_started";
    private static final String WIFI_SERVICE_STOPPED = "wifi_service_stopped";
    private static final String WIFI_SERVICE_ON_BOARDING_SHOWN = "wifi_service_onboarding";
    private static final String WIFI_SERVICE_NOTIFICATION_SHOWN = "wifi_notification_shown";

    //binding related
    private static final String WIFI_SERVICE_BINDING_FAILED = "wifi_service_binding_failed";
    private static final String WIFI_SERVICE_BINDING_SUCCESSFUL = "wifi_service_binding_good";
    private static final String WIFI_SERVICE_BINDING_NOT_AVAILABLE = "wifi_service_binding_not_found";
    private static final String WIFI_SERVICE_UNBINDING_CALLED = "wifi_service_unbinding";
    private static final String WIFI_SERVICE_DISCONNECTED = "wifi_service_disconnected";
    private static final String WIFI_SERVICE_SECURITY_EXCEPTION = "wifi_service_security_exception";
    private static final String WIFI_SERVICE_CONNECTED = "wifi_service_connected";



    private DobbyAnalyticsBackend dobbyAnalyticsBackend;

    @Inject
    DobbyAnalytics(DobbyApplication dobbyApplication, DobbyAnalyticsBackend dobbyAnalyticsBackend) {
        super(dobbyAnalyticsBackend);
        this.dobbyAnalyticsBackend = dobbyAnalyticsBackend;
        if (dobbyApplication.isRunningOnEmulator()) {
            dobbyAnalyticsBackend.setUserProperty("isRunningOnEmulator", Utils.TRUE_STRING);
        } else {
            dobbyAnalyticsBackend.setUserProperty("isRunningOnEmulator", Utils.FALSE_STRING);
        }
    }

    public void runTestsClicked() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, RUN_TESTS_ITEM);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, FAB_CLICKED_CONTENT);
        dobbyAnalyticsBackend.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    public void briefSuggestionsShown(String suggestionText) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, BRIEF_SUGGESTIONS_ITEM);
        bundle.putString(PARAM_SUGGESTION_TEXT, suggestionText);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, AUTO_CONTENT);
        dobbyAnalyticsBackend.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    public void moreSuggestionsShown(String title, ArrayList<String> longSuggestionList) {
        Bundle bundle = new Bundle();
        bundle.putString(PARAM_SUGGESTION_TITLE, title);
        bundle.putStringArrayList(PARAM_SUGGESTION_TEXT, longSuggestionList);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, MORE_SUGGESTION_CLICKED);
        dobbyAnalyticsBackend.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    public void aboutShown() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, ABOUT_DIALOG_SHOWN);
        dobbyAnalyticsBackend.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    public void feedbackFormShown() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, FEEDBACK_DIALOG_SHOWN);
        dobbyAnalyticsBackend.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    public void testsCancelled() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, TESTS_CANCELLED);
        dobbyAnalyticsBackend.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }


    //Wifi Expert events
    public void wifiExpertFragmentEntered() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, WIFI_EXPERT_FRAGMENT_ENTERED);
        dobbyAnalyticsBackend.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        dobbyAnalyticsBackend.logEvent(WIFI_EXPERT_FRAGMENT_ENTERED, bundle);
    }

    public void wifiExpertWelcomeMessageShown() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, ACTION_TYPE_WELCOME_TAKEN);
        dobbyAnalyticsBackend.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        dobbyAnalyticsBackend.logEvent(ACTION_TYPE_WELCOME_TAKEN, bundle);
    }

    public void wifiExpertRunTestButtonClicked() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, WIFI_EXPERT_RUN_TESTS_BUTTON_CLICKED);
        dobbyAnalyticsBackend.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        dobbyAnalyticsBackend.logEvent(WIFI_EXPERT_RUN_TESTS_BUTTON_CLICKED, bundle);
    }

    public void wifiExpertCheckWifiButtonClicked() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, WIFI_EXPERT_CHECK_WIFI_BUTTON_CLICKED);
        dobbyAnalyticsBackend.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        dobbyAnalyticsBackend.logEvent(WIFI_EXPERT_CHECK_WIFI_BUTTON_CLICKED, bundle);
    }

    public void wifiExpertSlowInternetButtonClicked() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, WIFI_EXPERT_SLOW_INTERNET_BUTTON_CLICKED);
        dobbyAnalyticsBackend.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        dobbyAnalyticsBackend.logEvent(WIFI_EXPERT_SLOW_INTERNET_BUTTON_CLICKED, bundle);

    }

    public void wifiExpertMoreDetailsButtonClicked() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, WIFI_EXPERT_MORE_DETAILS_BUTTON_CLICKED);
        dobbyAnalyticsBackend.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        dobbyAnalyticsBackend.logEvent(WIFI_EXPERT_MORE_DETAILS_BUTTON_CLICKED, bundle);
    }

    public void wifiExpertContactExpertButtonClicked() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, WIFI_EXPERT_CONTACT_EXPERT_BUTTON_CLICKED);
        dobbyAnalyticsBackend.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        dobbyAnalyticsBackend.logEvent(WIFI_EXPERT_CONTACT_EXPERT_BUTTON_CLICKED, bundle);
    }

    public void wifiExpertDeclineRunningFullBandwidthTestsClicked() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, WIFI_EXPERT_DECLINE_RUNNING_FULL_BANDWIDTH_TESTS_CLICKED);
        dobbyAnalyticsBackend.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        dobbyAnalyticsBackend.logEvent(WIFI_EXPERT_DECLINE_RUNNING_FULL_BANDWIDTH_TESTS_CLICKED, bundle);
    }

    public void wifiExpertAcceptRunningFullBandwidthTestsClicked() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, WIFI_EXPERT_ACCEPT_RUNNING_FULL_BANDWIDTH_TESTS_CLICKED);
        dobbyAnalyticsBackend.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        dobbyAnalyticsBackend.logEvent(WIFI_EXPERT_ACCEPT_RUNNING_FULL_BANDWIDTH_TESTS_CLICKED, bundle);

    }

    public void wifiExpertCancelBandwidthTestsClicked() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, WIFI_EXPERT_CANCEL_BANDWIDTH_TEST_BUTTON_CLICKED);
        dobbyAnalyticsBackend.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        dobbyAnalyticsBackend.logEvent(WIFI_EXPERT_CANCEL_BANDWIDTH_TEST_BUTTON_CLICKED, bundle);
    }

    //Wifi expert actions
    public void wifiExpertRunningBandwidthTests() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(ACTION_TYPE_BANDWIDTH_TEST_TAKEN, bundle);
    }

    public void wifiExpertWifiCheck() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(ACTION_TYPE_WIFI_CHECK_TAKEN, bundle);
    }

    public void wifiExpertCancelBandwidthTest() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(ACTION_TYPE_CANCEL_BANDWIDTH_TEST_TAKEN, bundle);
    }

    public void wifiExpertDiagnoseSlowInternet() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(ACTION_TYPE_DIAGNOSE_SLOW_INTERNET_TAKEN, bundle);
    }

    public void wifiExpertBwPingWifiTest() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(ACTION_TYPE_BANDWIDTH_PING_WIFI_TESTS_TAKEN, bundle);
    }

    public void wifiExpertShowShortSuggestion(String suggestionText) {
        Bundle bundle = new Bundle();
        bundle.putString(PARAM_SUGGESTION_TEXT, suggestionText);
        dobbyAnalyticsBackend.logEvent(ACTION_TYPE_SHOW_SHORT_SUGGESTION_TAKEN, bundle);
    }

    public void wifiExpertDefaultFallbackAction() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(ACTION_TYPE_DEFAULT_FALLBACK_TAKEN, bundle);
    }

    public void wifiExpertShowLongSuggestion(String suggestionText) {
        Bundle bundle = new Bundle();
        bundle.putString(PARAM_SUGGESTION_TEXT, suggestionText);
        dobbyAnalyticsBackend.logEvent(ACTION_TYPE_SHOWING_LONG_SUGGESTION_TAKEN, bundle);
    }

    public void wifiExpertShowWifiAnalysis() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(ACTION_TYPE_SHOW_WIFI_ANALYSIS_TAKEN, bundle);
    }

    public void wifiExpertListDobbyFunctions() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(ACTION_TYPE_LIST_DOBBY_FUNCTIONS_TAKEN, bundle);
    }

    public void wifiExpertAskForBwTestsAfterWifiCheck() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(ACTION_TYPE_ASK_FOR_BW_TESTS_TAKEN, bundle);
    }

    public void wifiExpertAskForLongSuggestion() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(ACTION_TYPE_ASK_FOR_DETAILED_SUGGESTIONS, bundle);
    }

    public void wifiExpertDeclineLongSuggestion() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(ACTION_TYPE_DECLINE_DETAILED_SUGGESTIONS, bundle);
    }

    public void wifiExpertWifiCardShown() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(ACTION_TYPE_WIFI_CARD_SHOWN, bundle);
    }

    public void wifiExpertBandwidthCardShown() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(ACTION_TYPE_BANDWIDTH_CARD_SHOWN, bundle);
    }

    //Long suggestion feedback
    public void wifiExpertAskForFeedbackAfterLongSuggestion() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(ACTION_TYPE_ASK_FOR_FEEDBACK_AFTER_LONG_SUGGESTION, bundle);
    }

    public void wifiExpertPositiveFeedbackAfterLongSuggestion() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(ACTION_TYPE_POSITIVE_FEEDBACK_AFTER_LONG_SUGGESTION, bundle);
    }

    public void wifiExpertNegativeFeedbackAfterLongSuggestion() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(ACTION_TYPE_NEGATIVE_FEEDBACK_AFTER_LONG_SUGGESTION, bundle);
    }

    public void wifiExpertNoFeedbackAfterLongSuggestion() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(ACTION_TYPE_NO_FEEDBACK_AFTER_LONG_SUGGESTION, bundle);
    }

    public void wifiExpertUnstructuredFeedbackAfterLongSuggestion(String feedback) {
        Bundle bundle = new Bundle();
        bundle.putString(FEEDBACK_UNSTRUCTURED, feedback);
        dobbyAnalyticsBackend.logEvent(ACTION_TYPE_UNSTRUCTURED_FEEDBACK_AFTER_LONG_SUGGESTION, bundle);
    }

    //Short suggestion feedback
    public void wifiExpertAskForFeedbackAfterShortSuggestion() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(ACTION_TYPE_ASK_FOR_FEEDBACK_AFTER_SHORT_SUGGESTION, bundle);
    }

    public void wifiExpertPositiveFeedbackAfterShortSuggestion() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(ACTION_TYPE_POSITIVE_FEEDBACK_AFTER_SHORT_SUGGESTION, bundle);
    }

    public void wifiExpertNegativeFeedbackAfterShortSuggestion() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(ACTION_TYPE_NEGATIVE_FEEDBACK_AFTER_SHORT_SUGGESTION, bundle);
    }

    public void wifiExpertNoFeedbackAfterShortSuggestion() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(ACTION_TYPE_NO_FEEDBACK_AFTER_SHORT_SUGGESTION, bundle);
    }

    public void wifiExpertUnstructuredFeedbackAfterShortSuggestion(String feedback) {
        Bundle bundle = new Bundle();
        bundle.putString(FEEDBACK_UNSTRUCTURED, feedback);
        dobbyAnalyticsBackend.logEvent(ACTION_TYPE_UNSTRUCTURED_FEEDBACK_AFTER_SHORT_SUGGESTION, bundle);
    }

    //Feedback after wifi check
    public void wifiExpertAskForFeedbackAfterWifiCheck() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(ACTION_TYPE_ASK_FOR_FEEDBACK_AFTER_WIFI_CHECK, bundle);
    }

    public void wifiExpertPositiveFeedbackAfterWifiCheck() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(ACTION_TYPE_POSITIVE_FEEDBACK_AFTER_WIFI_CHECK, bundle);
    }

    public void wifiExpertNegativeFeedbackAfterWifiCheck() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(ACTION_TYPE_NEGATIVE_FEEDBACK_AFTER_WIFI_CHECK, bundle);
    }

    public void wifiExpertNoFeedbackAfterWifiCheck() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(ACTION_TYPE_NO_FEEDBACK_AFTER_WIFI_CHECK, bundle);
    }

    public void wifiExpertUnstructuredFeedbackAfterWifiCheck(String feedback) {
        Bundle bundle = new Bundle();
        bundle.putString(FEEDBACK_UNSTRUCTURED, feedback);
        dobbyAnalyticsBackend.logEvent(ACTION_TYPE_UNSTRUCTURED_FEEDBACK_AFTER_WIFI_CHECK, bundle);
    }


    public void wifiExpertDailyHeartBeat(String uid, double lat, double lon) {
        Bundle bundle = new Bundle();
        bundle.putString(PARAM_UID, uid);
        bundle.putDouble(PARAM_LATITUDE, lat);
        bundle.putDouble(PARAM_LONGITUDE, lon);
        dobbyAnalyticsBackend.logEvent(DAILY_HEARTBEAT_EVENT, bundle);
    }

    //Expert button events
    public void contactExpertEvent() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(CONTACT_EXPERT_BUTTON_CLICKED, bundle);
    }

    public void shareResultsEvent() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(SHARE_RESULT_BUTTON_CLICKED, bundle);
    }

    public void showETAToUser(String text) {
        Bundle bundle = new Bundle();
        bundle.putString(PARAM_ETA, text);
        dobbyAnalyticsBackend.logEvent(SHOW_ETA_TO_USER, bundle);
    }

    public void onBoardingFinishClicked() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(ONBOARDING_FINISH_CLICKED, bundle);
    }

    public void onBoardingSkipClicked() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(ONBOARDING_SKIP_CLICKED, bundle);
    }

    public void firstTimeExpertChatContinueClicked() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(EXPERT_CHAT_CONTINUE_BUTTON_CLICKED, bundle);
    }

    public void onBoardingNextClicked() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(ONBOARDING_NEXT_CLICKED, bundle);
    }

    public void onBoardingShown() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(ONBOARDING_SHOWN, bundle);
    }

    public void feedbackButtonClicked() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(EXPERT_FEEDBACK_BUTTON_CLICKED, bundle);
    }


    //Expert says events
    public void setExpertSaysIssueResolved() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(EXPERT_SAYS_ISSUE_RESOLVED, bundle);
    }
    public void setExpertSaysIssueUnResolved() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(EXPERT_SAYS_ISSUE_UNRESOLVED, bundle);
    }
    public void setExpertSaysMoreDataNeeded() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(EXPERT_SAYS_MORE_DATA_NEEDED, bundle);
    }
    public void setExpertSaysGoodInferencing() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(EXPERT_SAYS_GOOD_INFERENCING, bundle);
    }
    public void setExpertSaysBadInferencing() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(EXPERT_SAYS_BAD_INFERENCING, bundle);
    }
    public void setExpertSaysUserDroppedOff() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(EXPERT_SAYS_USER_DROPPED_OFF, bundle);
    }
    public void setExpertSaysInferencingCanBeBetter() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(EXPERT_SAYS_INFERENCING_CAN_BE_BETTER, bundle);
    }

    public void setWifiServiceBindingFailed() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(WIFI_SERVICE_BINDING_FAILED, bundle);
    }

    public void setWifiServiceBindingSuccessful() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(WIFI_SERVICE_BINDING_SUCCESSFUL, bundle);
    }

    public void setWifiServiceDisconnected() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(WIFI_SERVICE_DISCONNECTED, bundle);
    }

    public void setWifiServiceConnected() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(WIFI_SERVICE_CONNECTED, bundle);
    }

    public void setWifiServiceUnbindingCalled() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(WIFI_SERVICE_UNBINDING_CALLED, bundle);
    }

    public void setWifiServiceUnavailableForRepair() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(WIFI_SERVICE_BINDING_NOT_AVAILABLE, bundle);
    }

    public void setWifiServiceBindingSecurityException() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(WIFI_SERVICE_SECURITY_EXCEPTION, bundle);
    }

    //Service related
    public void setWifiServiceStarted() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(WIFI_SERVICE_STARTED, bundle);
    }

    public void setWifiServiceStopped() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(WIFI_SERVICE_STOPPED, bundle);
    }

    public void setWifiServiceNotificationShown() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(WIFI_SERVICE_NOTIFICATION_SHOWN, bundle);
    }

    public void setWifiServiceOnBoardingShown() {
        Bundle bundle = new Bundle();
        dobbyAnalyticsBackend.logEvent(WIFI_SERVICE_ON_BOARDING_SHOWN, bundle);
    }



}
