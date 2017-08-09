package com.inceptai.wifiexpertsystem.expertSystem;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.inceptai.wifiexpertsystem.expertSystem.messages.ExpertMessage;
import com.inceptai.wifiexpertsystem.expertSystem.messages.UserMessage;
import com.inceptai.wifiexpertsystem.utils.DobbyLog;
import com.inceptai.wifimonitoringservice.ActionLibrary;
import com.inceptai.wifimonitoringservice.ActionRequest;
import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.Action;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by vivek on 8/4/17.
 */
public class ExpertSystemClient implements
        ExpertSystemService.ExpertSystemCallback,
        ActionLibrary.ActionCallback {

    private ExpertSystemService expertSystemService;
    private ActionLibrary actionLibrary;
    private String userId;
    private Context context;
    private ExpertSystemClientCallback expertSystemClientCallback;
    private ExecutorService executorService;

    public interface ExpertSystemClientCallback {
        void onExpertMessage(ExpertMessage expertMessage);
    }

    public ExpertSystemClient(String userId, Context context, ExecutorService executorService,
                              ListeningScheduledExecutorService listeningScheduledExecutorService,
                              ScheduledExecutorService scheduledExecutorService,
                              @NonNull ExpertSystemClientCallback expertSystemClientCallback) {
        this.context = context.getApplicationContext();
        this.userId = userId;
        this.expertSystemClientCallback = expertSystemClientCallback;
        this.executorService = executorService;
        actionLibrary = new ActionLibrary(context, executorService, listeningScheduledExecutorService, scheduledExecutorService);
        actionLibrary.registerCallback(this);
        expertSystemService = new ExpertSystemService(userId, context, executorService, this);
    }

    // Connect to the expert system service.
//    public void connect() {
//        expertSystemService = new ExpertSystemService(userId, context, this);
//    }

    public void onUserMessage(UserMessage userMessage) {
        expertSystemService.onUserMessage(userMessage);
    }

    public void cleanup() {
        expertSystemService.disconnect();
    }

    //Expert system service callbacks
    @Override
    public void onActionRequested(final ActionRequest actionRequest) {
        //Take action based on action request
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                actionLibrary.takeAction(actionRequest);
            }
        });
    }

    @Override
    public void onExpertMessage(final ExpertMessage expertMessage) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                expertSystemClientCallback.onExpertMessage(expertMessage);
            }
        });
    }

    //Action Library callbacks
    @Override
    public void actionStarted(final Action action) {
        DobbyLog.v("Action started : " + action.getName());
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                expertSystemService.actionStarted(action);
            }
        });
    }

    @Override
    public void actionCompleted(final Action action, final ActionResult actionResult) {
        DobbyLog.v("Action completed : " + action.getName());
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                expertSystemService.actionFinished(action, actionResult);
            }
        });
    }
}
