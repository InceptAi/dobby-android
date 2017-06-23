package com.inceptai.expertchat;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by arunesh on 6/6/17.
 */

public class Utils {

    public static final String NOTIFICATION_USER_UUID = "notificationUserId";
    public static final String PREF_EXPERT_AVATAR = "expertName";
    public static final String DEFAULT_EXPERT_AVATAR = "Jack";
    public static final String TAG = "ExpertChat";
    public static final String EMPTY_STRING = "";

    // Preference string keys
    public static final String SELECTED_FLAVOR = "flavor";
    public static final String SELECTED_USER_UUID = "userUuid";
    public static final String SELECTED_BUILD_TYPE = "buildType";
    public static final String PREF_FCM_TOKEN = "fcmToken";
    public static final String PREF_EXPERT_ONLINE = "isExpertOnline";

    public static final String WIFIDOC_FLAVOR = "wifidoc";
    public static final String DOBBY_FLAVOR = "dobby";
    public static final String BUILD_TYPE_DEBUG = "debug";
    public static final String BUILD_TYPE_RELEASE = "release";

    private Utils() {}

    public static String readSharedSetting(Context ctx, String settingName, String defaultValue) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);
        return sharedPref.getString(settingName, defaultValue);
    }

    public static void saveSharedSetting(Context ctx, String settingName, String settingValue) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(settingName, settingValue);
        editor.apply();
    }

    public static String getExpertAvatar(Context context) {
        return readSharedSetting(context, PREF_EXPERT_AVATAR, DEFAULT_EXPERT_AVATAR);
    }

    public static void saveExpertAvatar(Context context, String expertName) {
        saveSharedSetting(context, PREF_EXPERT_AVATAR, expertName);
    }

    public static String getFlavorFromFcmIdPath(String fcmIdPath) {
        if (fcmIdPath == null || fcmIdPath.isEmpty()) return EMPTY_STRING;
        if (fcmIdPath.contains(WIFIDOC_FLAVOR)) return WIFIDOC_FLAVOR;
        if (fcmIdPath.contains(DOBBY_FLAVOR)) return DOBBY_FLAVOR;
        return EMPTY_STRING;
    }

    public static String getBuildTypeFromFcmIdPath(String fcmIdPath) {
        if (fcmIdPath == null || fcmIdPath.isEmpty()) return EMPTY_STRING;
        if (fcmIdPath.contains(BUILD_TYPE_DEBUG)) return BUILD_TYPE_DEBUG;
        if (fcmIdPath.contains(BUILD_TYPE_RELEASE)) return BUILD_TYPE_RELEASE;
        return EMPTY_STRING;
    }

    public static String unknownIfEmpty(String orig) {
        if (orig == null || orig.isEmpty()) {
            return "UNKNOWN";
        }
        return orig;
    }
}
