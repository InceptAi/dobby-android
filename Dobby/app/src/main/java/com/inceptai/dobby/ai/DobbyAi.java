package com.inceptai.dobby.ai;

import android.content.Context;
import android.location.Location;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.dobby.DobbyAnalytics;
import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.NetworkLayer;
import com.inceptai.dobby.database.ActionDatabaseWriter;
import com.inceptai.dobby.database.ActionRecord;
import com.inceptai.dobby.database.FailureDatabaseWriter;
import com.inceptai.dobby.database.InferenceDatabaseWriter;
import com.inceptai.dobby.eventbus.DobbyEvent;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.model.PingStats;
import com.inceptai.dobby.speedtest.BandwidthObserver;
import com.inceptai.dobby.speedtest.BandwidthResult;
import com.inceptai.dobby.speedtest.BandwidthTestCodes;
import com.inceptai.dobby.utils.DobbyLog;
import com.inceptai.dobby.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Singleton;

import ai.api.model.Result;

import static com.inceptai.dobby.DobbyApplication.TAG;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_ASK_FOR_BW_TESTS;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_ASK_FOR_FEEDBACK;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_ASK_FOR_LONG_SUGGESTION;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_ASK_FOR_RESUMING_EXPERT_CHAT;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_BANDWIDTH_PING_WIFI_TESTS;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_BANDWIDTH_TEST;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_CANCEL_BANDWIDTH_TEST;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_CANCEL_TESTS_FOR_EXPERT;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_CONTACT_HUMAN_EXPERT;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_DEFAULT_FALLBACK;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_DIAGNOSE_SLOW_INTERNET;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_LIST_DOBBY_FUNCTIONS;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_NEGATIVE_FEEDBACK;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_NONE;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_NO_FEEDBACK;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_POSITIVE_FEEDBACK;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_RUN_TESTS_FOR_EXPERT;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_SET_CHAT_TO_BOT_MODE;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_SHOW_LONG_SUGGESTION;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_SHOW_SHORT_SUGGESTION;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_UNKNOWN;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_UNSTRUCTURED_FEEDBACK;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_USER_ASKS_FOR_HUMAN_EXPERT;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_WELCOME;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_WIFI_CHECK;

/**
 * This class is responsible for managing the user queries and showing the responses by working
 * with the main activity.
 * It can be made to send queries to API AI's server or use a local/another AI system.
 */

@Singleton
public class DobbyAi implements ApiAiClient.ResultListener, InferenceEngine.ActionListener {
    private static final boolean CLEAR_STATS_EVERY_TIME_USER_ASKS_TO_RUN_TESTS = true;
    private static final long MAX_ETA_TO_MARK_EXPERT_AS_LISTENING_SECONDS = 180;

    private Context context;
    private DobbyThreadpool threadpool;
    private ApiAiClient apiAiClient;

    @Nullable private ResponseCallback responseCallback;
    private InferenceEngine inferenceEngine;
    private boolean useApiAi = false; // We do not use ApiAi for the WifiDoc app.
    private AtomicBoolean repeatBwWifiPingAction;
    private SuggestionCreator.Suggestion lastSuggestion;
    private @Action.ActionType int lastAction;
    private boolean longSuggestionShown = false;
    private boolean wifiCheckDone = false;
    private boolean shortSuggestionShown = false;
    private boolean userAskedForHumanExpert = false;
    private boolean chatInExpertMode = false;
    private boolean isExpertListening = false;
    private boolean resumedWithExpertMode = false;



    @Inject
    NetworkLayer networkLayer;
    @Inject
    DobbyEventBus eventBus;
    @Inject
    DobbyAnalytics dobbyAnalytics;
    @Inject
    DobbyApplication dobbyApplication;
    @Inject
    ActionDatabaseWriter actionDatabaseWriter;

    public interface ResponseCallback {
        void showBotResponseToUser(String text);
        void showRtGraph(RtDataSource<Float, Integer> rtDataSource);
        void observeBandwidth(BandwidthObserver observer);
        void cancelTests();
        void showUserActionOptions(List<Integer> userResponseTypes);
        void showBandwidthViewCard(DataInterpreter.BandwidthGrade bandwidthGrade);
        void showNetworkInfoViewCard(DataInterpreter.WifiGrade wifiGrade, String isp, String ip);
        void showDetailedSuggestions(SuggestionCreator.Suggestion suggestion);
        void contactExpertAndGetETA();
        void onUserMessageAvailable(String text, boolean sendMessageToExpert);
        void showStatus(String text);
        void switchedToExpertMode();
        void switchedToBotMode();
        void switchedToExpertIsListeningMode();
        void userAskedForExpert();
    }

