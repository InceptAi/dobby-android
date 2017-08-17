package com.inceptai.dobby.feedback;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.dobby.BuildConfig;
import com.inceptai.dobby.RemoteConfig;
import com.inceptai.dobby.utils.Utils;

import java.util.Arrays;
import java.util.concurrent.Executor;

/**
 * Created by vivek on 8/16/17.
 */

public class RatingsManager {
    public static final String YES_PREF = "yes";
    public static final String NO_PREF = "no";
    public static final String LATER_PREF = "later";

    private static final String RATING_PREFERENCE = "rating_pref";
    private static final String LAST_ASKED_FOR_RATING = "rating_pref_ts";
    private static final String UNSET_PREF = "unset";
    private static final String PREF_REMOTE_FLAG_FOR_ENABLING_RATINGS = "remote_rating_flag";
    private static final String[] VALID_PREFS = new String[] {YES_PREF, NO_PREF, LATER_PREF};
    private static final long MIN_TIME_BEFORE_ASKING_AGAIN_MS = 7 * 24 * 60 * 60 * 1000;

    private Context context;
    private RemoteConfig remoteConfig;
    private Executor executor;

    public RatingsManager(Context context, RemoteConfig remoteConfig, Executor executor) {
        this.context = context;
        this.remoteConfig = remoteConfig;
        this.executor = executor;
        startConfigFetchAndWait();
    }

    public void launchAppStorePageForRatingTheApp() {
        saveRatingPreference(RatingsManager.YES_PREF);
        String uriToParse = "market://details?id=" + BuildConfig.APPLICATION_ID;
        context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(uriToParse)));
    }

    public boolean shouldBeAllowedToAskForRating() {
        long lastPrefUpdatedAtMs = getLastTimeRatingPrefUpdated();
        long timeSinceLastPrefUpdateMs = System.currentTimeMillis() - lastPrefUpdatedAtMs;
        if (!getRatingEnabledFlag() || hasUserSaidYesOrNoForRating()) {
            return false;
        } else if (getRatingPreference().equals(LATER_PREF) &&
                lastPrefUpdatedAtMs > 0 &&
                timeSinceLastPrefUpdateMs < MIN_TIME_BEFORE_ASKING_AGAIN_MS) {
            return false;
        }
        return true;
    }

    public boolean saveRatingPreference(String preference) {
        if (!Arrays.asList(VALID_PREFS).contains(preference)) {
            return false;
        }
        Utils.saveSharedSetting(context, RATING_PREFERENCE, preference);
        Utils.saveSharedSetting(context, LAST_ASKED_FOR_RATING, System.currentTimeMillis());
        return true;
    }

    //Private stuff
    private void startConfigFetchAndWait() {
        ListenableFuture<RemoteConfig> remoteConfigListenableFuture =  remoteConfig.fetchAsync();
        remoteConfigListenableFuture.addListener(new Runnable() {
            @Override
            public void run() {
                //We have the server info: lets start the service
                saveRatingEnabledFlag(remoteConfig.getRatingsFlag());
            }
        }, executor);
    }

    private boolean hasUserSaidYesOrNoForRating() {
        return getRatingPreference().equals(YES_PREF) || getRatingPreference().equals(NO_PREF);
    }



    private String getRatingPreference() {
        return Utils.readSharedSetting(context, RATING_PREFERENCE, UNSET_PREF);
    }

    private long getLastTimeRatingPrefUpdated() {
        return Utils.readSharedSetting(context, LAST_ASKED_FOR_RATING, 0);
    }

    private boolean getRatingEnabledFlag() {
        return Boolean.valueOf(Utils.readSharedSetting(context, PREF_REMOTE_FLAG_FOR_ENABLING_RATINGS, Utils.FALSE_STRING));
    }

    private void saveRatingEnabledFlag(boolean ratingsEnabled) {
        String valueToUpdate = ratingsEnabled ? Utils.TRUE_STRING : Utils.FALSE_STRING;
        Utils.saveSharedSetting(context, PREF_REMOTE_FLAG_FOR_ENABLING_RATINGS, valueToUpdate);
    }


}
