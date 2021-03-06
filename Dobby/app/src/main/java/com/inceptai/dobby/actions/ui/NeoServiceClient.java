package com.inceptai.dobby.actions.ui;

import android.content.Context;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.RemoteConfig;
import com.inceptai.dobby.utils.DobbyLog;
import com.inceptai.dobby.utils.Utils;
import com.inceptai.neopojos.ActionDetails;
import com.inceptai.neoservice.NeoService;
import com.inceptai.neoservice.uiactions.UIActionResult;

import java.util.concurrent.Executor;

/**
 * Created by vivek on 7/27/17.
 */
public class NeoServiceClient {
    private static final String DEFAULT_NEO_SERVER_ADDRESS = "ws://dobby1743.duckdns.org:8080/";
    private static final String PREF_SERVER_ADDRESS = "neo_server_address";
    private static final int MAX_CHARACTERS_FOR_STATUS = 120;
    public static final String SETTINGS_APP_NAME = "settings";

    private RemoteConfig remoteConfig;
    private Executor executor;
    private Context context;
    private NeoService neoService;
    private NeoService.Callback neoServiceCallback;

    public NeoServiceClient(RemoteConfig remoteConfig,
                            DobbyThreadpool dobbyThreadpool,
                            DobbyApplication dobbyApplication,
                            NeoService.Callback neoServiceCallback) {
        this.remoteConfig = remoteConfig;
        executor = dobbyThreadpool.getExecutor();
        context = dobbyApplication.getApplicationContext();
        DobbyLog.v("UIM: Starting neoService with server: " + remoteConfig.getNeoServer() + " and UUID " + dobbyApplication.getUserUuid());
        neoService = new NeoService(getServerAddress(), dobbyApplication.getUserUuid(), context, neoServiceCallback);
    }

    public void startService(boolean enableOverlay) {
        neoService.startService(enableOverlay);
    }

    public void stopService() {
        neoService.stopService();
    }

    public void toggleNeoService() {
        if (neoService.isServiceRunning()) {
            stopService();
        } else {
            startService(true);
        }
    }

    public void setStatus(String message) {
        String trailer = Utils.EMPTY_STRING;
        if (message != null && !message.isEmpty()) {
            if (message.length() > MAX_CHARACTERS_FOR_STATUS) {
                trailer = "...";
            }
            neoService.updateStatus(message.substring(0, Math.min(MAX_CHARACTERS_FOR_STATUS, message.length())) + trailer);
        }
    }

    public SettableFuture fetchAndPerformTopUIActions(String query, String appName, boolean forceAppRelaunch) {
        return neoService.fetchAndPerformTopUIAction(query, appName, forceAppRelaunch);
    }

    public SettableFuture<UIActionResult> fetchUIActionsForSettings(String query) {
        return neoService.fetchUIActionForSettings(query);
    }

    public SettableFuture performUIActionForSettings(ActionDetails actionDetails) {
        return neoService.performUIActionForSettings(actionDetails);
    }

    public void takeUserToAccessibilitySettings() {
        NeoService.showAccessibilitySettings(context);
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