    @Inject
    public DobbyAi(DobbyThreadpool threadpool,
                   InferenceDatabaseWriter inferenceDatabaseWriter,
                   FailureDatabaseWriter failureDatabaseWriter,
                   DobbyApplication dobbyApplication) {
        this.context = dobbyApplication.getApplicationContext();
        this.threadpool = threadpool;
        useApiAi = DobbyApplication.isDobbyFlavor();
        inferenceEngine = new InferenceEngine(threadpool.getScheduledExecutorService(),
                this, inferenceDatabaseWriter, failureDatabaseWriter, dobbyApplication);
        if (useApiAi) {
            initApiAiClient();
        }
        repeatBwWifiPingAction = new AtomicBoolean(false);
        lastAction = ACTION_TYPE_UNKNOWN;
        initChatToBotState();
    }

    @Action.ActionType
    public int getLastAction() {
        return lastAction;
    }

    public void setLastAction(int lastAction) {
        this.lastAction = lastAction;
    }

    public void setResponseCallback(ResponseCallback responseCallback) {
        this.responseCallback = responseCallback;
    }

    @Override
    public void onResult(final Action action, @Nullable final Result result) {
        // Thread switch (to release any Api.Ai threads).
        threadpool.submit(new Runnable() {
            @Override
            public void run() {
                takeAction(action);
            }
        });
        if (result != null) {
            DobbyLog.i("Got response Action: " + result.toString());
        }
    }

    public void startMic() {

    }

    public void initChatToBotState() {
        userAskedForHumanExpert = false;
        chatInExpertMode = false;
        isExpertListening = false;
        resumedWithExpertMode = false;
    }
    
    public boolean getIsExpertListening() {
        return isExpertListening;
    }

    public boolean getIsChatInExpertMode() {
        return chatInExpertMode;
    }

    public boolean getUserAskedForExpertMode() {
        return userAskedForHumanExpert;
    }

