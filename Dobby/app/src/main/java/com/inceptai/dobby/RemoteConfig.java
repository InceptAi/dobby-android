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

    private FirebaseRemoteConfig firebaseRemoteConfig;

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
        final SettableFuture<RemoteConfig> future = SettableFuture.create();
        // 12 hours default cache expiration.
        firebaseRemoteConfig.fetch()
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
                        future.set(RemoteConfig.this);
                    }
                });
        return future;
    }

    public boolean showInfoInReleaseBuilds() {
        return firebaseRemoteConfig.getBoolean(SHOW_INFO_IN_RELEASE);
    }

    public String getNeoServer() {
        return firebaseRemoteConfig.getString(NEO_SERVER);
    }

    public boolean getRatingsFlag() {
        //String valueReturned = firebaseRemoteConfig.getString(RATINGS_FLAG);
        //return valueReturned.equals(Utils.TRUE_STRING);
        return firebaseRemoteConfig.getBoolean(RATINGS_FLAG);
    }
}
