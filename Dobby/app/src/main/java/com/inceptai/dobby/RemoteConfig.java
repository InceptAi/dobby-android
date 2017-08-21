package com.inceptai.dobby;

import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.inceptai.dobby.utils.DobbyLog;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by arunesh on 5/11/17.
 */
@Singleton
public class RemoteConfig {
    private static final String SHOW_INFO_IN_RELEASE = "show_info_in_rb";
    private static final String NEO_SERVER = "neo_server_address";
    private static final String RATINGS_FLAG = "enable_ratings_ask";
    private static final String MIN_APP_OPENS_FOR_RATINGS_FLAG = "min_app_opens_for_rating";
    private static final String ENABLE_SERVICE_BY_DEFAULT = "enable_service_by_default";
    private static final long CACHE_EXPIRATION_SEC = 3600; //1 hour
    private FirebaseRemoteConfig firebaseRemoteConfig;
    private SettableFuture<RemoteConfig> fetchConfigFuture;

    @Inject
    public RemoteConfig() {
        firebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        firebaseRemoteConfig.setConfigSettings(configSettings);
        firebaseRemoteConfig.setDefaults(R.xml.firebase_remote_config_defaults);
    }

    /**
     * Asynchronous operation for fetching the config values from the server.
     */
    public ListenableFuture<RemoteConfig> fetchAsync() {
        if (fetchConfigFuture != null && !fetchConfigFuture.isDone()) {
            return fetchConfigFuture;
        }

        long cacheExpirationSec = CACHE_EXPIRATION_SEC;
        // If your app is using developer mode, cacheExpiration is set to 0, so each fetch will
        // retrieve values from the service.
        if (firebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpirationSec = 0;
        }

        fetchConfigFuture = SettableFuture.create();
        //final SettableFuture<RemoteConfig> future = SettableFuture.create();
        // 12 hours default cache expiration.
        firebaseRemoteConfig.fetch(cacheExpirationSec)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            // Once the config is successfully fetched it must be activated before newly fetched
                            // values are returned.
                            firebaseRemoteConfig.activateFetched();
                        } else {
                            // Fetch failed.
                            DobbyLog.e("Fetch of remote config failed");
                        }
                        // succeeded.
                        fetchConfigFuture.set(RemoteConfig.this);
                    }
                });
        return fetchConfigFuture;
    }

    public boolean showInfoInReleaseBuilds() {
        return firebaseRemoteConfig.getBoolean(SHOW_INFO_IN_RELEASE);
    }

    public String getNeoServer() {
        return firebaseRemoteConfig.getString(NEO_SERVER);
    }

    public boolean getRatingsFlag() {
        return firebaseRemoteConfig.getBoolean(RATINGS_FLAG);
    }

    public long getMinAppOpensForAskingRatingsFlag() {
        return firebaseRemoteConfig.getLong(MIN_APP_OPENS_FOR_RATINGS_FLAG);
    }

    public boolean getIsServiceEnabledByDefaultFlag() {
        return firebaseRemoteConfig.getBoolean(ENABLE_SERVICE_BY_DEFAULT);
    }
}
