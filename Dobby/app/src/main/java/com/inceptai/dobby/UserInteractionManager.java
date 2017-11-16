package com.inceptai.dobby;

import android.app.AlarmManager;
import android.content.Context;
import android.net.wifi.WifiInfo;

import com.google.common.eventbus.Subscribe;
import com.inceptai.dobby.actions.ActionTaker;
import com.inceptai.dobby.actions.api.APIActionLibraryClient;
import com.inceptai.dobby.actions.ui.NeoServiceClient;
import com.inceptai.dobby.ai.DataInterpreter;
import com.inceptai.dobby.ai.DobbyAi;
import com.inceptai.dobby.ai.RtDataSource;
import com.inceptai.dobby.ai.SuggestionCreator;
import com.inceptai.dobby.analytics.DobbyAnalytics;
import com.inceptai.dobby.eventbus.DobbyEvent;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.expert.ExpertChat;
import com.inceptai.dobby.expert.ExpertChatService;
import com.inceptai.dobby.feedback.RatingsManager;
import com.inceptai.dobby.speedtest.BandwidthObserver;
import com.inceptai.dobby.utils.DobbyLog;
import com.inceptai.dobby.utils.Utils;
import com.inceptai.neopojos.ActionDetails;
import com.inceptai.neoservice.NeoService;
import com.inceptai.neoservice.uiactions.UIActionResult;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

/**
 * Created by vivek on 6/30/17.
 */

