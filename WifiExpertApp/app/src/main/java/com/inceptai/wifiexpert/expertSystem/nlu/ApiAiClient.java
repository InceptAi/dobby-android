package com.inceptai.wifiexpert.expertSystem.nlu;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.inceptai.wifiexpert.utils.DobbyLog;
import com.inceptai.wifiexpert.utils.Utils;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;

import ai.api.AIListener;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.android.AIDataService;
import ai.api.android.AIService;
import ai.api.android.GsonFactory;
import ai.api.model.AIError;
import ai.api.model.AIEvent;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;

import static com.inceptai.wifiexpert.expertSystem.nlu.ApiAiAction.ActionType.ACTION_TYPE_ASK_FOR_RESUMING_EXPERT_CHAT;


/**
 * Created by arunesh on 3/28/17.
 */

public class ApiAiClient implements AIListener {
    private static final String CANNED_RESPONSE = "We are working on it.";

    //API AI events
    public static final String APIAI_WELCOME_EVENT = "welcome_dobby_event";
    public static final String APIAI_SHORT_SUGGESTION_SHOWN_EVENT = "short_suggestion_shown_event";
    public static final String APIAI_LONG_SUGGESTION_SHOWN_EVENT = "long_suggestion_shown_event";
    public static final String APIAI_WIFI_ANALYSIS_SHOWN_EVENT = "wifi_analysis_shown_event";
    public static final String APIAI_RUN_TESTS_FOR_EXPERT_EVENT = "run_tests_for_expert_event";
    public static final String APIAI_WELCOME_AND_RESUME_EXPERT_EVENT = "ask_user_for_resuming_expert_chat_event";


    //API AI actions
    public static final String APIAI_ACTION_DIAGNOSE_SLOW_INTERNET = "diagnose-slow-internet-action";
    public static final String APIAI_PERFORM_BW_TEST_RETURN_RESULT = "perform-bw-test-return-result";
    public static final String APIAI_ACTION_CANCEL = "cancel-bw-tests";
    public static final String APIAI_ACTION_SHOW_LONG_SUGGESTION = "show-long-suggestion";
    public static final String APIAI_ACTION_ASK_FOR_LONG_SUGGESTION = "ask-for-long-suggestion";
    public static final String APIAI_ACTION_WIFI_CHECK = "run-wifi-check-action";
    public static final String APIAI_ACTION_LIST_DOBBY_FUNCTIONS = "list-dobby-functions-action";
    public static final String APIAI_ACTION_ASK_FOR_BW_TESTS = "ask-to-run-bw-tests";
    public static final String APIAI_ACTION_ASK_FOR_FEEDBACK = "ask-for-feedback-action";
    public static final String APIAI_ACTION_POSITIVE_FEEDBACK = "user-says-app-helpful";
    public static final String APIAI_ACTION_NEGATIVE_FEEDBACK = "user-says-app-not-helpful";
    public static final String APIAI_ACTION_NO_FEEDBACK = "user-says-no-to-giving-feedback";
    public static final String APIAI_ACTION_UNSTRUCTURED_FEEDBACK = "user-feedback-unstructured";
    public static final String APIAI_ACTION_CONTACT_HUMAN_EXPERT = "contact-human-expert-action";
    public static final String APIAI_ACTION_RUN_TESTS_FOR_EXPERT = "run-tests-for-expert-action";
    public static final String APIAI_ACTION_CANCEL_TESTS_FOR_EXPERT = "cancel-expert-tests";
    public static final String APIAI_ACTION_SET_TO_BOT_MODE = "set-chat-to-bot-mode-action";
    public static final String APIAI_ACTION_WELCOME_AND_ASK_USER_FOR_RESUMING_EXPERT_CHAT = "ask-user-for-resuming-expert-chat-action";


    //private static final String CLIENT_ACCESS_TOKEN = "81dbd5289ee74637bf582fc3112b7dcb";
    private AIConfiguration aiConfiguration;
    private Context context;
    private AIService aiService;
    private AIDataService aiDataService;
    private ExecutorService executorService;
    private boolean apiAiAvailable = true;

