package com.inceptai.dobby.utils;

/**
 * Created by arunesh on 4/12/17.
 */

import android.util.Log;

import com.google.firebase.crash.FirebaseCrash;
import com.inceptai.dobby.BuildConfig;

import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * Logging helper class that logs to Firebase.
 */
public class DobbyLog {

    public static void checkCrashHandler() {
        Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
        Log.i(TAG, "DobbyLog: Current handler:" + handler.getClass().getCanonicalName());
        handler = Thread.currentThread().getUncaughtExceptionHandler();
        Log.i(TAG, "DobbyLog: Current Thread's handler:" + handler.getClass().getCanonicalName());
    }

    public static void i(String message) {
        checkCrashHandler();
        if (BuildConfig.DEBUG) {
            Log.i(TAG, message);
        } else {
            FirebaseCrash.logcat(Log.INFO, TAG, message);
        }
    }

    public static void e(String message) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, message);
        } else {
            FirebaseCrash.logcat(Log.ERROR, TAG, message);
        }
    }

    public static void v(String message) {
        if (BuildConfig.DEBUG) {
            Log.v(TAG, message);
        } else {
            FirebaseCrash.logcat(Log.VERBOSE, TAG, message);
        }
    }

    public static void w(String message) {
        if (BuildConfig.DEBUG) {
            Log.w(TAG, message);
        } else {
            FirebaseCrash.logcat(Log.WARN, TAG, message);
        }
    }
}
