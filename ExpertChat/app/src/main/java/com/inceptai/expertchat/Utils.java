package com.inceptai.expertchat;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by arunesh on 6/6/17.
 */

public class Utils {
    public static final String PREF_EXPERT_AVATAR = "expertName";
    public static final String DEFAULT_EXPERT_AVATAR = "Jack";
    public static final String TAG = "ExpertChat";
    public static final String EMPTY_STRING = "";

    // Preference string keys
    public static final String SELECTED_FLAVOR = "flavor";
    public static final String SELECTED_USER_UUID = "userUuid";
    public static final String SELECTED_BUILD_TYPE = "buildType";

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
}
