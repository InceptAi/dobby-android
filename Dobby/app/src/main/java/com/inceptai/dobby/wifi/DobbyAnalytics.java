package com.inceptai.dobby.wifi;

import android.content.Context;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.inceptai.dobby.DobbyApplication;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Provides an API for logging analytics events.
 */
@Singleton
public class DobbyAnalytics {

    FirebaseAnalytics firebaseAnalytics;

    @Inject
    DobbyAnalytics(DobbyApplication dobbyApplication) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(dobbyApplication.getApplicationContext());
    }


    public void wifiDocFragmentEntered() {

    }

    public void runTestsClicked() {

    }

    public void briefSuggestionsShown() {

    }

    public void moreSuggestionsShown() {

    }

    public void aboutShown() {

    }

    public void feedbackFormShown() {

    }

    public void testsCancelled() {

    }
}
