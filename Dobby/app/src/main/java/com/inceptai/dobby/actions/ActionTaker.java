package com.inceptai.dobby.actions;

import android.content.Context;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import com.inceptai.dobby.actions.api.APIActionLibraryClient;
import com.inceptai.dobby.actions.ui.NeoServiceClient;
import com.inceptai.dobby.utils.DobbyLog;
import com.inceptai.neoservice.Utils;
import com.inceptai.neoservice.uiactions.UIActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.Action;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by vivek on 11/15/17.
 */

public class ActionTaker implements APIActionLibraryClient.ActionLibraryClientCallback {
    private NeoServiceClient neoServiceClient;
    private APIActionLibraryClient apiActionLibraryClient;
    private ExecutorService executorService;
    private ActionTakerCallback actionTakerCallback;

    public interface ActionTakerCallback {
        void actionStarted(String query, String appName);
        void actionFinished(String query, String appName, String status, boolean isSuccessful);
    }

    public ActionTaker(Context context, ExecutorService executorService,
                       ListeningScheduledExecutorService listeningScheduledExecutorService,
                       ScheduledExecutorService scheduledExecutorService,
                       NeoServiceClient neoServiceClient, ActionTakerCallback actionTakerCallback) {
        this.neoServiceClient = neoServiceClient;
        this.executorService = executorService;
        this.apiActionLibraryClient = new APIActionLibraryClient(
                context,
                executorService,
                listeningScheduledExecutorService,
                scheduledExecutorService,
                this);
        this.actionTakerCallback = actionTakerCallback;
    }


    //Expert system service callbacks
    public void takeAction(final String query, final String appName, boolean apiAction) {
        boolean actionInitiated = false;
        if (apiAction) {
            actionInitiated = apiActionLibraryClient.takeAPIAction(query);
        }
        if (!actionInitiated) {
            final SettableFuture uiActionResultSettableFuture = neoServiceClient.fetchUIActions(query, Utils.nullOrEmpty(appName) ? Utils.SETTINGS_APP_NAME : appName);
            if (actionTakerCallback != null) {
                actionTakerCallback.actionStarted(query, appName);
            }
            uiActionResultSettableFuture.addListener(new Runnable() {
                @Override
                public void run() {
                    UIActionResult uiActionResult = null;
                    try {
                        uiActionResult = (UIActionResult)uiActionResultSettableFuture.get();
                    } catch (InterruptedException|ExecutionException e) {
                        DobbyLog.v("Exception while waiting for UI Action Result");
                    }
                    if (actionTakerCallback != null) {
                        if (uiActionResult != null) {
                            actionTakerCallback.actionFinished(
                                    uiActionResult.getQuery(),
                                    uiActionResult.getPackageName(),
                                    uiActionResult.getStatusString(),
                                    UIActionResult.isSuccessful(uiActionResult));
                        } else {
                            actionTakerCallback.actionFinished(
                                    query,
                                    appName,
                                    "NULL RESULT",
                                    false);
                        }
                    }

                }
            }, executorService);
        }
    }

    @Override
    public void actionStarted(Action action) {
        if (actionTakerCallback != null) {
            actionTakerCallback.actionStarted(action.getName(), Utils.SETTINGS_APP_NAME);
        }
    }

    @Override
    public void actionFinished(Action action, ActionResult actionResult) {
        if (actionTakerCallback != null) {
            String statusString = actionResult != null ? actionResult.getStatusString() : "NULL RESULT";
            actionTakerCallback.actionFinished(
                    action.getName(),
                    Utils.SETTINGS_APP_NAME,
                    statusString,
                    ActionResult.isSuccessful(actionResult));
        }
    }
}
