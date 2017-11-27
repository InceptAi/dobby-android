package com.inceptai.dobby.actions;

import android.content.Context;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import com.inceptai.dobby.actions.api.APIActionLibraryClient;
import com.inceptai.dobby.actions.ui.NeoServiceClient;
import com.inceptai.dobby.utils.DobbyLog;
import com.inceptai.neopojos.ActionDetails;
import com.inceptai.neoservice.Utils;
import com.inceptai.neoservice.uiactions.UIActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.Action;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
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
        void actionsAvailable(List<ActionDetails> actionDetailsList, int statusCode);
        void actionStarted(String query, String appName);
        void actionFinished(String query, String appName, String status, ActionResult apiActionResult, UIActionResult uiActionResult);
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
    public void fetchUIActionForSettings(String query) {
        final SettableFuture<UIActionResult> fetchFuture = neoServiceClient.fetchUIActionsForSettings(query);
        if (fetchFuture == null) {
            if (actionTakerCallback != null) {
                actionTakerCallback.actionsAvailable(
                        new ArrayList<ActionDetails>(),
                        UIActionResult.UIActionResultCodes.GENERAL_ERROR);
            }
            return;
        }
        fetchFuture.addListener(new Runnable() {
            @Override
            public void run() {
                UIActionResult fetchResult = null;
                try {
                    fetchResult = fetchFuture.get();
                } catch (InterruptedException | ExecutionException e) {
                    if (actionTakerCallback != null) {
                       actionTakerCallback.actionsAvailable(
                               new ArrayList<ActionDetails>(),
                               UIActionResult.UIActionResultCodes.EXCEPTION_WHILE_WAITING_FOR_UI_ACTION_FETCH_FROM_SERVER);
                    }
                }
                if (UIActionResult.isSuccessful(fetchResult)) {
                    List<ActionDetails> actionDetailsList = (List<ActionDetails>)fetchResult.getPayload();
                    actionDetailsList = actionDetailsList == null ? new ArrayList<ActionDetails>() : actionDetailsList;
                    if (actionTakerCallback != null) {
                        actionTakerCallback.actionsAvailable(actionDetailsList, fetchResult.getStatus());
                    }
                } else {
                    if (actionTakerCallback != null) {
                        @UIActionResult.UIActionResultCodes int errorCode =
                                fetchResult != null ? fetchResult.getStatus() :
                                        UIActionResult.UIActionResultCodes.UNKNOWN;
                        actionTakerCallback.actionsAvailable(new ArrayList<ActionDetails>(), errorCode);
                    }
                }
            }
        }, executorService);
    }

    public void performUIActionForSettings(final ActionDetails actionDetails) {
        final SettableFuture uiActionResultSettableFuture = neoServiceClient.performUIActionForSettings(actionDetails);
        final String query = actionDetails != null && actionDetails.getActionIdentifier() != null ?
                actionDetails.getActionIdentifier().getActionDescription() : Utils.EMPTY_STRING;
        final String packageName = actionDetails != null && actionDetails.getActionIdentifier() != null &&
                actionDetails.getActionIdentifier().getScreenIdentifier() != null ?
                actionDetails.getActionIdentifier().getScreenIdentifier().getPackageName() : Utils.EMPTY_STRING;
        if (actionTakerCallback != null) {
            actionTakerCallback.actionStarted(query, packageName);
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
                                null,
                                uiActionResult);
                    } else {
                        actionTakerCallback.actionFinished(
                                query,
                                packageName,
                                "NULL RESULT",
                                null,
                                null);
                    }
                }
            }
        }, executorService);
    }


    public void takeAction(final String query, final String appName, boolean apiAction, final boolean forceAppRelaunch) {
        boolean actionInitiated = false;
        if (apiAction) {
            actionInitiated = apiActionLibraryClient.takeAPIAction(query);
        }
        if (!actionInitiated) {
            //Check if accessibility permission is given
            final SettableFuture uiActionResultSettableFuture = neoServiceClient.fetchAndPerformTopUIActions(query, Utils.nullOrEmpty(appName) ? Utils.SETTINGS_APP_NAME : appName, forceAppRelaunch);
            if (actionTakerCallback != null) {
                actionTakerCallback.actionStarted(query, appName);
            }
            uiActionResultSettableFuture.addListener(new Runnable() {
                @Override
                public void run() {
                    UIActionResult uiActionResult = null;
                    try {
                        uiActionResult = (UIActionResult)uiActionResultSettableFuture.get();
                    } catch (InterruptedException|ExecutionException|CancellationException e) {
                        DobbyLog.v("Exception while waiting for UI Action Result");
                    }
                    if (uiActionResult != null &&
                            uiActionResult.getStatus() == UIActionResult.UIActionResultCodes.NO_ACTIONS_AVAILABLE &&
                            !forceAppRelaunch) {
                        takeAction(query, appName, false, true);
                        return;
                    }
                    if (actionTakerCallback != null) {
                        if (uiActionResult != null) {
                            actionTakerCallback.actionFinished(
                                    uiActionResult.getQuery(),
                                    uiActionResult.getPackageName(),
                                    uiActionResult.getStatusString(),
                                    null,
                                    uiActionResult);
                        } else {
                            actionTakerCallback.actionFinished(
                                    query,
                                    appName,
                                    "NULL RESULT",
                                    null,
                                    null);
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
                    actionResult,
                    null);
        }
    }
}
