package com.inceptai.dobby.ai;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.utils.DobbyLog;
import com.inceptai.dobby.utils.Utils;

import java.util.Arrays;

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


    private static final String CLIENT_ACCESS_TOKEN = "81dbd5289ee74637bf582fc3112b7dcb";
    private AIConfiguration aiConfiguration;
    private Context context;
    private AIService aiService;
    private AIDataService aiDataService;
    private DobbyThreadpool threadpool;


    public interface ResultListener {
        void onResult(Action action, Result result);
    }


    public ApiAiClient(Context context, DobbyThreadpool threadpool) {
        this.context = context;
        this.threadpool = threadpool;
    }

    /**
     * Connects to a server using a client access token.
     */
    public void connect() {
        aiConfiguration = new AIConfiguration(CLIENT_ACCESS_TOKEN,
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);
        aiService = AIService.getService(context, aiConfiguration);
        aiService.setListener(this);
        aiDataService = new AIDataService(context, aiConfiguration);
    }

    public void sendTextQuery(@Nullable final String query,
                              @Nullable final String event,
                              final ResultListener listener) {
        Preconditions.checkState(query != null || event != null);
        threadpool.submit(new Runnable() {
            @Override
            public void run() {
                final AIRequest aiRequest = new AIRequest();
                if (query != null) {
                    aiRequest.setQuery(query);
                }
                if (event != null) {
                    aiRequest.setEvent(new AIEvent(event));
                }
                try {
                    DobbyLog.i("Submitting query: " + query);
                    final AIResponse response = aiDataService.request(aiRequest);
                    DobbyLog.i(" Response:" + GsonFactory.getGson().toJson(response.toString()));
                    processResult(response.getResult(), listener);
                } catch (AIServiceException exception ) {
                    DobbyLog.e("Api.ai Exception: " + exception);
                }
            }
        });
    }

    void processTextQueryOffline(@Nullable final String query,
                                 @Nullable final String event,
                                 @Action.ActionType final int lastAction,
                                 final ResultListener listener) {
        DobbyLog.v("Submitting offline query");
        Preconditions.checkState(query != null || event != null);
        Action actionToReturn = new Action(Utils.EMPTY_STRING, Action.ActionType.ACTION_TYPE_NONE);
        if (query != null && ! query.equals(Utils.EMPTY_STRING)) {
            DobbyLog.v("Submitting offline query with text " + query);
            if (Utils.grepForString(query, Arrays.asList("run", "test", "bandwidth", "find"))) {
                //Run bw test
                actionToReturn = new Action("Sure I can run some tests for you. " +
                        "I will start with a quick wifi check ... ",
                        Action.ActionType.ACTION_TYPE_WIFI_CHECK);

            } else if (Utils.grepForString(query, Arrays.asList("slow", "sucks", "bad", "faults",
                    "doesn't", "laggy", "buffering", "disconnected", "problem", "shit"))) {
                //Slow internet diagnosis
                actionToReturn = new Action("Ah that's not good. Let me see whats going on. " +
                        "I will run some quick tests for you ! ",
                        Action.ActionType.ACTION_TYPE_WIFI_CHECK);
            } else if (Utils.grepForString(query, Arrays.asList("check", "how is my wifi", "check speed", "Check wifi"))) {
                //Check wifi
                actionToReturn = new Action("Ah that's not good. Let me see whats going on. " +
                    "I will run some quick wifi tests for you ! ",
                    Action.ActionType.ACTION_TYPE_WIFI_CHECK);
            } else if (Utils.grepForString(query, Arrays.asList("cancel", "stop", "later", "skip",
                    "never mind", "dismiss", "forget", "no", "nm"))) {
                if (lastAction == Action.ActionType.ACTION_TYPE_ASK_FOR_LONG_SUGGESTION || lastAction == Action.ActionType.ACTION_TYPE_ASK_FOR_BW_TESTS) {
                    actionToReturn = new Action("Ok sure. Hope I was of some help ! " +
                            "Let me know if I can answer any other questions.",
                            Action.ActionType.ACTION_TYPE_NONE);
                } else {
                    //Cancel intent
                    actionToReturn = new Action("No worries, I am cancelling the tests. " +
                            "Let me know if I can be of any other help ",
                            Action.ActionType.ACTION_TYPE_CANCEL_BANDWIDTH_TEST);
                }
            } else if (Utils.grepForString(query, Arrays.asList("show", "yes", "sure", "of course", "sounds good", "ok", "kk", "k"))) {
                //Show long suggestion
                //Check if your last action was shown short suggestion
                if (lastAction == Action.ActionType.ACTION_TYPE_ASK_FOR_LONG_SUGGESTION) {
                    actionToReturn = new Action(Utils.EMPTY_STRING,
                            Action.ActionType.ACTION_TYPE_SHOW_LONG_SUGGESTION);
                } else if (lastAction == Action.ActionType.ACTION_TYPE_SHOW_LONG_SUGGESTION) {
                    actionToReturn = new Action("Hope I was of some help. " +
                            "Let me know if I can answer other questions.",
                            Action.ActionType.ACTION_TYPE_NONE);
                } else if (lastAction == Action.ActionType.ACTION_TYPE_ASK_FOR_BW_TESTS) {
                    actionToReturn = new Action("Sure, I will run some speed tests for you now. Hold tight ...",
                            Action.ActionType.ACTION_TYPE_DIAGNOSE_SLOW_INTERNET);
                }
            } else if (Utils.grepForString(query, Arrays.asList("details", "more", "summary"))) {
                //Show long suggestion
                //Check if your last action was shown short suggestion
                actionToReturn = new Action(Utils.EMPTY_STRING,
                        Action.ActionType.ACTION_TYPE_SHOW_LONG_SUGGESTION);
            } else if (Utils.grepForString(query, Arrays.asList("About WifiExpert", "About"))) {
                    actionToReturn = new Action("Wifi Expert is your personal IT manager that helps " +
                            "you diagnose issues with your wireless network. " +
                            "It is developed by Incept AI, a company aimed at improving " +
                            "network management through smart monitoring and analytics.",
                        Action.ActionType.ACTION_TYPE_NONE);
            } else {
                //Default fallback
                actionToReturn = new Action("I'm sorry, I don't support that yet. " +
                        "You can say things like \"Run speed test\", \"why is my internet slow\" " +
                        "or \"why is my wireless slow\"",
                        Action.ActionType.ACTION_TYPE_DEFAULT_FALLBACK);
            }
        } else if (event != null && ! event.equals(Utils.EMPTY_STRING)) {
            DobbyLog.v("Submitting offline event with text " + event);
            //Process the events
            if (event.equals(APIAI_WELCOME_EVENT)) {
                actionToReturn = new Action("Hi there,  I can help you if you have questions about " +
                        "your network. You can say things like \"run tests\" or \"Is my wifi bad\" " +
                        "or \"why is my internet slow\" etc.",
                        Action.ActionType.ACTION_TYPE_WELCOME);
            } else if (event.equals(APIAI_SHORT_SUGGESTION_SHOWN_EVENT)) {
                actionToReturn = new Action("Do you want more details on my analysis ?",
                        Action.ActionType.ACTION_TYPE_ASK_FOR_LONG_SUGGESTION);
            } else if (event.equals(APIAI_LONG_SUGGESTION_SHOWN_EVENT)) {

            } else if (event.equals(APIAI_WIFI_ANALYSIS_SHOWN_EVENT)) {
                actionToReturn = new Action("Do you want to run detailed tests to see why we are offline ?",
                        Action.ActionType.ACTION_TYPE_ASK_FOR_BW_TESTS);
            }
        }
        DobbyLog.v("Sending action to listeners" + actionToReturn.getAction() + " with user response: " + actionToReturn.getUserResponse());
        listener.onResult(actionToReturn, null);
    }

    private void processResult(Result result, ResultListener listener) {
        String action = result.getAction();
        String response = result.getFulfillment().getSpeech();
        if (response == null) {
            response = Utils.EMPTY_STRING;
        }

        @Action.ActionType int actionInt = Action.ActionType.ACTION_TYPE_NONE;
        switch (action) {
            case APIAI_ACTION_DIAGNOSE_SLOW_INTERNET:
                actionInt = Action.ActionType.ACTION_TYPE_DIAGNOSE_SLOW_INTERNET;
                break;
            case APIAI_PERFORM_BW_TEST_RETURN_RESULT:
                actionInt = Action.ActionType.ACTION_TYPE_BANDWIDTH_TEST;
                break;
            case APIAI_ACTION_CANCEL:
                actionInt = Action.ActionType.ACTION_TYPE_CANCEL_BANDWIDTH_TEST;
                break;
            case APIAI_ACTION_SHOW_LONG_SUGGESTION:
                actionInt = Action.ActionType.ACTION_TYPE_SHOW_LONG_SUGGESTION;
                break;
            case APIAI_ACTION_ASK_FOR_LONG_SUGGESTION:
                actionInt = Action.ActionType.ACTION_TYPE_ASK_FOR_LONG_SUGGESTION;
                break;
            case APIAI_ACTION_WIFI_CHECK:
                actionInt = Action.ActionType.ACTION_TYPE_WIFI_CHECK;
                break;
            case APIAI_ACTION_LIST_DOBBY_FUNCTIONS:
                actionInt = Action.ActionType.ACTION_TYPE_LIST_DOBBY_FUNCTIONS;
                break;
            case APIAI_ACTION_ASK_FOR_BW_TESTS:
                actionInt = Action.ActionType.ACTION_TYPE_ASK_FOR_BW_TESTS;
                break;
            case APIAI_ACTION_ASK_FOR_FEEDBACK:
                actionInt = Action.ActionType.ACTION_TYPE_ASK_FOR_FEEDBACK;
                break;
            case APIAI_ACTION_POSITIVE_FEEDBACK:
                actionInt = Action.ActionType.ACTION_TYPE_POSITIVE_FEEDBACK;
                break;
            case APIAI_ACTION_NEGATIVE_FEEDBACK:
                actionInt = Action.ActionType.ACTION_TYPE_NEGATIVE_FEEDBACK;
                break;
            case APIAI_ACTION_NO_FEEDBACK:
                actionInt = Action.ActionType.ACTION_TYPE_NO_FEEDBACK;
                break;
            case APIAI_ACTION_UNSTRUCTURED_FEEDBACK:
                actionInt = Action.ActionType.ACTION_TYPE_UNSTRUCTURED_FEEDBACK;
                break;
        }
        listener.onResult(new Action(response, actionInt), result);
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
