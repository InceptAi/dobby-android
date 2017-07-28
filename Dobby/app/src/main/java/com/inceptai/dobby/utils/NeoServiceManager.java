package com.inceptai.dobby.utils;

import android.content.Context;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.RemoteConfig;
import com.inceptai.neoservice.NeoService;

import java.util.concurrent.Executor;

/**
 * Created by vivek on 7/27/17.
 */
public class NeoServiceManager {
    private static final String DEFAULT_NEO_SERVER_ADDRESS = "ws://dobby1743.duckdns.org:8080/";
    private static final String PREF_SERVER_ADDRESS = "neo_server_address";

    private RemoteConfig remoteConfig;
    private Executor executor;
    private Context context;
    private NeoService neoService;
    private NeoService.Callback neoServiceCallback;

    public NeoServiceManager(RemoteConfig remoteConfig,
                             DobbyThreadpool dobbyThreadpool,
                             DobbyApplication dobbyApplication,
                             NeoService.Callback neoServiceCallback) {
        this.remoteConfig = remoteConfig;
        executor = dobbyThreadpool.getExecutor();
        context = dobbyApplication.getApplicationContext();
        DobbyLog.v("UIM: Starting neoService with server: " + remoteConfig.getNeoServer() + " and UUID " + dobbyApplication.getUserUuid());
        neoService = new NeoService(getServerAddress(), dobbyApplication.getUserUuid(), context, neoServiceCallback);
    }

    public void startService() {
        neoService.startService();
    }

    public void stopService() {
        neoService.stopService();
    }

    public void toggleNeoService() {
        if (neoService.isServiceRunning()) {
            stopService();
        } else {
            startService();
        }
    }

    private boolean saveServerAddressIfChanged(String newAddress) {
        String oldAddress = Utils.readSharedSetting(context, PREF_SERVER_ADDRESS, DEFAULT_NEO_SERVER_ADDRESS);
        if (oldAddress.equals(newAddress)) {
            return false;
        }
        //Address changed
        Utils.saveSharedSetting(context, PREF_SERVER_ADDRESS, newAddress);
        return true;
    }

    private String getServerAddress() {
        return Utils.readSharedSetting(context, PREF_SERVER_ADDRESS, DEFAULT_NEO_SERVER_ADDRESS);
    }

    public String fetchServerAddress() {
        startConfigFetchAndWait();
        return getServerAddress();
    }

    public void cleanup() {
        neoService.cleanup();
        executor = null;
    }

    private void startConfigFetchAndWait() {
        ListenableFuture<RemoteConfig> remoteConfigListenableFuture =  remoteConfig.fetchAsync();
        remoteConfigListenableFuture.addListener(new Runnable() {
            @Override
            public void run() {
                //We have the server info: lets start the service
                saveServerAddressIfChanged(remoteConfig.getNeoServer());
            }
        }, executor);
    }
}