    /**
     * Implements the action returned by the InferenceEngine.
     *
     * @param action Action to be taken.
     */
    @Override
    public void takeAction(Action action) {
        DobbyLog.v("In takeAction with action: " + action.getAction() + " and user resp: " + action.getUserResponse());
        setLastAction(action.getAction());
        showMessageToUser(action.getUserResponse());

        //Suppress non-action messages from bot when the expert is talking to the user.
        //if (!(chatInExpertMode && Action.isNonEssentialAction(action))) {
        //    showMessageToUser(action.getUserResponse());
        //}

        if (responseCallback != null) {
            responseCallback.showUserActionOptions(getPotentialUserResponses(lastAction));
        }
        switch (action.getAction()) {
            case ACTION_TYPE_BANDWIDTH_TEST:
                DobbyLog.i("Starting ACTION BANDWIDTH TEST.");
                dobbyAnalytics.wifiExpertRunningBandwidthTests();
                //postBandwidthTestOperation();
                postAllOperations();
                if (CLEAR_STATS_EVERY_TIME_USER_ASKS_TO_RUN_TESTS && repeatBwWifiPingAction.getAndSet(true)) {
                    //Clear the ping/wifi cache to get fresh results.
                    clearCache();
                }
                return;
            case ACTION_TYPE_CANCEL_BANDWIDTH_TEST:
                DobbyLog.i("Starting ACTION CANCEL BANDWIDTH TEST.");
                cancelBandwidthTest();
                break;
            case ACTION_TYPE_DIAGNOSE_SLOW_INTERNET:
                Action newAction = inferenceEngine.addGoal(InferenceEngine.Goal.GOAL_DIAGNOSE_SLOW_INTERNET);
                takeAction(newAction);
                return;
            case ACTION_TYPE_BANDWIDTH_PING_WIFI_TESTS:
                if (CLEAR_STATS_EVERY_TIME_USER_ASKS_TO_RUN_TESTS && repeatBwWifiPingAction.getAndSet(true)) {
                    //Clear the ping/wifi cache to get fresh results.
                    clearCache();
                }
                DobbyLog.i("Going into postAllOperations()");
                postAllOperations();
                dobbyAnalytics.wifiExpertRunningBandwidthTests();
                break;
            case ACTION_TYPE_SHOW_SHORT_SUGGESTION:
                //Send event for showing short suggestion
                if (lastSuggestion != null) {
                    shortSuggestionShown = true;
                    if (responseCallback != null) {
                        DataInterpreter.BandwidthGrade lastBandwidthGrade = lastSuggestion.suggestionCreatorParams.bandwidthGrade;
                        if (!DataInterpreter.isUnknown(lastBandwidthGrade.getDownloadBandwidthMetric()) ||
                                !DataInterpreter.isUnknown(lastBandwidthGrade.getDownloadBandwidthMetric())) {
                            responseCallback.showBandwidthViewCard(lastBandwidthGrade);
                        }
                    }
                    showMessageToUser(lastSuggestion.getTitle());
                    dobbyAnalytics.wifiExpertShowShortSuggestion(lastSuggestion.getTitle());
                }
                //See if we are pending on contacting a human expert.
                // If so, contact the human here and don't display long suggestion.
                // User has not run a full test -- run a full test and then contact the expert
                if (userAskedForHumanExpert) {
                    String messageToShow = "I will contact a human expert with these results...";
                    Action actionToTake = new Action(messageToShow, ACTION_TYPE_CONTACT_HUMAN_EXPERT);
                    takeAction(actionToTake);
                } else {
                    sendEvent(ApiAiClient.APIAI_SHORT_SUGGESTION_SHOWN_EVENT);
                }
                break;
            case ACTION_TYPE_SHOW_LONG_SUGGESTION:
                //Show the long suggestion here and send the event
                if (responseCallback != null) {
                    responseCallback.showDetailedSuggestions(lastSuggestion);
                }
                if (lastSuggestion != null) {
                    sendEvent(ApiAiClient.APIAI_LONG_SUGGESTION_SHOWN_EVENT);
                    longSuggestionShown = true;
                    dobbyAnalytics.wifiExpertShowLongSuggestion(lastSuggestion.getTitle());
                }
                break;
            case ACTION_TYPE_NONE:
                break;
            case ACTION_TYPE_ASK_FOR_LONG_SUGGESTION:
                dobbyAnalytics.wifiExpertAskForLongSuggestion();
                break;
            case ACTION_TYPE_WIFI_CHECK:
                if (responseCallback != null) {
                    responseCallback.showNetworkInfoViewCard(getCurrentWifiGrade(), getCurrentIsp(), getCurrentIp());
                }
                //We only proceed with bw tests requests if wifi is online -- otherwise there is no point.
                // We can actually analyze this further and run the tests to show detailed analysis.
                //if (networkLayer.isWifiOnline()) {
                if (!networkLayer.isWifiOff()) {
                    sendEvent(ApiAiClient.APIAI_WIFI_ANALYSIS_SHOWN_EVENT);
                }
                wifiCheckDone = true;
                dobbyAnalytics.wifiExpertWifiCheck();
                break;
            case ACTION_TYPE_LIST_DOBBY_FUNCTIONS:
                dobbyAnalytics.wifiExpertListDobbyFunctions();
                break;
            case ACTION_TYPE_ASK_FOR_BW_TESTS:
                dobbyAnalytics.wifiExpertAskForBwTestsAfterWifiCheck();
                break;
            case ACTION_TYPE_WELCOME:
                dobbyAnalytics.wifiExpertWelcomeMessageShown();
                break;
            case ACTION_TYPE_ASK_FOR_FEEDBACK:
                if (lastSuggestion == null) {
                    dobbyAnalytics.wifiExpertAskForFeedbackAfterWifiCheck();
                }
                break;
            case ACTION_TYPE_POSITIVE_FEEDBACK:
                if (longSuggestionShown) {
                    dobbyAnalytics.wifiExpertPositiveFeedbackAfterLongSuggestion();
                } else if (shortSuggestionShown) {
                    dobbyAnalytics.wifiExpertPositiveFeedbackAfterShortSuggestion();
                } else {
                    dobbyAnalytics.wifiExpertPositiveFeedbackAfterWifiCheck();
                }
                break;
            case ACTION_TYPE_NEGATIVE_FEEDBACK:
                if (longSuggestionShown) {
                    dobbyAnalytics.wifiExpertNegativeFeedbackAfterLongSuggestion();
                } else if (shortSuggestionShown) {
                    dobbyAnalytics.wifiExpertNegativeFeedbackAfterShortSuggestion();
                } else {
                    dobbyAnalytics.wifiExpertNegativeFeedbackAfterWifiCheck();
                }
                break;
            case ACTION_TYPE_NO_FEEDBACK:
                if (longSuggestionShown) {
                    dobbyAnalytics.wifiExpertNoFeedbackAfterLongSuggestion();
                } else if (shortSuggestionShown) {
                    dobbyAnalytics.wifiExpertNoFeedbackAfterShortSuggestion();
                } else {
                    dobbyAnalytics.wifiExpertNoFeedbackAfterWifiCheck();
                }
                break;
            case ACTION_TYPE_UNSTRUCTURED_FEEDBACK:
                if (longSuggestionShown) {
                    dobbyAnalytics.wifiExpertUnstructuredFeedbackAfterLongSuggestion(action.getUserResponse());
                } else if (shortSuggestionShown) {
                    dobbyAnalytics.wifiExpertUnstructuredFeedbackAfterShortSuggestion(action.getUserResponse());
                } else {
                    dobbyAnalytics.wifiExpertUnstructuredFeedbackAfterWifiCheck(action.getUserResponse());
                }
                break;
            case ACTION_TYPE_USER_ASKS_FOR_HUMAN_EXPERT:
                String messageToShow = "Sure, I will contact a person with Wifi expertise who can look at your problem. ";
                showMessageToUser(messageToShow);
                if (lastSuggestion != null) {
                    //User has already run a test and now needs help -- contact the expert
                    contactExpert();
                } else {
                    // User has not run a full test -- run a full test and then contact the expert
                    sendEvent(ApiAiClient.APIAI_RUN_TESTS_FOR_EXPERT_EVENT);
                }
                userAskedForHumanExpert = true;
                if (responseCallback != null) {
                    responseCallback.userAskedForExpert();
                }
                break;
            case ACTION_TYPE_CONTACT_HUMAN_EXPERT:
                //We are here and we have the results. Now tell the user ETA and next steps.
                //chatInExpertMode = true;
                setChatInExpertMode();
                //Fulfilled the request to contact expert, so setting to false.
                userAskedForHumanExpert = false;
                String expertContactMessage = "Contacting Wifi Expert and getting ETA now ...";
                //showMessageToUser(expertContactMessage);
                if (responseCallback != null) {
                    responseCallback.showBotResponseToUser(expertContactMessage);
                    responseCallback.contactExpertAndGetETA();
                }
                break;
            case ACTION_TYPE_RUN_TESTS_FOR_EXPERT:
                //Run all tests for expert
                if (!wifiCheckDone && responseCallback != null) {
                    responseCallback.showNetworkInfoViewCard(getCurrentWifiGrade(), getCurrentIsp(), getCurrentIp());
                    wifiCheckDone = true;
                }
                if (!networkLayer.isWifiOff()) {
                    //run full tests
                    postAllOperations();
                } else {
                    //Contact the expert
                    contactExpert();
                }
                break;
            case ACTION_TYPE_CANCEL_TESTS_FOR_EXPERT:
                DobbyLog.i("Starting ACTION CANCEL BANDWIDTH TEST.");
                cancelBandwidthTest();
                //Contact the expert
                contactExpert();
                break;
            case ACTION_TYPE_SET_CHAT_TO_BOT_MODE:
                setChatInBotMode();
                break;
            default:
                DobbyLog.i("Unknown Action");
                break;
        }
    }

