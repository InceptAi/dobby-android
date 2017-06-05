package com.inceptai.dobby;

import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.inceptai.dobby.ai.DataInterpreter;

import java.util.ArrayList;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Provides an API for logging analytics events.
 */
@Singleton
public class DobbyAnalytics {
    private static final String RUN_TESTS_ITEM = "run_tests";
    private static final String BRIEF_SUGGESTIONS_ITEM = "brief_suggestions";

    private static final String FAB_CLICKED_CONTENT = "fab_clicked";
    private static final String AUTO_CONTENT = "auto_content";
    private static final String MORE_SUGGESTION_CLICKED = "more_suggestions_clicked";
    private static final String WIFI_DOC_FRAGMENT_ENTERED = "wifidoc_opened";
    private static final String ABOUT_DIALOG_SHOWN = "about_dialog_shown";
    private static final String FEEDBACK_DIALOG_SHOWN = "feedback_dialog_shown";
    private static final String TESTS_CANCELLED = "tests_cancelled";

    private static final String PARAM_SUGGESTION_TEXT = "suggestion_text";
    private static final String PARAM_SUGGESTION_TITLE = "suggestion_title";
    private static final String PARAM_GRADE_TEXT = "grade_text";
    private static final String PARAM_GRADE_JSON = "grade_json";

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


    FirebaseAnalytics firebaseAnalytics;

