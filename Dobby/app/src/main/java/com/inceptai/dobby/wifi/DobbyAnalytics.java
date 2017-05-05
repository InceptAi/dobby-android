package com.inceptai.dobby.wifi;

import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.ai.DataInterpreter;

import java.util.ArrayList;
import java.util.List;

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
}