public class UserInteractionManager implements
        DobbyAi.ResponseCallback,
        ExpertChatService.ChatCallback,
        NeoService.Callback,
        WifiMonitoringServiceClient.WifiMonitoringCallback,
        ActionTaker.ActionTakerCallback {

    private static final String PREF_CHAT_IN_EXPERT_MODE = "wifidoc_in_expert_mode";
    private static final String EXPERT_MODE_INITIATED_TIMESTAMP = "expert_mode_start_ts";
    private static final long MAX_TIME_ELAPSED_FOR_RESUMING_EXPERT_MODE_MS = AlarmManager.INTERVAL_DAY;
    private static final long DELAY_BEFORE_WELCOME_MESSAGE_MS = 500;
    private static final long ACCESSIBILITY_SETTING_CHECKER_TIMEOUT_MS = 10000;
    private static final long WIFI_REPAIR_TIMEOUT_MS = 30000;
    private static final String PREF_CHAT_FIRST_TIME = "first_user_interaction";


    private long currentEtaSeconds;
    private InteractionCallback interactionCallback;
    private Context context;
    private boolean expertIsPresent = false;
    private ScheduledExecutorService scheduledExecutorService;
    private Set<String> expertChatIdsDisplayed;
    private boolean explicitHumanContactMode;
    private boolean historyAvailable;
    private ScheduledFuture<?> accessibilityCheckFuture;
    private boolean locationPermissionGranted;
    //private NotificationInfoReceiver notificationInfoReceiver;

    @Inject
    DobbyAi dobbyAi;
    @Inject
    ExpertChatService expertChatService;
    @Inject
    DobbyThreadpool dobbyThreadpool;
    @Inject
    DobbyEventBus dobbyEventBus;
    @Inject
    DobbyAnalytics dobbyAnalytics;
    @Inject
    DobbyApplication dobbyApplication;
    @Inject
    RemoteConfig remoteConfig;

    private NeoServiceClient neoServiceClient;
    private WifiMonitoringServiceClient wifiMonitoringServiceClient;
    private boolean isUserInChat;
    private boolean triggerAccessibilityDialogOnResume;
    private RatingsManager ratingsManager;
    private APIActionLibraryClient apiActionLibraryClient;
    private ActionTaker actionTaker;


    public UserInteractionManager(Context context, InteractionCallback interactionCallback, boolean showContactHumanAction) {
        ((DobbyApplication) context.getApplicationContext()).getProdComponent().inject(this);
        this.context = context;
        currentEtaSeconds = 0;
        scheduledExecutorService = dobbyThreadpool.getScheduledExecutorService();
        expertChatIdsDisplayed = new HashSet<>();
        this.interactionCallback = interactionCallback;
        this.explicitHumanContactMode = showContactHumanAction;
        expertChatService.setCallback(this);
        dobbyAi.setResponseCallback(this);
        dobbyAi.initChatToBotState(); //Resets booleans indicating which mode of expert are we in
        dobbyAi.setShowContactHumanButton(showContactHumanAction);
        dobbyEventBus.registerListener(this);
        this.historyAvailable = false;
        neoServiceClient = new NeoServiceClient(remoteConfig, dobbyThreadpool, dobbyApplication, this);
        ratingsManager = new RatingsManager(context, remoteConfig);
        wifiMonitoringServiceClient = new WifiMonitoringServiceClient(context.getApplicationContext(),
                dobbyApplication.getUserUuid(), dobbyApplication.getPhoneInfo(),
                dobbyThreadpool.getExecutor(), this);
        isUserInChat = false;
        triggerAccessibilityDialogOnResume = false;
        locationPermissionGranted = true;
        if (isFirstUserInteraction()) {
            dobbyAnalytics.setFirstRunAfterInstall();
            saveFirstUserInteraction();
        } else {
            dobbyAnalytics.setReturningUser();
        }
        actionTaker = new ActionTaker(
                context, dobbyThreadpool.getExecutorService(),
                dobbyThreadpool.getListeningScheduledExecutorService(),
                dobbyThreadpool.getScheduledExecutorService(),
                neoServiceClient, this);
    }

    public interface InteractionCallback {
        //Text responses
        void showBotResponse(String text);
        void showUserResponse(String text);
        void showExpertResponse(String text);
        void showStatusUpdate(String text);
        void showFillerTypingMessage(String text);


        //Show user action options
        void showUserActionOptions(List<Integer> userResponseTypes);
        void updateExpertIndicator(String text);
        void hideExpertIndicator();

        //Richer content UI stuff -- TODO extract out all bw/nl stuff -- send only numbers and text
        void observeBandwidth(BandwidthObserver observer);
        void cancelTestsResponse();
        void showBandwidthViewCard(DataInterpreter.BandwidthGrade bandwidthGrade);
        void showNetworkInfoViewCard(DataInterpreter.WifiGrade wifiGrade, String isp, String ip);
        void showDetailedSuggestions(SuggestionCreator.Suggestion suggestion);
        void showWifiRepairCard(boolean success, WifiInfo wifiInfo, String repairSummary);
        void showRepairCancelled();
        void showWifiMonitoringStatusChange(boolean status);
        void requestAccessibilityPermission();
    }

    //API calls
    public void onUserEnteredChat() {
        isUserInChat = true;
        remoteConfig.fetchAsync();
        fetchChatMessages();
        expertChatService.checkIn();
        expertChatService.registerToEventBusListener();
        expertChatService.sendUserEnteredMetaMessage();
        expertChatService.disableNotifications();
        updateExpertIndicator();
        wifiMonitoringServiceClient.pauseNotifications();
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
        wifiMonitoringServiceClient.resumeNotificationIfNeeded();
    }

    public void cleanup() {
        dobbyEventBus.unregisterListener(this);
        expertChatIdsDisplayed.clear();
        expertChatService.unregisterChatCallback();
        expertChatService.disconnect();
        dobbyAi.cleanup();
        neoServiceClient.cleanup();
        wifiMonitoringServiceClient.cleanup();
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

    public void onUserQuery(String text, boolean isButtonActionText) {
        dobbyAi.sendQuery(text, isButtonActionText);
    }

    public void initChatToBotState() {
        dobbyAi.initChatToBotState();
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



    public boolean isLocationPermissionGranted() {
        return locationPermissionGranted;
    }

    public void setLocationPermissionGranted(boolean locationPermissionGranted) {
        this.locationPermissionGranted = locationPermissionGranted;
    }


    //Action Taker callbacks

    @Override
    public void actionStarted(String query, String appPackageName) {
        DobbyLog.v("ESXXXX Action started: query: " + query + " app: " + appPackageName);
    }

    @Override
    public void actionFinished(String query, String appPackageName, String status, boolean isSuccessful) {
        DobbyLog.v("ESXXXX Action finished: query: " + query + " app: " + appPackageName + " status: " + status + " isSuccess: " + isSuccessful);
    }

    //Expert chat service callback
    @Override
    public void takeExpertSystemAction(String query, String appName) {
        final boolean TRY_API_ACTION_FIRST = true;
        actionTaker.takeAction(query, appName, TRY_API_ACTION_FIRST);
    }

    @Override
    public void fetchUIActionsForQuery(String query, String appName) {
        neoServiceClient.fetchUIActions(query, appName);
    }

    @Override
    public void onUIActionsAvailable(List<ActionDetails> actionDetailsList) {
        if (actionDetailsList != null && ! actionDetailsList.isEmpty()) {
            expertChatService.pushBotChatMessage(actionDetailsList.toString());
        } else {
            expertChatService.pushBotChatMessage("Received empty or nil actions");
        }
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
    public void onMessageAvailable(ExpertChat expertChat) {
        if (!expertChat.getId().equals(Utils.EMPTY_STRING) && expertChatIdsDisplayed.contains(expertChat.getId())) {
            //We have already displayed this chat message
            DobbyLog.v("Ignoring since empty text " + expertChat.getText());
            return;
        }

        expertChatIdsDisplayed.add(expertChat.getId());
        String messageReceived = expertChat.getText();
        DobbyLog.v("MainActivity:FirebaseMessage recvd from firebase: " + messageReceived);

        if (messageReceived.equals(Utils.EMPTY_STRING)) {
            DobbyLog.v("Ignoring empty string");
            return;
        }

        if (interactionCallback == null) {
            DobbyLog.v("callback is null, so ignoring message from firebase");
            return;
        }

        switch (expertChat.getMessageType()) {
            case ExpertChat.MSG_TYPE_EXPERT_TEXT:
                interactionCallback.showExpertResponse(messageReceived);
                //Set to expert mode on the first message received from expert
                if (expertChat.isMessageFresh()) {
                    DobbyLog.v("UserInteractionManager: coming in fresh expert message, set chat to expert");
                    dobbyAi.setChatInExpertMode();
                }
                neoServiceClient.setStatus(messageReceived);
                DobbyLog.v("MainActivity:FirebaseMessage added mesg to ExpertChat");
                break;
            case ExpertChat.MSG_TYPE_BOT_TEXT:
                DobbyLog.v("MainActivity:FirebaseMessage added mesg to Bot chat");
                interactionCallback.showBotResponse(messageReceived);
                break;
            case ExpertChat.MSG_TYPE_USER_TEXT:
                DobbyLog.v("MainActivity:FirebaseMessage added mesg to User chat");
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


    //Dobby Ai callbacks


    @Override
    public void userSaysAppIsHelpful() {
        maybeAskForRating();
    }

    @Override
    public void onUserSaysYesToAppRating() {
        ratingsManager.launchAppStorePageForRatingTheApp();
    }

    @Override
    public void onUserSaysNoToAppRating() {
        ratingsManager.saveRatingPreference(RatingsManager.NO_PREF);
    }

    @Override
    public void onUserSaysLaterToAppRating() {
        ratingsManager.saveRatingPreference(RatingsManager.LATER_PREF);
    }

    @Override
    public void startWifiRepair() {
        wifiMonitoringServiceClient.repairWifiNetwork(WIFI_REPAIR_TIMEOUT_MS, isLocationPermissionGranted());
    }

    @Override
    public void cancelWifiRepair() {
        wifiMonitoringServiceClient.cancelWifiRepair();
        if (interactionCallback != null) {
            interactionCallback.showRepairCancelled();
        }
    }

    @Override
    public void startWifiMonitoringService() {
        wifiMonitoringServiceClient.enableWifiService();

    }

    @Override
    public void stopWifiMonitoringService() {
        wifiMonitoringServiceClient.disableWifiService();

    }

    @Override
    public void startNeoByExpert() {
        //Stop notifications in onServiceReady
        neoServiceClient.startService();
    }

    @Override
    public void stopNeoByExpert() {
        expertChatService.enableNotifications(); //Enable notifications here.
        neoServiceClient.stopService();
    }

    @Override
    public void showBotResponseToUser(String text) {
        expertChatService.pushBotChatMessage(text);
    }

    @Override
    public void showRtGraph(RtDataSource<Float, Integer> rtDataSource) {
        //No-op
    }

    @Override
    public void observeBandwidth(BandwidthObserver observer) {
        if (interactionCallback != null) {
            interactionCallback.observeBandwidth(observer);
        }
    }

    @Override
    public void cancelTests() {
        if (interactionCallback != null) {
            interactionCallback.cancelTestsResponse();
        }
    }

    @Override
    public void showUserActionOptions(List<Integer> userResponseTypes) {
        if (interactionCallback != null) {
            interactionCallback.showUserActionOptions(userResponseTypes);
        }
    }

    @Override
    public void showBandwidthViewCard(DataInterpreter.BandwidthGrade bandwidthGrade) {
        if (interactionCallback != null) {
            interactionCallback.showBandwidthViewCard(bandwidthGrade);
        }
    }

    @Override
    public void showNetworkInfoViewCard(DataInterpreter.WifiGrade wifiGrade, String isp, String ip) {
        if (interactionCallback != null) {
            interactionCallback.showNetworkInfoViewCard(wifiGrade, isp, ip);
        }
        expertChatService.pushBotChatMessage(wifiGrade.userReadableInterpretation());
    }

    @Override
    public void showDetailedSuggestions(SuggestionCreator.Suggestion suggestion) {
        if (interactionCallback != null) {
            interactionCallback.showDetailedSuggestions(suggestion);
        }
    }

    @Override
    public void contactExpertAndGetETA() {
        //Showing ETA to user
        if (currentEtaSeconds > 0) {
            interactionCallback.showStatusUpdate(context.getString(R.string.contacting_and_getting_eta));
            interactionCallback.showStatusUpdate(getEtaMessage());
        } else {
            showStatus(context.getString(R.string.continue_expert_chat));
        }
        sendInitialMessageToExpert();
        updateExpertIndicator();
    }

    @Override
    public void onUserMessageAvailable(String text, boolean isButtonText) {
        DobbyLog.v("Pushing out user message " + text);
        expertChatService.pushUserChatMessage(text, isButtonText);
    }

    @Override
    public void showStatus(String text) {
        if (interactionCallback != null) {
            interactionCallback.showStatusUpdate(text);
        }
    }

    @Override
    public void switchedToExpertMode() {
        saveSwitchedToExpertMode();
        updateExpertIndicator();
    }

    @Override
    public void switchedToBotMode() {
        saveSwitchedToBotMode();
        updateExpertIndicator();
    }

    @Override
    public void switchedToExpertIsListeningMode() {
        updateExpertIndicator();
    }

    @Override
    public void userAskedForExpert() {
        updateExpertIndicator();
    }


    @Override
    public void expertActionStarted() {
        expertChatService.sendActionStartedMessage();
    }

    @Override
    public void expertActionCompleted() {
        expertChatService.sendActionCompletedMessage();
    }


    //Wifi monitoring callbacks


    @Override
    public void wifiMonitoringStarted() {
        if (interactionCallback != null) {
            interactionCallback.showWifiMonitoringStatusChange(true);
        }
        dobbyAi.wifiMonitoringStatusChanged(true);
    }

    @Override
    public void wifiMonitoringStopped() {
        if (interactionCallback != null) {
            interactionCallback.showWifiMonitoringStatusChange(false);
        }
        dobbyAi.wifiMonitoringStatusChanged(false);
    }

    @Override
    public void repairStarted(boolean started) {
        //No-op for now

    }

    @Override
    public void repairFinished(boolean success, WifiInfo repairedWifiInfo, String repairSummary) {
        dobbyAi.handleRepairFinished(success);
        String repairTitle = context.getString(R.string.repair_wifi_success);
        if (!success) {
            repairTitle = context.getString(R.string.repair_wifi_failure);
        }
        if (interactionCallback != null) {
            interactionCallback.showWifiRepairCard(success, repairedWifiInfo, repairSummary);
        }
        expertChatService.pushBotChatMessage(repairTitle);
        expertChatService.pushBotChatMessage(repairSummary);
        // Ask for rating here
        if (success) {
            maybeAskForRating();
        }
    }

    public boolean isFirstChatAfterInstall() {
        return expertChatService.isFirstChatAfterInstall();
    }

    public void notificationConsumed() {
        expertChatService.onNotificationConsumed();
    }


    //Private methods
    //Private stuff
    private void saveFirstUserInteraction() {
        Utils.saveSharedSetting(context,
                PREF_CHAT_FIRST_TIME, Utils.FALSE_STRING);
    }

    private boolean isFirstUserInteraction() {
        return Boolean.valueOf(Utils.readSharedSetting(context,
                PREF_CHAT_FIRST_TIME, Utils.TRUE_STRING));
    }

    private void onFirstTimeResumedChat(final boolean resumeWithSuggestionIfAvailable,
                                        final boolean resumeWithWifiCheck,
                                        final boolean resumeWithWelcomeMessage) {
        final boolean resumingInExpertMode = checkSharedPrefForExpertModeResume();
        DobbyLog.v("MainActivity:onFirstTimeResumed");
        final long delayForWelcomeMessage;
        if (historyAvailable) {
            delayForWelcomeMessage = 2 * DELAY_BEFORE_WELCOME_MESSAGE_MS;
        } else {
            delayForWelcomeMessage = DELAY_BEFORE_WELCOME_MESSAGE_MS;
        }
        if (dobbyAi != null) {
            if (resumeWithSuggestionIfAvailable) {
                scheduledExecutorService.schedule(new Runnable() {
                    @Override
                    public void run() {
                        dobbyAi.startUserInteractionWithShortSuggestion(resumingInExpertMode);
                    }
                }, delayForWelcomeMessage, TimeUnit.MILLISECONDS);
            } else if (resumeWithWifiCheck) {
                scheduledExecutorService.schedule(new Runnable() {
                    @Override
                    public void run() {
                        dobbyAi.startUserInteractionWithWifiCheck(resumingInExpertMode);
                    }
                }, delayForWelcomeMessage, TimeUnit.MILLISECONDS);
            } else if (resumeWithWelcomeMessage) {
                scheduledExecutorService.schedule(new Runnable() {
                    @Override
                    public void run() {
                        dobbyAi.startUserInteractionWithWelcome(resumingInExpertMode);
                    }
                }, delayForWelcomeMessage, TimeUnit.MILLISECONDS);
            }
            if (resumingInExpertMode) {
                dobbyAi.contactExpert();
            }
        }
    }




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

    private void updateExpertIndicator() {
        if (interactionCallback != null) {
            if (dobbyAi.getIsExpertListening()) {
                interactionCallback.updateExpertIndicator(context.getString(R.string.you_are_now_talking_to_human_expert));
            } else if (dobbyAi.getIsChatInExpertMode()){
                if (currentEtaSeconds > 0) {
                    interactionCallback.updateExpertIndicator(getEtaMessage());
                } else {
                    interactionCallback.updateExpertIndicator(context.getString(R.string.contacting_human_expert));
                }
            } else if (dobbyAi.getUserAskedForExpertMode() && !dobbyAi.getIsChatInExpertMode()){
                interactionCallback.updateExpertIndicator(context.getString(R.string.pre_human_contact_tests));
            } else {
                interactionCallback.hideExpertIndicator();
            }
        }
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
        dobbyAi.updatedEtaAvailable(currentEtaSeconds);
        updateExpertIndicator();
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


    //Neo callbacks

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

    private void maybeAskForRating() {
        if (ratingsManager.shouldBeAllowedToAskForRating()) {
            dobbyAi.askUserForRatingTheApp();
        }
    }

    //EventBus events
    @Subscribe
    public void listenToEventBus(final DobbyEvent event) {
        if (event.getEventType() == DobbyEvent.EventType.EXPERT_ASKED_FOR_ACTION) {
            //Switching thread so we don't block the event bus
            scheduledExecutorService.schedule(new Runnable() {
                @Override
                public void run() {
                    dobbyAi.parseMessageFromExpert((String) event.getPayload());
                }
            }, 0, TimeUnit.MILLISECONDS);
        }
    }

}
