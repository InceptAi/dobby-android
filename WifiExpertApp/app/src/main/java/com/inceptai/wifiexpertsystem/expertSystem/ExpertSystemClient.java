package com.inceptai.wifiexpertsystem.expertSystem;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.inceptai.neopojos.ActionDetails;
import com.inceptai.neoservice.uiactions.UIActionResult;
import com.inceptai.wifiexpertsystem.actions.ActionTaker;
import com.inceptai.wifiexpertsystem.actions.ui.NeoServiceClient;
import com.inceptai.wifiexpertsystem.expertSystem.messages.ExpertMessage;
import com.inceptai.wifiexpertsystem.expertSystem.messages.StructuredUserResponse;
import com.inceptai.wifiexpertsystem.expertSystem.messages.UserMessage;
import com.inceptai.wifiexpertsystem.utils.DobbyLog;
import com.inceptai.wifiexpertsystem.utils.Utils;
import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by vivek on 8/4/17.
 */
public class ExpertSystemClient implements
        ExpertSystemService.ExpertSystemCallback,
        ActionTaker.ActionTakerCallback {


    private static final String[] SUCCESS_MESSAGE_LIST = {
            "Done! What else can I help with ?",
            "Hooray, done with this one",
            "Yay, I did it, whats next",
            "Ok done. Anything else ?",
            "Finished, next ?",
            "Done with this one :)"
    };

    private ExpertSystemService expertSystemService;
    private String userId;
    private Context context;
    private ExpertSystemClientCallback expertSystemClientCallback;
    private ExecutorService executorService;
    private ActionTaker actionTaker;
    private NeoServiceClient neoServiceClient;
    private HashMap<Integer, ActionDetails> actionDetailsHashMap;

    public interface ExpertSystemClientCallback {
        void onExpertMessage(ExpertMessage expertMessage);
    }

    public ExpertSystemClient(String userId, Context context, ExecutorService executorService,
                              ListeningScheduledExecutorService listeningScheduledExecutorService,
                              ScheduledExecutorService scheduledExecutorService,
                              NeoServiceClient neoServiceClient,
                              @NonNull ExpertSystemClientCallback expertSystemClientCallback) {
        this.context = context.getApplicationContext();
        this.userId = userId;
        this.expertSystemClientCallback = expertSystemClientCallback;
        this.executorService = executorService;
        this.neoServiceClient = neoServiceClient;
        actionTaker = new ActionTaker(
                context, executorService,
                listeningScheduledExecutorService,
                scheduledExecutorService,
                neoServiceClient, this);
        expertSystemService = new ExpertSystemService(userId, context, executorService, this);
        actionDetailsHashMap = new HashMap<>();
    }


    @Override
    public void actionsAvailable(List<ActionDetails> actionDetailsList, int statusCode) {
        //Check if the error is because neo service is uninitialized
        if (UIActionResult.failedDueToAccessibilityIssue(statusCode)) {
            //launch accessibility permission and try this command again
            startNeoServiceWithStreaming(false);
            sendFailureMessageToUser(UIActionResult.getUserReadableMessage(statusCode));
            return;
        }
        //Display the list of actions to the user and ask it to choose.
        List<StructuredUserResponse> structuredUserResponses = updateActionDetailsMap(actionDetailsList);
        ExpertMessage expertMessage = ExpertMessage.createMessageForActionDetails(structuredUserResponses);
        if (expertSystemClientCallback != null) {
            expertSystemClientCallback.onExpertMessage(expertMessage);
        }
    }

    private void sendSuccessMessageToUser() {
        int successMessageIndex = Utils.getRandomIntWithUpperBound(SUCCESS_MESSAGE_LIST.length);
        String successMessage = SUCCESS_MESSAGE_LIST[successMessageIndex];
        ExpertMessage expertMessage = ExpertMessage.createMessageForUIActionEnded(successMessage);
        if (expertSystemClientCallback != null) {
            expertSystemClientCallback.onExpertMessage(expertMessage);
        }
    }

    private void sendFailureMessageToUser(String message) {
        ExpertMessage expertMessage = ExpertMessage.createMessageForUIActionEnded(message);
        if (expertSystemClientCallback != null) {
            expertSystemClientCallback.onExpertMessage(expertMessage);
        }
    }

    @Override
    public void actionStarted(String query, String appName) {
        DobbyLog.v("ESXXXX Action started: query: " + query + " app: " + appName);
    }

    @Override
    public void actionFinished(String query, String appName, String status, ActionResult apiActionResult, UIActionResult uiActionResult) {
        DobbyLog.v("ESXXXX Action finished: query: " + query + " app: " + appName + " status: " + status);
        if (apiActionResult != null) {
            //This was an api action, see the results here
            DobbyLog.v("ESXXX API action result: " + ActionResult.isSuccessful(apiActionResult));
        } else if (uiActionResult != null) {
            if (UIActionResult.isSuccessful(uiActionResult)) {
                sendSuccessMessageToUser();
            } else {
                sendFailureMessageToUser(UIActionResult.getUserReadableMessage(uiActionResult));
            }
            DobbyLog.v("ESXXX API action result: " + UIActionResult.isSuccessful(uiActionResult));
            if (UIActionResult.failedDueToAccessibilityIssue(uiActionResult)) {
                //launch accessibility permission and try this command again
                startNeoServiceWithStreaming(false);
            } else {
                //bring the user back into the app after UIAction finishes.
                Utils.launchWifiExpertMainActivity(context.getApplicationContext());
                //Utils.resumeWifiExpertMainActivity(context.getApplicationContext());
            }
        }
    }

    private void startNeoServiceWithStreaming(boolean enableOverlay) {
        if (neoServiceClient != null) {
            neoServiceClient.startService(enableOverlay);
        }
    }


    public void onUserMessage(UserMessage userMessage) {
        if (userMessage.isStructuredResponse() && userMessage.getStructuredUserResponse().isUIActionStructuredResponse()) {
            //We got a response from the user -- take an appropriate action
            StructuredUserResponse structuredUserResponse = userMessage.getStructuredUserResponse();
            int responseId = structuredUserResponse != null ? structuredUserResponse.getResponseId() : 0;
            ActionDetails actionDetails = actionDetailsHashMap.get(responseId);
            if (actionDetails != null) {
                //User response, take UI action
                actionTaker.performUIActionForSettings(actionDetails);
            }
        } else {
            expertSystemService.onUserMessage(userMessage);
        }
    }

    public void cleanup() {
        expertSystemService.disconnect();
        if (neoServiceClient != null) {
            neoServiceClient.cleanup();
        }
    }

    public void takeUIAction(ActionDetails actionDetailToTake) {
        //Take particular ui action
        actionTaker.performUIActionForSettings(actionDetailToTake);
    }

    //Expert system service callbacks
    @Override
    public void onActionRequested(String actionQuery) {
        //Parse string and take action.
        //final boolean TRY_API_ACTION_FIRST = true;
        //actionTaker.takeAction(actionQuery, Utils.SETTINGS_APP_NAME, TRY_API_ACTION_FIRST, false);
        actionTaker.fetchUIActionForSettings(actionQuery);
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

    private List<StructuredUserResponse> updateActionDetailsMap(List<ActionDetails> actionDetailsList) {
        actionDetailsHashMap.clear();
        List<StructuredUserResponse> structuredUserResponses = new ArrayList<>();
        for (ActionDetails actionDetails: actionDetailsList) {
            String actionDescription = actionDetails.getActionIdentifier() != null ? actionDetails.getActionIdentifier().getActionDescription() : Utils.EMPTY_STRING;
            StructuredUserResponse structuredUserResponse = new StructuredUserResponse(
                    actionDescription,
                    actionDetails.hashCode());
            structuredUserResponses.add(structuredUserResponse);
            actionDetailsHashMap.put(actionDetails.hashCode(), actionDetails);
        }
        return structuredUserResponses;
    }
}