    public void updatedEtaAvailable(long currentEtaSeconds) {
        if (chatInExpertMode && currentEtaSeconds <= MAX_ETA_TO_MARK_EXPERT_AS_LISTENING_SECONDS) {
            isExpertListening = true;
            if (responseCallback != null) {
                responseCallback.switchedToExpertIsListeningMode();
            }
        } else {
            isExpertListening = false;
        }
    }

    public void setChatResumedInExpertMode() {
         resumedWithExpertMode = true;
    }

    public void setChatInExpertMode() {
        chatInExpertMode = true;
        if (responseCallback != null) {
            responseCallback.switchedToExpertMode();
        }
    }

    public void setChatInBotMode() {
        initChatToBotState();
        if (responseCallback != null) {
            responseCallback.switchedToBotMode();
        }
    }

    @Override
    public void suggestionsAvailable(SuggestionCreator.Suggestion suggestion) {
        DobbyLog.v("Updating last suggestion to:" + suggestion.toString());
        lastSuggestion = suggestion;
        //Create a new action
        Action shortSuggestionAction = new Action(Utils.EMPTY_STRING, ACTION_TYPE_SHOW_SHORT_SUGGESTION);
        takeAction(shortSuggestionAction);
        eventBus.postEvent(DobbyEvent.EventType.SUGGESTIONS_AVAILABLE, suggestion);
    }

    @Override
    public Location fetchLocation() {
        return networkLayer.fetchLastKnownLocation();
    }

    public void sendQuery(String text, boolean isButtonActionText ) {

        if (responseCallback != null) {
            DobbyLog.v("DobbyAi: onUserMessageAvailable with text " + text);
            responseCallback.onUserMessageAvailable(text, chatInExpertMode);
        }

        if (useApiAi) {
            if (!chatInExpertMode || isButtonActionText) {
                if (networkLayer.isWifiOnline()) {
                    if (isQueryRequestForHumanExpert(text)) {
                        //User is asking for human through button,
                        //so reset the contexts before sending the request
                        //apiAiClient.resetContexts();
                    }
                    apiAiClient.sendTextQuery(text, null, getLastAction(), this);
                } else {
                    //Context doesn't matter here.
                    apiAiClient.processTextQueryOffline(text, null, getLastAction(), this);
                }
            }
        } else {
            DobbyLog.w("Ignoring text query for Wifi doc version :" + text);
        }

    }

    public void contactExpert() {
        Action actionToTake = new Action(Utils.EMPTY_STRING, ACTION_TYPE_CONTACT_HUMAN_EXPERT);
        takeAction(actionToTake);
    }

