package com.inceptai.wifiexpertsystem;

import android.app.AlarmManager;
import android.content.Context;
import android.support.annotation.NonNull;

import com.inceptai.neopojos.ActionDetails;
import com.inceptai.neoservice.NeoService;
import com.inceptai.neoservice.uiactions.UIActionResult;
import com.inceptai.wifiexpertsystem.actions.ui.NeoServiceClient;
import com.inceptai.wifiexpertsystem.analytics.DobbyAnalytics;
import com.inceptai.wifiexpertsystem.config.RemoteConfig;
import com.inceptai.wifiexpertsystem.eventbus.DobbyEventBus;
import com.inceptai.wifiexpertsystem.expert.ExpertChat;
import com.inceptai.wifiexpertsystem.expert.ExpertChatService;
import com.inceptai.wifiexpertsystem.expertSystem.ExpertSystemClient;
import com.inceptai.wifiexpertsystem.expertSystem.inferencing.DataInterpreter;
import com.inceptai.wifiexpertsystem.expertSystem.inferencing.SuggestionCreator;
import com.inceptai.wifiexpertsystem.expertSystem.messages.ExpertMessage;
import com.inceptai.wifiexpertsystem.expertSystem.messages.StructuredUserResponse;
import com.inceptai.wifiexpertsystem.expertSystem.messages.UserMessage;
import com.inceptai.wifiexpertsystem.utils.DobbyLog;
import com.inceptai.wifiexpertsystem.utils.Utils;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.speedtest.BandwidthResult;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.wifi.WifiNetworkOverview;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.Action;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.ObservableAction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Observable;

/**
 * Created by vivek on 6/30/17.
 */