    @Inject
    DobbyAnalytics(DobbyApplication dobbyApplication) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(dobbyApplication.getApplicationContext());
    }

    public void wifiDocFragmentEntered() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, WIFI_DOC_FRAGMENT_ENTERED);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    public void runTestsClicked() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, RUN_TESTS_ITEM);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, FAB_CLICKED_CONTENT);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    public void briefSuggestionsShown(String suggestionText) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, BRIEF_SUGGESTIONS_ITEM);
        bundle.putString(PARAM_SUGGESTION_TEXT, suggestionText);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, AUTO_CONTENT);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    public void moreSuggestionsShown(String title, ArrayList<String> longSuggestionList) {
        Bundle bundle = new Bundle();
        bundle.putString(PARAM_SUGGESTION_TITLE, title);
        bundle.putStringArrayList(PARAM_SUGGESTION_TEXT, longSuggestionList);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, MORE_SUGGESTION_CLICKED);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    public void aboutShown() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, ABOUT_DIALOG_SHOWN);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    public void feedbackFormShown() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, FEEDBACK_DIALOG_SHOWN);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    public void testsCancelled() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, TESTS_CANCELLED);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    public void bandwidthGrade(DataInterpreter.BandwidthGrade grade) {
        Bundle bundle = new Bundle();
        bundle.putString(PARAM_GRADE_TEXT, grade.toString());
        bundle.putString(PARAM_GRADE_JSON, grade.toJson());
        firebaseAnalytics.logEvent(BANDWIDTH_GRADE_EVENT, bundle);
    }

    public void wifiGrade(DataInterpreter.WifiGrade grade) {
        Bundle bundle = new Bundle();
        bundle.putString(PARAM_GRADE_TEXT, grade.toString());
        bundle.putString(PARAM_GRADE_JSON, grade.toJson());
        firebaseAnalytics.logEvent(WIFI_GRADE_EVENT, bundle);
    }

    public void pingGrade(DataInterpreter.PingGrade grade) {
        Bundle bundle = new Bundle();
        bundle.putString(PARAM_GRADE_TEXT, grade.toString());
        bundle.putString(PARAM_GRADE_JSON, grade.toJson());
        firebaseAnalytics.logEvent(PING_GRADE_EVENT, bundle);
    }

    //Wifi Expert events
    public void wifiExpertFragmentEntered() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, WIFI_EXPERT_FRAGMENT_ENTERED);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        firebaseAnalytics.logEvent(WIFI_EXPERT_FRAGMENT_ENTERED, bundle);
    }

    public void wifiExpertWelcomeMessageShown() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, ACTION_TYPE_WELCOME_TAKEN);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        firebaseAnalytics.logEvent(ACTION_TYPE_WELCOME_TAKEN, bundle);
    }

    public void wifiExpertRunTestButtonClicked() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, WIFI_EXPERT_RUN_TESTS_BUTTON_CLICKED);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        firebaseAnalytics.logEvent(WIFI_EXPERT_RUN_TESTS_BUTTON_CLICKED, bundle);
    }

    public void wifiExpertCheckWifiButtonClicked() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, WIFI_EXPERT_CHECK_WIFI_BUTTON_CLICKED);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        firebaseAnalytics.logEvent(WIFI_EXPERT_CHECK_WIFI_BUTTON_CLICKED, bundle);
    }

    public void wifiExpertSlowInternetButtonClicked() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, WIFI_EXPERT_SLOW_INTERNET_BUTTON_CLICKED);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        firebaseAnalytics.logEvent(WIFI_EXPERT_SLOW_INTERNET_BUTTON_CLICKED, bundle);

    }

    public void wifiExpertMoreDetailsButtonClicked() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, WIFI_EXPERT_MORE_DETAILS_BUTTON_CLICKED);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        firebaseAnalytics.logEvent(WIFI_EXPERT_MORE_DETAILS_BUTTON_CLICKED, bundle);
    }

    public void wifiExpertDeclineRunningFullBandwidthTestsClicked() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, WIFI_EXPERT_DECLINE_RUNNING_FULL_BANDWIDTH_TESTS_CLICKED);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        firebaseAnalytics.logEvent(WIFI_EXPERT_DECLINE_RUNNING_FULL_BANDWIDTH_TESTS_CLICKED, bundle);
    }

    public void wifiExpertAcceptRunningFullBandwidthTestsClicked() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, WIFI_EXPERT_ACCEPT_RUNNING_FULL_BANDWIDTH_TESTS_CLICKED);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        firebaseAnalytics.logEvent(WIFI_EXPERT_ACCEPT_RUNNING_FULL_BANDWIDTH_TESTS_CLICKED, bundle);

    }

    public void wifiExpertCancelBandwidthTestsClicked() {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, WIFI_EXPERT_CANCEL_BANDWIDTH_TEST_BUTTON_CLICKED);
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        firebaseAnalytics.logEvent(WIFI_EXPERT_CANCEL_BANDWIDTH_TEST_BUTTON_CLICKED, bundle);
    }

    //Wifi expert actions
    public void wifiExpertRunningBandwidthTests() {
        Bundle bundle = new Bundle();
        firebaseAnalytics.logEvent(ACTION_TYPE_BANDWIDTH_TEST_TAKEN, bundle);
    }

    public void wifiExpertWifiCheck() {
        Bundle bundle = new Bundle();
        firebaseAnalytics.logEvent(ACTION_TYPE_WIFI_CHECK_TAKEN, bundle);
    }

    public void wifiExpertCancelBandwidthTest() {
        Bundle bundle = new Bundle();
        firebaseAnalytics.logEvent(ACTION_TYPE_CANCEL_BANDWIDTH_TEST_TAKEN, bundle);
    }

    public void wifiExpertDiagnoseSlowInternet() {
        Bundle bundle = new Bundle();
        firebaseAnalytics.logEvent(ACTION_TYPE_DIAGNOSE_SLOW_INTERNET_TAKEN, bundle);
    }

    public void wifiExpertBwPingWifiTest() {
        Bundle bundle = new Bundle();
        firebaseAnalytics.logEvent(ACTION_TYPE_BANDWIDTH_PING_WIFI_TESTS_TAKEN, bundle);
    }

    public void wifiExpertShowShortSuggestion(String suggestionText) {
        Bundle bundle = new Bundle();
        bundle.putString(PARAM_SUGGESTION_TEXT, suggestionText);
        firebaseAnalytics.logEvent(ACTION_TYPE_SHOW_SHORT_SUGGESTION_TAKEN, bundle);
    }

    public void wifiExpertDefaultFallbackAction() {
        Bundle bundle = new Bundle();
        firebaseAnalytics.logEvent(ACTION_TYPE_DEFAULT_FALLBACK_TAKEN, bundle);
    }

    public void wifiExpertShowLongSuggestion(String suggestionText) {
        Bundle bundle = new Bundle();
        bundle.putString(PARAM_SUGGESTION_TEXT, suggestionText);
        firebaseAnalytics.logEvent(ACTION_TYPE_SHOWING_LONG_SUGGESTION_TAKEN, bundle);
    }

    public void wifiExpertShowWifiAnalysis() {
        Bundle bundle = new Bundle();
        firebaseAnalytics.logEvent(ACTION_TYPE_SHOW_WIFI_ANALYSIS_TAKEN, bundle);
    }

    public void wifiExpertListDobbyFunctions() {
        Bundle bundle = new Bundle();
        firebaseAnalytics.logEvent(ACTION_TYPE_LIST_DOBBY_FUNCTIONS_TAKEN, bundle);
    }

    public void wifiExpertAskForBwTestsAfterWifiCheck() {
        Bundle bundle = new Bundle();
        firebaseAnalytics.logEvent(ACTION_TYPE_ASK_FOR_BW_TESTS_TAKEN, bundle);
    }

    public void wifiExpertAskForLongSuggestion() {
        Bundle bundle = new Bundle();
        firebaseAnalytics.logEvent(ACTION_TYPE_ASK_FOR_DETAILED_SUGGESTIONS, bundle);
    }

    public void wifiExpertDeclineLongSuggestion() {
        Bundle bundle = new Bundle();
        firebaseAnalytics.logEvent(ACTION_TYPE_DECLINE_DETAILED_SUGGESTIONS, bundle);
    }

    public void wifiExpertWifiCardShown() {
        Bundle bundle = new Bundle();
        firebaseAnalytics.logEvent(ACTION_TYPE_WIFI_CARD_SHOWN, bundle);
    }

    public void wifiExpertBandwidthCardShown() {
        Bundle bundle = new Bundle();
        firebaseAnalytics.logEvent(ACTION_TYPE_BANDWIDTH_CARD_SHOWN, bundle);
    }
}