    public void sendEvent(String text) {
        if (useApiAi) {
            if (networkLayer.isWifiOnline()) {
                apiAiClient.sendTextQuery(null, text, getLastAction(), this);
            } else {
                apiAiClient.processTextQueryOffline(null, text, getLastAction(), this);
            }
        } else {
            DobbyLog.w("Ignoring events for Wifi doc version :" + text);
        }
    }

    public void sendWelcomeEvent() {
        if (useApiAi) {
            if (!chatInExpertMode) {
                apiAiClient.processTextQueryOffline(null, ApiAiClient.APIAI_WELCOME_EVENT, getLastAction(), this);
            }
//            if (resumedWithExpertMode) {
//                //apiAiClient.resetContexts();
//                apiAiClient.sendTextQuery(null, ApiAiClient.APIAI_WELCOME_AND_RESUME_EXPERT_EVENT, getLastAction(), this);
//            } else {
//                apiAiClient.processTextQueryOffline(null, ApiAiClient.APIAI_WELCOME_EVENT, getLastAction(), this);
//            }
        } else {
            DobbyLog.w("Ignoring events for Wifi doc version :" + ApiAiClient.APIAI_WELCOME_EVENT);
        }
    }

    public void cleanup() {
        networkLayer.cleanup();
        inferenceEngine.cleanup();
        if (useApiAi) {
            apiAiClient.cleanup();
        }
        responseCallback = null;
    }

    private boolean isQueryRequestForHumanExpert(String text) {
        return (text.toLowerCase().contains("contact human"));
    }

    private List<Integer> getPotentialUserResponses(@Action.ActionType int lastActionShownToUser) {
        List<Integer> responseList = new ArrayList<>();
        switch (lastActionShownToUser) {
            case ACTION_TYPE_ASK_FOR_LONG_SUGGESTION:
                responseList.add(UserResponse.ResponseType.YES);
                responseList.add(UserResponse.ResponseType.NO);
                break;
            case ACTION_TYPE_BANDWIDTH_TEST:
            case ACTION_TYPE_RUN_TESTS_FOR_EXPERT:
                responseList.add(UserResponse.ResponseType.CANCEL);
                break;
            case ACTION_TYPE_WIFI_CHECK:
                responseList.add(UserResponse.ResponseType.RUN_BW_TESTS);
                break;
            case ACTION_TYPE_DIAGNOSE_SLOW_INTERNET:
            case ACTION_TYPE_BANDWIDTH_PING_WIFI_TESTS:
                responseList.add(UserResponse.ResponseType.CANCEL);
                break;
            case ACTION_TYPE_SHOW_SHORT_SUGGESTION:
                break;
            case ACTION_TYPE_LIST_DOBBY_FUNCTIONS:
                responseList.add(UserResponse.ResponseType.RUN_ALL_DIAGNOSTICS);
                responseList.add(UserResponse.ResponseType.RUN_BW_TESTS);
                responseList.add(UserResponse.ResponseType.RUN_WIFI_TESTS);
                break;
            case ACTION_TYPE_ASK_FOR_BW_TESTS:
                responseList.add(UserResponse.ResponseType.YES);
                responseList.add(UserResponse.ResponseType.NO);
                break;
            case ACTION_TYPE_NEGATIVE_FEEDBACK:
            case ACTION_TYPE_ASK_FOR_FEEDBACK:
                responseList.add(UserResponse.ResponseType.YES);
                responseList.add(UserResponse.ResponseType.NO);
                responseList.add(UserResponse.ResponseType.NO_COMMENTS);
                break;
            case ACTION_TYPE_ASK_FOR_RESUMING_EXPERT_CHAT:
                responseList.add(UserResponse.ResponseType.YES);
                responseList.add(UserResponse.ResponseType.NO);
//                responseList.add(UserResponse.ResponseType.RUN_ALL_DIAGNOSTICS);
//                responseList.add(UserResponse.ResponseType.RUN_BW_TESTS);
//                responseList.add(UserResponse.ResponseType.RUN_WIFI_TESTS);
                break;
            case ACTION_TYPE_WELCOME:
            case ACTION_TYPE_NONE:
            case ACTION_TYPE_UNKNOWN:
            case ACTION_TYPE_DEFAULT_FALLBACK:
            case ACTION_TYPE_SHOW_LONG_SUGGESTION:
            case ACTION_TYPE_CANCEL_BANDWIDTH_TEST:
            default:
                responseList.add(UserResponse.ResponseType.RUN_ALL_DIAGNOSTICS);
                responseList.add(UserResponse.ResponseType.RUN_BW_TESTS);
                responseList.add(UserResponse.ResponseType.RUN_WIFI_TESTS);
                break;
        }
        //Get detailed suggestions by pressing a button
        if (lastSuggestion != null &&
                lastActionShownToUser != ACTION_TYPE_DIAGNOSE_SLOW_INTERNET &&
                lastActionShownToUser != ACTION_TYPE_BANDWIDTH_PING_WIFI_TESTS) {
            responseList.add(UserResponse.ResponseType.SHOW_LAST_SUGGESTION_DETAILS);
        }
        if (!responseList.contains(UserResponse.ResponseType.CANCEL) && !userAskedForHumanExpert && !chatInExpertMode) {
            responseList.add(UserResponse.ResponseType.CONTACT_HUMAN_EXPERT);
        }
        //Special case since, chatInExpertMode is not properly yet
        if (!responseList.contains(UserResponse.ResponseType.CONTACT_HUMAN_EXPERT) && lastActionShownToUser == ACTION_TYPE_SET_CHAT_TO_BOT_MODE) {
            responseList.add(UserResponse.ResponseType.CONTACT_HUMAN_EXPERT);
        }

        if (userAskedForHumanExpert || chatInExpertMode) {
            for (Iterator<Integer> iter = responseList.listIterator(); iter.hasNext(); ) {
                Integer responseType = iter.next();
                if (responseType == UserResponse.ResponseType.RUN_ALL_DIAGNOSTICS || responseType == UserResponse.ResponseType.RUN_WIFI_TESTS) {
                    iter.remove();
                }
            }
        }
        return responseList;
    }

