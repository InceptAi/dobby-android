package com.inceptai.dobby.utils;

/**
 * Created by arunesh on 4/12/17.
 */

import android.util.Log;

import com.google.firebase.crash.FirebaseCrash;

import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * Logging helper class that logs to Firebase.
 */
public class DobbyLog {

    public static void i(String message) {
        FirebaseCrash.logcat(Log.INFO, TAG, message);
    }

    public static void e(String message) {
        FirebaseCrash.logcat(Log.ERROR, TAG, message);
    }

    public static void v(String message) {
        FirebaseCrash.logcat(Log.VERBOSE, TAG, message);
    }

    public static void w(String message) {
        FirebaseCrash.logcat(Log.WARN, TAG, message);
    }
}
