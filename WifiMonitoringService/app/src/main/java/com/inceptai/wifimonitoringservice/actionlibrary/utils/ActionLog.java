package com.inceptai.wifimonitoringservice.actionlibrary.utils;

/**
 * Created by arunesh on 4/12/17.
 */

import android.util.Log;

import com.inceptai.wifimonitoringservice.BuildConfig;


/**
 * Logging helper class that logs to Firebase.
 */
public class ActionLog {
    private static String TAG = "ActionLibrary";

    public static void i(String message) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, message);
        } else {
            Log.i(TAG, message);
        }
    }

    public static void e(String message) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, message);
        } else {
            Log.e(TAG, message);
        }
    }

    public static void v(String message) {
        if (BuildConfig.DEBUG) {
            Log.v(TAG, message);
        } else {
            Log.v(TAG, message);
        }
    }

    public static void w(String message) {
        if (BuildConfig.DEBUG) {
            Log.w(TAG, message);
        } else {
            Log.w(TAG, message);
        }
    }
}
