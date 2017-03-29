package com.inceptai.dobby.com.inceptai.dobby.apiai;

import android.content.Context;
import android.util.DebugUtils;
import android.util.Log;

import com.inceptai.dobby.DobbyThreadpool;

import ai.api.AIListener;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.android.AIDataService;
import ai.api.android.AIService;
import ai.api.model.AIError;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Result;

import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * Created by arunesh on 3/28/17.
 */

public class ApiAiClient implements AIListener {
    private static final String CLIENT_ACCESS_TOKEN = "81dbd5289ee74637bf582fc3112b7dcb";
    private AIConfiguration aiConfiguration;
    private Context context;
    private AIService aiService;
    private AIDataService aiDataService;
    private DobbyThreadpool threadpool;


    public interface ResultListener {
        public void onResult(Result result);
    }


    public ApiAiClient(Context context, DobbyThreadpool threadpool) {
        this.context = context;
        this.threadpool = threadpool;
    }

    /**
     * Connects to a server using a client access token.
     */
    public void connect() {
        aiConfiguration = new AIConfiguration("CLIENT_ACCESS_TOKEN",
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);
        aiService = AIService.getService(context, aiConfiguration);
        aiService.setListener(this);
        aiDataService = new AIDataService(context, aiConfiguration);
    }

    public void sendTextQuery(final String query, final ResultListener listener) {

        threadpool.submit(new Runnable() {
            @Override
            public void run() {
                final AIRequest aiRequest = new AIRequest();
                aiRequest.setQuery(query);
                try {
                    final AIResponse response = aiDataService.request(aiRequest);
                    listener.onResult(response.getResult());
                } catch (AIServiceException exception ) {
                    Log.e(TAG, "Api.ai Exception: " + exception);
                }
            }
        });
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

    @Override
    public void onResult(final  AIResponse result) {
        Log.i(TAG, "Action: " + result.getResult().getAction());
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
