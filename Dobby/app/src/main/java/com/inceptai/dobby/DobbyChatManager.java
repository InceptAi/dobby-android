package com.inceptai.dobby;

import android.content.Context;
import android.util.Log;

import com.inceptai.dobby.apiai.ApiAiClient;

import ai.api.model.Result;

import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * This class is responsible for managing the user queries and showing the responses by working
 * with the main activity.
 * It can be made to send queries to API AI's server or use a local/another AI system.
 */

public class DobbyChatManager implements ApiAiClient.ResultListener {

    private Context context;
    private DobbyThreadpool threadpool;
    private ApiAiClient apiAiClient;
    private ResponseCallback responseCallback;


    public interface ResponseCallback {
        void showResponse(String text);
    }

    public DobbyChatManager(Context context, DobbyThreadpool threadpool, ResponseCallback callback) {
        this.context = context;
        this.threadpool = threadpool;
        this.responseCallback = callback;
        apiAiClient = new ApiAiClient(context, threadpool);
        apiAiClient.connect();
    }

    @Override
    public void onResult(Result result) {
        responseCallback.showResponse(result.getAction());
        Log.i(TAG, "Got response Action: " + result.toString());
    }

    public void startMic() {

    }

    public void sendQuery(String text) {
        apiAiClient.sendTextQuery(text, this);
    }
}
