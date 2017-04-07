package com.inceptai.dobby.ai;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.ai.Action;
import com.inceptai.dobby.ai.ApiAiClient;
import com.inceptai.dobby.ai.InferenceEngine;
import com.inceptai.dobby.speedtest.BandwidthAnalyzer;
import com.inceptai.dobby.speedtest.BandwithTestCodes;
import com.inceptai.dobby.speedtest.NewBandwidthAnalyzer;
import com.inceptai.dobby.speedtest.ServerInformation;
import com.inceptai.dobby.speedtest.SpeedTestConfig;
import com.inceptai.dobby.speedtest.SpeedTestTask;

import java.util.List;

import ai.api.model.Result;
import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.model.SpeedTestError;

import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * This class is responsible for managing the user queries and showing the responses by working
 * with the main activity.
 * It can be made to send queries to API AI's server or use a local/another AI system.
 */

public class DobbyAi implements ApiAiClient.ResultListener, InferenceEngine.ActionListener {
    private Context context;
    private DobbyThreadpool threadpool;
    private ApiAiClient apiAiClient;
    private ResponseCallback responseCallback;
    private SpeedTestTask speedTestTask;
    private InferenceEngine inferenceEngine;


    public interface ResponseCallback {
        void showResponse(String text);
    }

    public DobbyAi(Context context, DobbyThreadpool threadpool) {
        this.context = context;
        this.threadpool = threadpool;
        apiAiClient = new ApiAiClient(context, threadpool);
        apiAiClient.connect();
        speedTestTask = new SpeedTestTask();
        inferenceEngine = new InferenceEngine(threadpool.getScheduledExecutorService(), this);
    }


    public void setResponseCallback(ResponseCallback responseCallback) {
        this.responseCallback = responseCallback;
    }

    @Override
    public void onResult(final Result result) {
        // Thread switch (to release any Api.Ai threads).
        threadpool.submit(new Runnable() {
            @Override
            public void run() {
                Action action = inferenceEngine.interpretApiAiResult(result);
                takeAction(action);
            }
        });


        Log.i(TAG, "Got response Action: " + result.toString());
        if (result.toString().contains("test")) {
            //Vivek--testing best server code.
            //WifiAnalyzer wifiAnalyzer = WifiAnalyzer.create(this.context, null);
            //PingAnalyzer pingAnalyzer = new PingAnalyzer(null);
            //PingAnalyzer.PingStats routerPingStats = pingAnalyzer.pingAndReturnStats("192.168.3.1");
            //Log.v(TAG, routerPingStats.toJson());
            //BandwidthAnalyzer bandwidthAnalyzer = BandwidthAnalyzer.create(null);
            //bandwidthAnalyzer.startBandwidthTest(BandwithTestCodes.BandwidthTestMode.DOWNLOAD_AND_UPLOAD);
            /*
            threadpool.submit(new Runnable() {
                @Override
                public void run() {
                    speedTestSocket.startDownload("2.testdebit.info", "/fichiers/1Mo.dat");
                }
            });
            speedTestTask.doInBackground();
            */
        }
    }

    public void startMic() {

    }

    /**
     * Implements the action returned by the InferenceEngine.
     *
     * @param action Action to be taken.
     */
    @Override
    public void takeAction(Action action) {
        if (responseCallback != null) {
            responseCallback.showResponse(action.getUserResponse());
        }

        if (action.getAction() == Action.ActionType.ACTION_BANDWIDTH_TEST) {
            Log.i(TAG, "Starting ACTION BANDWIDTH TEST.");
            runBandwidthTest();
        }
    }

    private void runBandwidthTest() {
        NewBandwidthAnalyzer newBandwidthAnalyzer = NewBandwidthAnalyzer.create(new BandwidthObserver(inferenceEngine));
        newBandwidthAnalyzer.startBandwidthTest(BandwithTestCodes.BandwidthTestMode.DOWNLOAD_AND_UPLOAD);
    }

    public void sendQuery(String text) {
        apiAiClient.sendTextQuery(text, this);
    }
}
