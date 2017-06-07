package com.inceptai.expertchat;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by arunesh on 6/6/17.
 */

public class Utils {
    public static final String PREFERENCES_FILE = "expert_chat_settings";
    public static final String PREF_EXPERT_AVATAR = "expertName";
    public static final String DEFAULT_EXPERT_AVATAR = "Jack";
    public static final String TAG = "ExpertChat";
    public static final String EMPTY_STRING = "";
    public static final String SELECTED_FLAVOR = "flavor";
    public static final String SELECTED_USER_UUID = "userUuid";

    private Utils() {}

    public static String readSharedSetting(Context ctx, String settingName, String defaultValue) {
        SharedPreferences sharedPref = ctx.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
        return sharedPref.getString(settingName, defaultValue);
    }

    public static void saveSharedSetting(Context ctx, String settingName, String settingValue) {
        SharedPreferences sharedPref = ctx.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE);
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
