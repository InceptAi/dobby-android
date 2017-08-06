package com.inceptai.wifiexpert.analytics;

import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.utils.Utils;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by vivek on 7/3/17.
 */
@Singleton
public class DobbyAnalyticsBackend extends AnalyticsBackend {

    private FirebaseAnalytics firebaseAnalytics;

    @Inject
    DobbyAnalyticsBackend(DobbyApplication dobbyApplication) {
        firebaseAnalytics = FirebaseAnalytics.getInstance(dobbyApplication.getApplicationContext());
        if (firebaseAnalytics != null) {
            if (dobbyApplication.isRunningOnEmulator()) {
                firebaseAnalytics.setUserProperty("isRunningOnEmulator", Utils.TRUE_STRING);
            } else {
                firebaseAnalytics.setUserProperty("isRunningOnEmulator", Utils.FALSE_STRING);
            }
        }
    }

    @Override
    public void logEvent(String eventType, Bundle bundle) {
        firebaseAnalytics.logEvent(eventType, bundle);
    }

    @Override
    public void setUserProperty(String propertyName, String propertyValue) {
        firebaseAnalytics.setUserProperty(propertyName, propertyValue);
    }
}
