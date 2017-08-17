package com.inceptai.dobby.analytics;

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
            setIsUserRunningOnEmulator(dobbyApplication.isRunningOnEmulator());
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

    void setIsFreshInstall(boolean isFreshInstall) {
        if (isFreshInstall) {
            firebaseAnalytics.setUserProperty("firstLaunch", Utils.TRUE_STRING);
        } else {
            firebaseAnalytics.setUserProperty("firstLaunch", null);
        }
    }

    void setIsWifiMonitoringServiceOn(boolean isServiceOn) {
        if (isServiceOn) {
            firebaseAnalytics.setUserProperty("serviceOn", Utils.TRUE_STRING);
        } else {
            firebaseAnalytics.setUserProperty("serviceOn", null);
        }
    }

    void setIsRepairSuccessfulForThisUser(boolean isRepairSuccessfulForThisUser) {
        String propertyValue = isRepairSuccessfulForThisUser ? Utils.TRUE_STRING : Utils.FALSE_STRING;
        firebaseAnalytics.setUserProperty("repairSuccess", propertyValue);
    }

    void setUserSaidAppWasHelpful(boolean didUserSayAppWasHelpful) {
        String propertyValue = didUserSayAppWasHelpful ? Utils.TRUE_STRING : Utils.FALSE_STRING;
        firebaseAnalytics.setUserProperty("userSaidAppHelpful", propertyValue);
    }

    void setUserSawDetailedSuggestions(boolean didUserSeeDetailedSuggestions) {
        String propertyValue = didUserSeeDetailedSuggestions ? Utils.TRUE_STRING : Utils.FALSE_STRING;
        firebaseAnalytics.setUserProperty("sawDetailedSuggestions", propertyValue);
    }

    void setUserSawShortSuggestions(boolean didUserSeeShortSuggestions) {
        String propertyValue = didUserSeeShortSuggestions ? Utils.TRUE_STRING : Utils.FALSE_STRING;
        firebaseAnalytics.setUserProperty("sawShortSuggestion", propertyValue);
    }


    private void setIsUserRunningOnEmulator(boolean isUserRunningOnEmulator) {
        if (isUserRunningOnEmulator) {
            firebaseAnalytics.setUserProperty("isRunningOnEmulator", Utils.TRUE_STRING);
        } else {
            firebaseAnalytics.setUserProperty("isRunningOnEmulator", Utils.FALSE_STRING);
        }
    }

}
