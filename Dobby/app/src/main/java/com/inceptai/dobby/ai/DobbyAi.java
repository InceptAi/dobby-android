package com.inceptai.dobby.ai;

import android.content.Context;
import android.util.Log;

import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.NetworkLayer;
import com.inceptai.dobby.speedtest.SpeedTestTask;

import javax.inject.Inject;

import ai.api.model.Result;

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

    @Inject
    NetworkLayer networkLayer;


    public interface ResponseCallback {
        void showResponse(String text);
        void showRtGraph(RtDataSource<Float> rtDataSource);
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
    public void onResult(final Action action, final Result result) {
        // Thread switch (to release any Api.Ai threads).
        threadpool.submit(new Runnable() {
            @Override
            public void run() {
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

        if (action.getAction() == Action.ActionType.ACTION_TYPE_BANDWIDTH_TEST) {
            Log.i(TAG, "Starting ACTION BANDWIDTH TEST.");
            postBandwidthTestOperation();
        }

        if (action.getAction() == Action.ActionType.ACTION_TYPE_CANCEL_BANDWIDTH_TEST) {
            Log.i(TAG, "Starting ACTION CANCEL BANDWIDTH TEST.");
            try {
                cancelBandwidthTest();
            } catch (Exception e) {
                Log.i(TAG, "Exception while cancelling:" + e);
            }
        }
        if (action.getAction() == Action.ActionType.ACTION_TYPE_DIAGNOSE_SLOW_INTERNET) {
            Action newAction = inferenceEngine.addGoal(InferenceEngine.Goal.GOAL_DIAGNOSE_SLOW_INTERNET);
            takeAction(newAction);
            return;
        }
        if (action.getAction() == Action.ActionType.ACTION_TYPE_BANDWIDTH_PING_WIFI_TESTS) {
            // Run b/w, wifi and ping tests.
        }
    }

    public void sendQuery(String text) {
        apiAiClient.sendTextQuery(text, this);
    }

    public void cleanup() {
        networkLayer.cleanup();
        inferenceEngine.cleanup();
        apiAiClient.cleanup();
    }

    private void postBandwidthTestOperation() {
        BandwidthOperation operation = new BandwidthOperation(threadpool, networkLayer,
                inferenceEngine, responseCallback);
        operation.post();
//
//        Log.i(TAG, "Going to start bandwidth test.");
//        @BandwithTestCodes.BandwidthTestMode
//        int testMode = BandwithTestCodes.BandwidthTestMode.DOWNLOAD_AND_UPLOAD;
//        BandwidthObserver observer = networkLayer.startBandwidthTest(testMode);
//        observer.setInferenceEngine(inferenceEngine);
//        responseCallback.showRtGraph(observer);
    }

    private ComposableOperation wifiScanOperation() {
        return null;
    }

    private void runBandwidthTest() {
        Log.i(TAG, "Going to start bandwidth test.");
        @BandwithTestCodes.BandwidthTestMode
        int testMode = BandwithTestCodes.BandwidthTestMode.DOWNLOAD_AND_UPLOAD;
        BandwidthObserver observer = networkLayer.startBandwidthTest(testMode);
        if (observer.bandwidthTestState == BandwithTestCodes.BandwidthTestExceptionErrorCodes.TEST_STARTED_NO_EXCEPTION) {
            Log.v(TAG, "Started bw test successfully");
        } else if (observer.bandwidthTestState == BandwithTestCodes.BandwidthTestExceptionErrorCodes.TEST_ALREADY_RUNNING) {
            Log.v(TAG, "BW test already running.");
        } else {
            Log.v(TAG, "Error while starting bandwidth tests: " + observer.bandwidthTestState);
        }
        observer.setInferenceEngine(inferenceEngine);
        responseCallback.showRtGraph(observer);
    }

    private void cancelBandwidthTest() throws Exception {
        networkLayer.cancelBandwidthTests();
    }
}