public class UserInteractionManager implements
        ExpertChatService.ChatCallback,
        NeoService.Callback,
        ExpertSystemClient.ExpertSystemClientCallback {

    private static final String PREF_CHAT_IN_EXPERT_MODE = "wifidoc_in_expert_mode";
    private static final String EXPERT_MODE_INITIATED_TIMESTAMP = "expert_mode_start_ts";
    private static final long MAX_TIME_ELAPSED_FOR_RESUMING_EXPERT_MODE_MS = AlarmManager.INTERVAL_DAY;
    private static final long DELAY_BEFORE_WELCOME_MESSAGE_MS = 500;
    private static final long ACCESSIBILITY_SETTING_CHECKER_TIMEOUT_MS = 30000;

    public static final String[] INITIAL_COMMAND_LIST = {
            "Bluetooth on",
            "Battery saver on",
            "Turn off cellular data"
    };

    private long currentEtaSeconds;
    private InteractionCallback interactionCallback;
    private Context context;
    private boolean expertIsPresent = false;
    private ScheduledExecutorService scheduledExecutorService;
    private Set<String> expertChatIdsDisplayed;
    private boolean explicitHumanContactMode;
    private boolean historyAvailable;
    private ScheduledFuture<?> accessibilityCheckFuture;
    private ExpertSystemClient expertSystemClient;
    private NeoServiceClient neoServiceClient;

    @Inject
    ExpertChatService expertChatService;
    @Inject
    DobbyThreadPool dobbyThreadPool;
    @Inject
    DobbyEventBus dobbyEventBus;
    @Inject
    DobbyAnalytics dobbyAnalytics;
    @Inject
    DobbyApplication dobbyApplication;
    @Inject
    RemoteConfig remoteConfig;

    private boolean isUserInChat;
    private boolean triggerAccessibilityDialogOnResume;

    public UserInteractionManager(Context context, @NonNull InteractionCallback interactionCallback, boolean showContactHumanAction) {
        ((DobbyApplication) context.getApplicationContext()).getProdComponent().inject(this);
        this.context = context;
        currentEtaSeconds = 0;
        scheduledExecutorService = dobbyThreadPool.getScheduledExecutorService();
        expertChatIdsDisplayed = new HashSet<>();
        this.explicitHumanContactMode = showContactHumanAction;
        expertChatService.setCallback(this);
        dobbyEventBus.registerListener(this);
        this.historyAvailable = false;
        neoServiceClient = new NeoServiceClient(remoteConfig, dobbyThreadPool, dobbyApplication, this);
        isUserInChat = false;
        triggerAccessibilityDialogOnResume = false;
        expertSystemClient = new ExpertSystemClient(
                dobbyApplication.getUserUuid(),
                context,
                dobbyThreadPool.getExecutorService(),
                dobbyThreadPool.getListeningScheduledExecutorService(),
                dobbyThreadPool.getScheduledExecutorService(),
                neoServiceClient,
                this);
        registerCallback(interactionCallback);

    }

    @Override
    public void onUIActionsAvailable(List<ActionDetails> actionDetailsList) {
        if (actionDetailsList != null && ! actionDetailsList.isEmpty()) {
            expertChatService.pushBotChatMessage(actionDetailsList.toString());
        } else {
            expertChatService.pushBotChatMessage("Received empty or nil actions");
        }
    }

    /**
     * Registers new callback -- overrides old listener
     * @param interactionCallback
     */
    public void registerCallback(InteractionCallback interactionCallback) {
        this.interactionCallback = UserInteractionManagerCallbackThreadSwitcher.wrap(
                dobbyThreadPool.getExecutorService(), interactionCallback);
    }

    public interface InteractionCallback {
        //Text responses
        void showBotResponse(String text);
        void showUserResponse(String text);
        void showExpertResponse(String text);
        void showStatusUpdate(String text);
        void showFillerTypingMessage(String text);


        //Show user action options
        void showUserActionOptions(List<StructuredUserResponse> structuredUserResponses);
        void updateExpertIndicator(String text);
        void hideExpertIndicator();

        //Richer content UI stuff -- TODO extract out all bw/nl stuff -- send only numbers and text
        void observeBandwidth(Observable bandwidthObservable);
        void cancelTestsResponse();
        void showBandwidthViewCard(double downloadMbps, double uploadMbps);
        void showNetworkInfoViewCard(WifiNetworkOverview wifiNetworkOverview);
        void showPingInfoViewCard(DataInterpreter.PingGrade pingGrade);
        void showDetailedSuggestions(SuggestionCreator.Suggestion suggestion);
        void requestAccessibilityPermission();
    }

    //API calls
    public void onUserEnteredChat() {
        isUserInChat = true;
        fetchChatMessages();
        expertChatService.checkIn();
        expertChatService.sendUserEnteredMetaMessage();
        expertChatService.disableNotifications();
        if (triggerAccessibilityDialogOnResume) {
            askForAccessibilityPermission();
            triggerAccessibilityDialogOnResume = false;
        }
    }

    public void onUserExitChat() {
        isUserInChat = false;
        expertChatService.sendUserLeftMetaMessage();
        expertChatService.enableNotifications();
        expertChatService.saveState();
        //registerNotificationInfoReceiver();
    }

    public void cleanup() {
        dobbyEventBus.unregisterListener(this);
        expertChatIdsDisplayed.clear();
        expertChatService.unregisterChatCallback();
        expertChatService.disconnect();
        neoServiceClient.cleanup();
        expertSystemClient.cleanup();
    }

    public void toggleNeoService() {
        neoServiceClient.toggleNeoService();
    }

    public void overlayPermissionStatus(boolean granted) {
        if (!granted) {
            DobbyLog.e("Permission denied for overlay draw.");
            sendOverlayPermissionDeniedMetaMessage();
        } else {
            sendOverlayPermissionGrantedMetaMessage();
        }
    }

    public void onUserQuery(String text, boolean isStructuredResponse, @StructuredUserResponse.ResponseType int responseType, int responseId) {
        UserMessage userMessage = null;
        if (isStructuredResponse && responseType != StructuredUserResponse.ResponseType.UNKNOWN) {
            userMessage = new UserMessage(new StructuredUserResponse(text, responseType, responseId));
        } else {
            userMessage = new UserMessage(text);
        }
        expertSystemClient.onUserMessage(userMessage);
        expertChatService.pushUserChatMessage(text, isStructuredResponse);
    }

    public void resumeChatWithShortSuggestion() {
        onFirstTimeResumedChat(true, false, false);
    }

    public void resumeChatWithWifiCheck() {
        onFirstTimeResumedChat(false, true, false);
    }

    public void resumeChatWithWelcomeMessage() {
        onFirstTimeResumedChat(false, false, true);
    }

    private void onFirstTimeResumedChat(final boolean resumeWithSuggestionIfAvailable,
                                        final boolean resumeWithWifiCheck,
                                        final boolean resumeWithWelcomeMessage) {
        final boolean resumingInExpertMode = checkSharedPrefForExpertModeResume();
        DobbyLog.v("UIM:onFirstTimeResumed");
        interactionCallback.showExpertResponse("Hi, this is Neo. I can help you with managing your settings. You can type stuff like:");
        interactionCallback.showExpertResponse("Turn on bluetooth");
        interactionCallback.showExpertResponse("Turn on battery saver");
        interactionCallback.showExpertResponse("Turn off cellular data");
        interactionCallback.showUserActionOptions(generateInitialStructuredResponse());
    }

    public void onAccessibilityPermissionGranted(boolean enableOverlay) {
        if (neoServiceClient != null) {
            //To make sure we can get to streaming if onServiceConnected not called --
            // onServiceConnected is not called always
            neoServiceClient.startService(enableOverlay);
        }
        onServiceReady();
    }

    @Override
    public void onUIActionStarted(String query, String appName) {
        DobbyLog.v("UIAction started for appName " + appName + " and query: " + query);
    }

    @Override
    public void onUIActionFinished(String query, String appName, UIActionResult uiActionResult) {
        DobbyLog.v("UIAction finished for appName " + appName + " and query: " + query + " status: " + uiActionResult.getStatus());
    }

    @Override
    public void onExpertMessage(final ExpertMessage expertMessage) {
        //Push the message to fire base
        if (expertMessage.getMessage() != null) {
            expertChatService.pushExpertChatMessage(expertMessage.getMessage());
        }
        //Next step of UI to show
        if (expertMessage.getUserResponseOptionsToShow() != null) {
            interactionCallback.showUserActionOptions(expertMessage.getUserResponseOptionsToShow());
        }

        //Handle action start/finish
        switch (expertMessage.getExpertMessageType()) {
            case ExpertMessage.ExpertMessageType.ACTION_STARTED:
                handleActionStarted(expertMessage.getExpertAction());
                break;
            case ExpertMessage.ExpertMessageType.ACTION_FINISHED:
                expertChatService.sendActionCompletedMessage();
                break;
            case ExpertMessage.ExpertMessageType.SHOW_BANDWIDTH_INFO:
                BandwidthResult bandwidthResult = expertMessage.getBandwidthResult();
                if (bandwidthResult != null) {
                    interactionCallback.showBandwidthViewCard(
                            Utils.toMbps(bandwidthResult.getDownloadStats().getPercentile75(), 2),
                            Utils.toMbps(bandwidthResult.getUploadStats().getPercentile75(), 2));
                }
                break;
            case ExpertMessage.ExpertMessageType.SHOW_PING_INFO:
                DataInterpreter.PingGrade pingGrade = expertMessage.getPingGrade();
                if (pingGrade != null) {
                    interactionCallback.showPingInfoViewCard(pingGrade);
                }
                break;
            case ExpertMessage.ExpertMessageType.SHOW_WIFI_INFO:
                WifiNetworkOverview wifiNetworkOverview = expertMessage.getWifiNetworkOverview();
                if (wifiNetworkOverview != null) {
                    interactionCallback.showNetworkInfoViewCard(wifiNetworkOverview);
                }
                break;
            case ExpertMessage.ExpertMessageType.SHOW_ACTION_DETAIL_LIST:
//                WifiNetworkOverview wifiNetworkOverview = expertMessage.getWifiNetworkOverview();
//                if (wifiNetworkOverview != null) {
//                    interactionCallback.showNetworkInfoViewCard(wifiNetworkOverview);
//                }
                break;
            default:
                break;
        }
    }

    private void handleActionStarted(Action action) {
        if (action.getActionType() == Action.ActionType.PERFORM_BANDWIDTH_TEST) {
            final ObservableAction observableAction = (ObservableAction)action;
            DobbyLog.v("UIM: handleActionStarted: Sending observeBandwidth callback");
            interactionCallback.observeBandwidth(observableAction.getObservable());
        }
        expertChatService.sendActionStartedMessage();
    }

    //Expert chat service callback
    @Override
    public void onMessageAvailable(ExpertChat expertChat) {
        //Thread switch here
        if (!expertChat.getId().equals(Utils.EMPTY_STRING) && expertChatIdsDisplayed.contains(expertChat.getId())) {
            //We have already displayed this chat message
            DobbyLog.v("Ignoring since empty text " + expertChat.getText());
            return;
        }
        expertChatIdsDisplayed.add(expertChat.getId());
        final String messageReceived = expertChat.getText();
        DobbyLog.v("DobbyActivity:FirebaseMessage recvd from firebase: " + messageReceived);

        switch (expertChat.getMessageType()) {
            case ExpertChat.MSG_TYPE_EXPERT_TEXT:
                interactionCallback.showExpertResponse(messageReceived);
                //Set to expert mode on the first message received from expert
                if (expertChat.isMessageFresh()) {
                    DobbyLog.v("UserInteractionManager: coming in fresh expert message, set chat to expert");
                    //dobbyAi.setChatInExpertMode();
                }
                neoServiceClient.setStatus(messageReceived);
                DobbyLog.v("DobbyActivity:FirebaseMessage added mesg to ExpertChat");
                break;
            case ExpertChat.MSG_TYPE_BOT_TEXT:
                DobbyLog.v("DobbyActivity:FirebaseMessage added mesg to Bot chat");
                interactionCallback.showBotResponse(messageReceived);
                break;
            case ExpertChat.MSG_TYPE_USER_TEXT:
                DobbyLog.v("DobbyActivity:FirebaseMessage added mesg to User chat");
                interactionCallback.showUserResponse(messageReceived);
                break;
        }
    }

    @Override
    public void onNoHistoryAvailable() {
        historyAvailable = true;
    }

    @Override
    public void onEtaUpdated(long newEtaSeconds, boolean isPresent) {
        updateEta(newEtaSeconds, isPresent);
    }

    @Override
    public void onEtaAvailable(long newEtaSeconds, boolean isPresent) {
        updateEta(newEtaSeconds, isPresent);
    }


    public boolean isFirstChatAfterInstall() {
        return expertChatService.isFirstChatAfterInstall();
    }

    public void notificationConsumed() {
        expertChatService.onNotificationConsumed();
    }


    //Private methods
    private String getEtaMessage() {
        String messagePrefix = context.getResources().getString(R.string.expected_response_time_for_expert);
        String message;
        if (!expertIsPresent || currentEtaSeconds >= ExpertChatService.ETA_12HOURS) {
            message = "Our experts are currently offline. You shall receive a response in about 12 hours.";
        } else {
            message = messagePrefix + " Less than " + Utils.timeSecondsToString(currentEtaSeconds);
        }
        return message;
    }

    private void sendInitialMessageToExpert() {
        //Contacting expert
        expertChatService.triggerContactWithHumanExpert("Expert help needed here");
    }


    private void saveSwitchedToBotMode() {
        Utils.saveSharedSetting(context,
                PREF_CHAT_IN_EXPERT_MODE, Utils.FALSE_STRING);
        Utils.saveSharedSetting(context,
                EXPERT_MODE_INITIATED_TIMESTAMP, 0);
    }

    private void saveSwitchedToExpertMode() {
        Utils.saveSharedSetting(context,
                PREF_CHAT_IN_EXPERT_MODE, Utils.TRUE_STRING);
        Utils.saveSharedSetting(context,
                EXPERT_MODE_INITIATED_TIMESTAMP, System.currentTimeMillis());
    }

    private boolean checkSharedPrefForExpertModeResume() {
        long lastExpertInitiatedAtMs = Utils.readSharedSetting(context, EXPERT_MODE_INITIATED_TIMESTAMP, 0);
        if (lastExpertInitiatedAtMs > 0 &&
                System.currentTimeMillis() - lastExpertInitiatedAtMs < MAX_TIME_ELAPSED_FOR_RESUMING_EXPERT_MODE_MS) {
            return true;
        }
        return false;
    }

    private void updateEta(long newEtaSeconds, boolean isPresent) {
        currentEtaSeconds = newEtaSeconds;
        expertIsPresent = isPresent;
        //dobbyAi.updatedEtaAvailable(currentEtaSeconds);
        //updateExpertIndicator();
    }

    private void fetchChatMessages() {
        expertChatService.setCallback(this);
        if (!expertChatService.isListenerConnected()) {
            expertChatService.fetchChatMessages();
        }
    }


    //Neo callbacks
    @Override
    public void onRequestAccessibilitySettings() {
        if (isUserInChat) {
            askForAccessibilityPermission();
        } else {
            triggerAccessibilityDialogOnResume = true;
        }
    }

    @Override
    public void onServiceReady() {
        try {
            if (accessibilityCheckFuture != null && !accessibilityCheckFuture.isDone()) {
                accessibilityCheckFuture.cancel(true);
            }
        } catch (CancellationException e) {
            DobbyLog.v("Exception while cancelling accessibility future");
        }
        sendServiceReadyMetaMessage();
        expertChatService.disableNotifications();
    }

    public UserInteractionManager() {
        super();
    }

    @Override
    public void onServiceStopped() {
        sendServiceStoppedDueToErrorsMetaMessage();
    }

    @Override
    public void onUiStreamingStoppedByUser() {
        sendServiceStoppedByUserMetaMessage();
        Utils.launchWifiExpertMainActivity(context.getApplicationContext());
    }

    @Override
    public void onUiStreamingStoppedByExpert() {
        sendServiceStoppedByExpertMetaMessage();
        Utils.launchWifiExpertMainActivity(context.getApplicationContext());
    }


    //private stuff
    private void askForAccessibilityPermission() {
        if (interactionCallback != null) {
            interactionCallback.requestAccessibilityPermission();
        }
        accessibilityCheckFuture = scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                DobbyLog.e("User did not grant accessibility permission");
                sendAccessibilityPermissionNotGrantedWithinTimeoutMessage();
            }
        }, ACCESSIBILITY_SETTING_CHECKER_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    }

    private void sendAccessibilityPermissionNotGrantedWithinTimeoutMessage() {
        expertChatService.pushMetaChatMessage(ExpertChat.MSG_TYPE_META_NEO_ACCESSIBILITY_PERMISSION_NOT_GRANTED_WITHIN_TIMEOUT);
    }

    private void sendServiceReadyMetaMessage() {
        expertChatService.pushMetaChatMessage(ExpertChat.MSG_TYPE_META_NEO_SERVICE_READY);
    }

    private void sendServiceStoppedDueToErrorsMetaMessage() {
        expertChatService.pushMetaChatMessage(ExpertChat.MSG_TYPE_META_NEO_SERVICE_STOPPED_DUE_TO_ERRORS);

    }

    private void sendOverlayPermissionDeniedMetaMessage() {
        expertChatService.pushMetaChatMessage(ExpertChat.MSG_TYPE_META_NEO_SERVICE_OVERLAY_PERMISSION_DENIED);
    }

    private void sendOverlayPermissionGrantedMetaMessage() {
        expertChatService.pushMetaChatMessage(ExpertChat.MSG_TYPE_META_NEO_SERVICE_OVERLAY_PERMISSION_GRANTED);
    }

    private void sendServiceStoppedByUserMetaMessage() {
        expertChatService.pushMetaChatMessage(ExpertChat.MSG_TYPE_META_NEO_SERVICE_STOPPED_BY_USER);

    }

    private void sendServiceStoppedByExpertMetaMessage() {
        expertChatService.pushMetaChatMessage(ExpertChat.MSG_TYPE_META_NEO_SERVICE_STOPPED_BY_EXPERT);

    }

    private void sendServiceStoppedDueToErrorsMessage() {
        expertChatService.pushMetaChatMessage(ExpertChat.MSG_TYPE_META_NEO_SERVICE_STOPPED_BY_EXPERT);

    }

    private List<StructuredUserResponse> generateInitialStructuredResponse() {
        List<StructuredUserResponse> structuredUserResponses = new ArrayList<>();
        for (String command: INITIAL_COMMAND_LIST) {
            StructuredUserResponse structuredUserResponse = new StructuredUserResponse(command, StructuredUserResponse.ResponseType.UNKNOWN, 0);
            structuredUserResponses.add(structuredUserResponse);
        }
        return structuredUserResponses;
    }
}