    public boolean isApiAiAvailable() {
        return apiAiAvailable;
    }

    public void setApiAiAvailable(boolean apiAiAvailable) {
        this.apiAiAvailable = apiAiAvailable;
    }

    public interface ResultListener {
        void onResult(ApiAiAction action, Result result);
    }


    public ApiAiClient(Context context, ExecutorService executorService) {
        this.context = context;
        this.executorService = executorService;
    }

    /**
     * Connects to a server using a client access token.
     */
    public void connect() {
        aiConfiguration = new AIConfiguration(getClientAccessToken(),
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);
        aiService = AIService.getService(context, aiConfiguration);
        aiService.setListener(this);
        aiDataService = new AIDataService(context, aiConfiguration);
    }

    public void sendTextQuery(@Nullable final String query,
                              @Nullable final String event,
                              @ApiAiAction.ActionType final int lastAction,
                              final ResultListener listener) {
        Preconditions.checkState(query != null || event != null);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                final AIRequest aiRequest = new AIRequest();
                if (query != null) {
                    DobbyLog.i("Submitting query: " + query);
                    aiRequest.setQuery(query);
                }
                if (event != null) {
                    DobbyLog.i("Submitting event: " + event);
                    aiRequest.setEvent(new AIEvent(event));
                }
                try {
                    final AIResponse response = aiDataService.request(aiRequest);
                    DobbyLog.i(" Response:" + GsonFactory.getGson().toJson(response.toString()));
                    processResult(response.getResult(), listener);
                    setApiAiAvailable(true);
                } catch (AIServiceException exception ) {
                    DobbyLog.e("Api.ai Exception: " + exception);
                    setApiAiAvailable(false);
                    processTextQueryOffline(query, event, lastAction, listener);
                }
            }
        });
    }

    void processTextQueryOffline(@Nullable final String query,
                                 @Nullable final String event,
                                 @ApiAiAction.ActionType final int lastAction,
                                 final ResultListener listener) {
        DobbyLog.v("Submitting offline query");
        Preconditions.checkState(query != null || event != null);
        ApiAiAction actionToReturn = new ApiAiAction(Utils.EMPTY_STRING, ApiAiAction.ActionType.ACTION_TYPE_NONE);
        if (query != null && ! query.equals(Utils.EMPTY_STRING)) {
            DobbyLog.v("Submitting offline query with text " + query);
            if (Utils.grepForString(query, Arrays.asList("contact"))) {
                actionToReturn = new ApiAiAction(Utils.EMPTY_STRING,
                        ApiAiAction.ActionType.ACTION_TYPE_USER_ASKS_FOR_HUMAN_EXPERT);
            } else if (Utils.grepForString(query, Arrays.asList("run", "test", "bandwidth", "find"))) {
                //Run bw test
                actionToReturn = new ApiAiAction("Sure I can run some tests for you. " +
                        "I will start with a quick wifi check ... ",
                        ApiAiAction.ActionType.ACTION_TYPE_WIFI_CHECK);

            } else if (Utils.grepForString(query, Arrays.asList("slow", "sucks", "bad", "faults",
                    "doesn't", "laggy", "buffering", "disconnected", "problem", "shit"))) {
                //Slow internet diagnosis
                actionToReturn = new ApiAiAction("Ah that's not good. Let me see whats going on. " +
                        "I will run some quick tests for you ! ",
                        ApiAiAction.ActionType.ACTION_TYPE_WIFI_CHECK);
            } else if (Utils.grepForString(query, Arrays.asList("check", "how is my wifi", "check speed", "Check wifi"))) {
                //Check wifi
                actionToReturn = new ApiAiAction("Ah that's not good. Let me see whats going on. " +
                    "I will run some quick wifi tests for you ! ",
                        ApiAiAction.ActionType.ACTION_TYPE_WIFI_CHECK);
            } else if (Utils.grepForString(query, Arrays.asList("cancel", "stop", "later", "skip",
                    "never mind", "dismiss", "forget", "no", "nm"))) {
                //User says no
                if (lastAction == ApiAiAction.ActionType.ACTION_TYPE_ASK_FOR_LONG_SUGGESTION || lastAction == ApiAiAction.ActionType.ACTION_TYPE_ASK_FOR_BW_TESTS) {
                    actionToReturn = new ApiAiAction("Ok sure. Hope I was of some help ! " +
                            "Let me know if I can answer any other questions.",
                            ApiAiAction.ActionType.ACTION_TYPE_NONE);
                } else if (lastAction == ApiAiAction.ActionType.ACTION_TYPE_RUN_TESTS_FOR_EXPERT) {
                        actionToReturn = new ApiAiAction("No worries, I am cancelling the tests. " +
                                "Will try to contact the expert now ...",
                                ApiAiAction.ActionType.ACTION_TYPE_CANCEL_TESTS_FOR_EXPERT);
                } else if (lastAction == ACTION_TYPE_ASK_FOR_RESUMING_EXPERT_CHAT) {
                    actionToReturn = new ApiAiAction("No worries, I can help you if you have questions about " +
                            "your network. You can say things like \"run tests\" or  " +
                            "or \"why is my wifi slow\" etc." ,
                            ApiAiAction.ActionType.ACTION_TYPE_SET_CHAT_TO_BOT_MODE);
                } else {
                        actionToReturn = new ApiAiAction("No worries, I am cancelling the tests. " +
                                "Let me know if I can be of any other help ",
                                ApiAiAction.ActionType.ACTION_TYPE_CANCEL_BANDWIDTH_TEST);
                }
            } else if (Utils.grepForString(query, Arrays.asList("show", "yes", "sure", "of course", "sounds good", "ok", "kk", "k"))) {
                //Show long suggestion
                //Check if your last action was shown short suggestion
                if (lastAction == ApiAiAction.ActionType.ACTION_TYPE_ASK_FOR_LONG_SUGGESTION) {
                    actionToReturn = new ApiAiAction(Utils.EMPTY_STRING,
                            ApiAiAction.ActionType.ACTION_TYPE_SHOW_LONG_SUGGESTION);
                } else if (lastAction == ApiAiAction.ActionType.ACTION_TYPE_SHOW_LONG_SUGGESTION) {
                    actionToReturn = new ApiAiAction("Hope I was of some help. " +
                            "Let me know if I can answer other questions.",
                            ApiAiAction.ActionType.ACTION_TYPE_NONE);
                } else if (lastAction == ApiAiAction.ActionType.ACTION_TYPE_ASK_FOR_BW_TESTS) {
                    actionToReturn = new ApiAiAction("Sure, I will run some speed tests for you now. Hold tight ...",
                            ApiAiAction.ActionType.ACTION_TYPE_DIAGNOSE_SLOW_INTERNET);
                } else if (lastAction == ACTION_TYPE_ASK_FOR_RESUMING_EXPERT_CHAT) {
                    actionToReturn = new ApiAiAction(Utils.EMPTY_STRING,
                            ApiAiAction.ActionType.ACTION_TYPE_USER_ASKS_FOR_HUMAN_EXPERT);
                }
            } else if (Utils.grepForString(query, Arrays.asList("details", "more", "summary"))) {
                //Show long suggestion
                //Check if your last action was shown short suggestion
                actionToReturn = new ApiAiAction(Utils.EMPTY_STRING,
                        ApiAiAction.ActionType.ACTION_TYPE_SHOW_LONG_SUGGESTION);
            } else if (Utils.grepForString(query, Arrays.asList("About WifiExpert", "About"))) {
                    actionToReturn = new ApiAiAction("Wifi Expert is your personal IT manager that helps " +
                            "you diagnose issues with your wireless network. " +
                            "It is developed by Incept AI, a company aimed at improving " +
                            "network management through smart monitoring and analytics.",
                            ApiAiAction.ActionType.ACTION_TYPE_NONE);
            } else {
                //Default fallback
                actionToReturn = new ApiAiAction("I'm sorry, I don't support that yet. " +
                        "You can say things like \"Run speed test\", \"why is my wifi slow\".",
                        ApiAiAction.ActionType.ACTION_TYPE_DEFAULT_FALLBACK);
            }
        } else if (event != null && ! event.equals(Utils.EMPTY_STRING)) {
            DobbyLog.v("Submitting offline event with text " + event);
            //Process the events
            if (event.equals(APIAI_WELCOME_EVENT)) {
                //Handle the case when user exited in expert mode.
                actionToReturn = new ApiAiAction("Hi there,  I can help you if you have questions about " +
                        "your network. You can say things like \"run tests\" or  " +
                        "or \"why is my internet slow\" etc.",
                        ApiAiAction.ActionType.ACTION_TYPE_WELCOME);
            } else if (event.equals(APIAI_SHORT_SUGGESTION_SHOWN_EVENT)) {
                actionToReturn = new ApiAiAction("Do you want more details on this analysis ?",
                        ApiAiAction.ActionType.ACTION_TYPE_ASK_FOR_LONG_SUGGESTION);
            } else if (event.equals(APIAI_LONG_SUGGESTION_SHOWN_EVENT)) {

            } else if (event.equals(APIAI_WIFI_ANALYSIS_SHOWN_EVENT)) {
                actionToReturn = new ApiAiAction("Do you want to run detailed tests to see why we are offline ?",
                        ApiAiAction.ActionType.ACTION_TYPE_ASK_FOR_BW_TESTS);
            } else if (event.equals(APIAI_RUN_TESTS_FOR_EXPERT_EVENT)) {
                actionToReturn = new ApiAiAction("I will run some tests for you right now and contact an expert with the results.",
                        ApiAiAction.ActionType.ACTION_TYPE_RUN_TESTS_FOR_EXPERT);
            } else if (event.equals(APIAI_WELCOME_AND_RESUME_EXPERT_EVENT)) {
                actionToReturn = new ApiAiAction("Hi, welcome back. You can ask me any question about your network. Do you want to resume your chat with the Wifi experts ?",
                        ACTION_TYPE_ASK_FOR_RESUMING_EXPERT_CHAT);
            }
        }
        DobbyLog.v("Sending action to listeners" + actionToReturn.getAction() + " with user response: " + actionToReturn.getUserResponse());
        listener.onResult(actionToReturn, null);
    }

    private String getClientAccessToken() {
        return "81dbd5289ee74637bf582fc3112b7dcb"; //Create a new model for wifi expert system app
    }

    private void processResult(Result result, ResultListener listener) {
        String action = result.getAction();
        String response = result.getFulfillment().getSpeech();
        if (response == null) {
            response = Utils.EMPTY_STRING;
        }

        @ApiAiAction.ActionType int actionInt = ApiAiAction.ActionType.ACTION_TYPE_NONE;
        switch (action) {
            case APIAI_ACTION_DIAGNOSE_SLOW_INTERNET:
                actionInt = ApiAiAction.ActionType.ACTION_TYPE_DIAGNOSE_SLOW_INTERNET;
                break;
            case APIAI_PERFORM_BW_TEST_RETURN_RESULT:
                actionInt = ApiAiAction.ActionType.ACTION_TYPE_BANDWIDTH_TEST;
                break;
            case APIAI_ACTION_CANCEL:
                actionInt = ApiAiAction.ActionType.ACTION_TYPE_CANCEL_BANDWIDTH_TEST;
                break;
            case APIAI_ACTION_SHOW_LONG_SUGGESTION:
                actionInt = ApiAiAction.ActionType.ACTION_TYPE_SHOW_LONG_SUGGESTION;
                break;
            case APIAI_ACTION_ASK_FOR_LONG_SUGGESTION:
                actionInt = ApiAiAction.ActionType.ACTION_TYPE_ASK_FOR_LONG_SUGGESTION;
                break;
            case APIAI_ACTION_WIFI_CHECK:
                actionInt = ApiAiAction.ActionType.ACTION_TYPE_WIFI_CHECK;
                break;
            case APIAI_ACTION_LIST_DOBBY_FUNCTIONS:
                actionInt = ApiAiAction.ActionType.ACTION_TYPE_LIST_DOBBY_FUNCTIONS;
                break;
            case APIAI_ACTION_ASK_FOR_BW_TESTS:
                actionInt = ApiAiAction.ActionType.ACTION_TYPE_ASK_FOR_BW_TESTS;
                break;
            case APIAI_ACTION_ASK_FOR_FEEDBACK:
                actionInt = ApiAiAction.ActionType.ACTION_TYPE_ASK_FOR_FEEDBACK;
                break;
            case APIAI_ACTION_POSITIVE_FEEDBACK:
                actionInt = ApiAiAction.ActionType.ACTION_TYPE_POSITIVE_FEEDBACK;
                break;
            case APIAI_ACTION_NEGATIVE_FEEDBACK:
                actionInt = ApiAiAction.ActionType.ACTION_TYPE_NEGATIVE_FEEDBACK;
                break;
            case APIAI_ACTION_NO_FEEDBACK:
                actionInt = ApiAiAction.ActionType.ACTION_TYPE_NO_FEEDBACK;
                break;
            case APIAI_ACTION_UNSTRUCTURED_FEEDBACK:
                actionInt = ApiAiAction.ActionType.ACTION_TYPE_UNSTRUCTURED_FEEDBACK;
                break;
            case APIAI_ACTION_CONTACT_HUMAN_EXPERT:
                actionInt = ApiAiAction.ActionType.ACTION_TYPE_USER_ASKS_FOR_HUMAN_EXPERT;
                break;
            case APIAI_ACTION_RUN_TESTS_FOR_EXPERT:
                actionInt = ApiAiAction.ActionType.ACTION_TYPE_RUN_TESTS_FOR_EXPERT;
                break;
            case APIAI_ACTION_CANCEL_TESTS_FOR_EXPERT:
                actionInt = ApiAiAction.ActionType.ACTION_TYPE_CANCEL_TESTS_FOR_EXPERT;
                break;
            case APIAI_ACTION_SET_TO_BOT_MODE:
                actionInt = ApiAiAction.ActionType.ACTION_TYPE_SET_CHAT_TO_BOT_MODE;
                break;
            case APIAI_ACTION_WELCOME_AND_ASK_USER_FOR_RESUMING_EXPERT_CHAT:
                actionInt = ApiAiAction.ActionType.ACTION_TYPE_ASK_FOR_RESUMING_EXPERT_CHAT;
                break;
        }
        if (listener != null) {
            listener.onResult(new ApiAiAction(response, actionInt), result);
        }
    }

    public void startListening() {
        aiService.startListening();
    }

    public void stopListening() {
        aiService.stopListening();
    }

    public void cancelListening() {
        aiService.cancel();
    }

    public void cleanup() {
    }

    void resetContexts() {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                boolean contextsCleared = aiDataService.resetContexts();
                if (contextsCleared) {
                    DobbyLog.v("Context clearing succeeded");
                } else {
                    DobbyLog.v("Context clearing failed");
                }
            }
        });
    }

    /////// AIListener methods :

    @Override
    public void onResult(final  AIResponse result) {
        DobbyLog.i("Action: " + result.getResult().getAction());
    }

    @Override
    public void onError(AIError error) {

    }

    @Override
    public void onAudioLevel(float level) {

    }

    @Override
    public void onListeningStarted() {

    }

    @Override
    public void onListeningCanceled() {

    }

    @Override
    public void onListeningFinished() {

    }
}