    private void showMessageToUser(String messageToShow) {
        if (responseCallback != null && messageToShow != null && ! messageToShow.equals(Utils.EMPTY_STRING)) {
            DobbyLog.v("Showing to user message: " + messageToShow);
            responseCallback.showBotResponseToUser(messageToShow);
        }
    }

    private void clearCache() {
        wifiCheckDone = false;
        shortSuggestionShown = false;
        longSuggestionShown = false;

        if (networkLayer != null) {
            networkLayer.clearStatsCache();
        }
        if (inferenceEngine != null) {
            inferenceEngine.clearConditionsAndMetrics();
        }
        lastSuggestion = null;
    }

    private void postBandwidthTestOperation() {
        ComposableOperation operation = bandwidthOperation();
        operation.post();
    }

    private DataInterpreter.WifiGrade getCurrentWifiGrade() {
        return networkLayer.getCurrentWifiGrade();
    }

    private String getCurrentIsp() {
        return networkLayer.getCachedClientIspIfAvailable();
    }

    private String getCurrentIp() {
        return networkLayer.getCachedExternalIpIfAvailable();
    }

    private void postAllOperations() {
        final ComposableOperation bwTest = bandwidthOperation();
        final ComposableOperation wifiScan = wifiScanOperation();
        final ComposableOperation ping = pingOperation();
        final ComposableOperation gatewayLatencyTest = gatewayLatencyTestOperation();
        bwTest.uponCompletion(wifiScan);
        wifiScan.uponCompletion(ping);
        ping.uponCompletion(gatewayLatencyTest);
        bwTest.post();

        // Wire up with IE.
        wifiScan.getFuture().addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    OperationResult result = wifiScan.getFuture().get();
                    DobbyLog.v("DobbyAI: Setting the result for wifiscan");
                }catch (Exception e) {
                    e.printStackTrace(System.out);
                    DobbyLog.w("Exception getting wifi results: " + e.getStackTrace().toString());
                    //Informing inference engine of the error.
                } finally {
                    DobbyLog.v("DobbyAI: Notifying wifi state ");
                    DataInterpreter.WifiGrade wifiGrade = inferenceEngine.notifyWifiState(
                            networkLayer.getWifiState(),
                            networkLayer.getLatestScanResult(),
                            networkLayer.getWifiLinkMode(),
                            networkLayer.getCurrentConnectivityMode());
                    if (wifiGrade != null) {
                        eventBus.postEvent(DobbyEvent.EventType.WIFI_GRADE_AVAILABLE, wifiGrade);
                    }
                }
            }
        }, threadpool.getExecutor());

        ping.getFuture().addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    OperationResult result = ping.getFuture().get();
                    DobbyLog.v("DobbyAI: Getting result for pingFuture");
                    HashMap<String, PingStats> payload = (HashMap<String, PingStats>) result.getPayload();
                    if (payload == null) {
                        //Error.
                        DobbyLog.i("Error starting ping.");
                    } else {
                        DobbyLog.v("DobbyAI: Notifying ping stats with payload " + payload.toString());
                    }
                    //We notify inferencing engine in any case so it knows that ping
                    // failed and can make the inferencing based on other measurements.
                    DataInterpreter.PingGrade pingGrade = inferenceEngine.notifyPingStats(payload, networkLayer.getIpLayerInfo());
                    if (pingGrade != null) {
                        eventBus.postEvent(DobbyEvent.EventType.PING_GRADE_AVAILABLE, pingGrade);
                    }

                } catch (Exception e) {
                    e.printStackTrace(System.out);
                    DobbyLog.w("Exception getting ping results: " + e.getStackTrace().toString());
                    //Informing inference engine of the error.
                    DobbyLog.v("DobbyAI: Notifying ping stats with null payload ");
                    inferenceEngine.notifyPingStats(null, networkLayer.getIpLayerInfo());
                }
            }
        }, threadpool.getExecutor());

        gatewayLatencyTest.getFuture().addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    OperationResult result = gatewayLatencyTest.getFuture().get();
                    DobbyLog.v("DobbyAI: Getting result for download latency future");
                    PingStats payload = (PingStats) result.getPayload();
                    DobbyLog.v("DobbyAI: Notifying download latency stats with payload " + payload);
                    DataInterpreter.HttpGrade httpGrade = inferenceEngine.notifyGatewayHttpStats(payload);
                    if (httpGrade != null) {
                        eventBus.postEvent(DobbyEvent.EventType.GATEWAY_HTTP_GRADE_AVAILABLE, httpGrade);
                    }
                    if (payload == null) {
                        // Error.
                        DobbyLog.i("Error starting ping.");
                    }
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                    DobbyLog.w("Exception getting gateway latency results: " + e.getStackTrace().toString());
                    inferenceEngine.notifyGatewayHttpStats(null);
                }
            }
        }, threadpool.getExecutor());
    }

    private ComposableOperation wifiScanOperation() {
        return new ComposableOperation(threadpool) {
            @Override
            public void post() {
                DobbyLog.v("DobbyAI: Starting wifi operation");
                setFuture(networkLayer.wifiScan());
            }

            @Override
            protected String getName() {
                return "Wifi Scan operation";
            }
        };
    }

    private ComposableOperation pingOperation() {
        return new ComposableOperation(threadpool) {
            @Override
            public void post() {
                DobbyLog.v("DobbyAI: Starting ping operation");
                setFuture(networkLayer.startPing());
            }

            @Override
            protected String getName() {
                return "Ping Operation";
            }
        };
    }

    private ComposableOperation bandwidthOperation() {
        return new ComposableOperation(threadpool) {
            @Override
            public void post() {
                DobbyLog.v("DobbyAI: Starting bw operation");
                setFuture(startBandwidthTest());
            }

            @Override
            protected String getName() {
                return "Bandwidth test operation";
            }
        };
    }

    private ComposableOperation gatewayLatencyTestOperation() {
        return new ComposableOperation(threadpool) {
            @Override
            public void post() {
                DobbyLog.v("DobbyAI: Starting latency operation");
                setFuture(networkLayer.startGatewayDownloadLatencyTest());
            }

            @Override
            protected String getName() {
                return "Gateway latency test operation";
            }
        };
    }

    private ListenableFuture<BandwidthResult> startBandwidthTest() {
        DobbyLog.i("Going to start bandwidth test.");
        @BandwidthTestCodes.TestMode
        int testMode = BandwidthTestCodes.TestMode.DOWNLOAD_AND_UPLOAD;
        BandwidthObserver observer = networkLayer.startBandwidthTest(testMode);
        if (observer == null) {
            Log.w(TAG, "Null observer returned from NL, abandoning bandwidth test.");
            //Notify inference engine of negative bandwidth numbers or that we don't have valid answers for it.
            threadpool.submit(new Runnable() {
                @Override
                public void run() {
                    inferenceEngine.notifyBandwidthTestError(BandwidthTestCodes.TestMode.DOWNLOAD_AND_UPLOAD,
                            BandwidthTestCodes.ErrorCodes.ERROR_WIFI_OFFLINE,
                            "Wifi is offline, so cannot run bw tests", -1.0);
                }
            });
            return null;
        }
        observer.setInferenceEngine(inferenceEngine);
        if (responseCallback != null) {
            // responseCallback.showRtGraph(observer);
            responseCallback.observeBandwidth(observer);
        }
        return observer.asFuture();
    }

    private void cancelBandwidthTest() {
        try {
            networkLayer.cancelBandwidthTests();
            lastSuggestion = null;
            if (responseCallback != null) {
                responseCallback.cancelTests();
            }
            dobbyAnalytics.wifiExpertCancelBandwidthTest();
        } catch (Exception e) {
            DobbyLog.v("Exception while cancelling tests " + e);
        }
    }

    private void initApiAiClient() {
        apiAiClient = new ApiAiClient(context, threadpool);
        apiAiClient.connect();
    }


    public void performAndRecordPingAction() {
        final ComposableOperation ping = pingOperation();
        //Clear the cache of results first
        clearCache();
        ping.getFuture().addListener(new Runnable() {
            @Override
            public void run() {
                DataInterpreter.PingGrade pingGrade = new DataInterpreter.PingGrade();
                try {
                    eventBus.postEvent(DobbyEvent.EventType.EXPERT_ACTION_STARTED);
                    OperationResult result = ping.getFuture().get();
                    DobbyLog.v("DobbyAI: Getting result for pingFuture");
                    HashMap<String, PingStats> payload = (HashMap<String, PingStats>) result.getPayload();
                    if (payload == null) {
                        //Error.
                        DobbyLog.i("Error starting ping.");
                    } else {
                        DobbyLog.v("DobbyAI: Notifying ping stats with payload " + payload.toString());
                    }
                    //We notify inferencing engine in any case so it knows that ping
                    // failed and can make the inferencing based on other measurements.
                    pingGrade = DataInterpreter.interpret(payload, networkLayer.getIpLayerInfo());
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                    DobbyLog.w("Exception getting ping results: " + e.getStackTrace().toString());
                    //Informing inference engine of the error.
                    DobbyLog.v("DobbyAI: Notifying ping stats with null payload ");
                    pingGrade = DataInterpreter.interpret(null, networkLayer.getIpLayerInfo());
                } finally {
                    writeActionRecord(null, null, pingGrade, null);
                    eventBus.postEvent(DobbyEvent.EventType.EXPERT_ACTION_STARTED);
                }
            }
        }, threadpool.getExecutor());
        ping.post();
    }

    public void performAndRecordHttpAction() {

    }

    public void triggerFeedbackRequest() {
        //Sending this event will trigger the feedback loop
        sendEvent(ApiAiClient.APIAI_LONG_SUGGESTION_SHOWN_EVENT);
    }

    public void triggerSwitchToBotMode() {
        setChatInBotMode();
    }


    public void performAndRecordWifiAction() {
        final ComposableOperation wifiScan = wifiScanOperation();
        //Clear the cache of results first
        clearCache();
        wifiScan.getFuture().addListener(new Runnable() {
            @Override
            public void run() {
                DataInterpreter.WifiGrade wifiGrade;
                try {
                    eventBus.postEvent(DobbyEvent.EventType.EXPERT_ACTION_STARTED);
                    OperationResult result = wifiScan.getFuture().get();
                    DobbyLog.v("DobbyAI: Setting the result for wifiscan");
                }catch (Exception e) {
                    e.printStackTrace(System.out);
                    DobbyLog.w("Exception getting wifi results: " + e.getStackTrace().toString());
                    //Informing inference engine of the error.
                } finally {
                    DobbyLog.v("DobbyAI: Notifying wifi state ");
                    wifiGrade = DataInterpreter.interpret(networkLayer.getWifiState(),
                            networkLayer.getLatestScanResult(),
                            networkLayer.getWifiLinkMode(),
                            networkLayer.getCurrentConnectivityMode());
                    writeActionRecord(null, wifiGrade, null, null);
                    eventBus.postEvent(DobbyEvent.EventType.EXPERT_ACTION_COMPLETED);
                }
            }
        }, threadpool.getExecutor());
        wifiScan.post();
    }

    private void writeActionRecord(@Nullable DataInterpreter.BandwidthGrade bandwidthGrade,
                                            @Nullable DataInterpreter.WifiGrade wifiGrade,
                                            @Nullable DataInterpreter.PingGrade pingGrade,
                                            @Nullable DataInterpreter.HttpGrade httpGrade) {
        ActionRecord actionRecord = new ActionRecord();
        HashMap<String, String> phoneInfo = new HashMap<>();
        actionRecord.uid = dobbyApplication.getUserUuid();
        actionRecord.phoneInfo = dobbyApplication.getPhoneInfo();
        actionRecord.appVersion = DobbyApplication.getAppVersion();
        actionRecord.actionType = "UNKNOWN";

        int count = 0;
        //Assign all the grades
        if (bandwidthGrade != null) {
            actionRecord.bandwidthGradeJson = bandwidthGrade.toJson();
            actionRecord.actionType = "BWTEST";
            count++;
        }

        if (wifiGrade != null) {
            actionRecord.wifiGradeJson = wifiGrade.toJson();
            actionRecord.actionType = "WIFI";
            count++;
        }

        if (pingGrade != null) {
            actionRecord.pingGradeJson = pingGrade.toJson();
            actionRecord.actionType = "PING";
            count++;
        }

        if (httpGrade != null) {
            actionRecord.httpGradeJson = httpGrade.toJson();
            actionRecord.actionType = "HTTP";
            count++;
        }

        if (count > 1) {
            actionRecord.actionType = "MIXED";
        }

        Location location = networkLayer.fetchLastKnownLocation();
        if (location != null) {
            actionRecord.lat = location.getLatitude();
            actionRecord.lon = location.getLongitude();
            actionRecord.accuracy = location.getAccuracy();
        }
        //Assign the timestamp
        actionRecord.timestamp = System.currentTimeMillis();
        //Write to db.
        actionDatabaseWriter.writeActionToDatabase(actionRecord);
    }


}
