package com.inceptai.dobby.ai;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.utils.DobbyLog;

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

    public static final String APIAI_WELCOME_EVENT = "welcome_dobby_event";
    public static final String APIAI_SHORT_SUGGESTION_SHOWN_EVENT = "short_suggestion_shown_event";


    public static final String APIAI_ACTION_DIAGNOSE_SLOW_INTERNET = "diagnose-slow-internet-action";
    public static final String APIAI_PERFORM_BW_TEST_RETURN_RESULT = "perform-bw-test-return-result";
    public static final String APIAI_ACTION_CANCEL = "cancel";

    public static final String APIAI_ACTION_SI_STARTING_INTENT_NO =
            "slow-internet-starting-intent.slow-internet-starting-intent-no";

    public static final String APIAI_ACTION_SI_STARTING_INTENT_CANCEL =
            "slow-internet-starting-intent.slow-internet-starting-intent-cancel";

    public static final String APIAI_ACTION_SI_STARTING_INTENT_LATER =
            "slow-internet-starting-intent.slow-internet-starting-intent-later";

    public static final String APIAI_ACTION_SI_STARTING_INTENT_YES_YES_CANCEL =
            "slow-internet-starting-intent.slow-internet-starting-intent-yes.slow-internet-starting-intent-yes-cancel";

    public static final String APIAI_ACTION_SI_STARTING_INTENT_YES_YES_LATER =
            "slow-internet-starting-intent.slow-internet-starting-intent-yes.slow-internet-starting-intent-yes-later";

    public static final String APIAI_ACTION_SI_STARTING_INTENT_YES_YES_NO =
            "slow-internet-starting-intent.slow-internet-starting-intent-yes.slow-internet-starting-intent-yes-no";

    public static final String APIAI_ACTION_SHOW_LONG_SUGGESTION = "show-long-suggestion";


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

    public void processResult(Result result, ResultListener listener) {
        String action = result.getAction();
        String response = result.getFulfillment().getSpeech();
        if (response == null || response.isEmpty()) {
            response = CANNED_RESPONSE;
        }

        @Action.ActionType int actionInt = Action.ActionType.ACTION_TYPE_NONE;
        if (APIAI_ACTION_DIAGNOSE_SLOW_INTERNET.equals(action)) {
            actionInt = Action.ActionType.ACTION_TYPE_DIAGNOSE_SLOW_INTERNET;
        } else if (ApiAiClient.APIAI_PERFORM_BW_TEST_RETURN_RESULT.equals(action)) {
            actionInt = Action.ActionType.ACTION_TYPE_BANDWIDTH_TEST;
        } else if((action.contains("cancel") || action.contains("later") || action.contains("no")) && action.contains("test")) {
            actionInt = Action.ActionType.ACTION_TYPE_CANCEL_BANDWIDTH_TEST;
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
