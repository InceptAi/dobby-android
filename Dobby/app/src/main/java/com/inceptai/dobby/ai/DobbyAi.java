package com.inceptai.dobby.ai;

import android.content.Context;
import android.util.Log;

import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.NetworkLayer;
import com.inceptai.dobby.model.PingStats;

import java.util.HashMap;

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
            postAllOperations();
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
    }

    private void postAllOperations() {
        BandwidthOperation bwTest = new BandwidthOperation(threadpool, networkLayer,
                inferenceEngine, responseCallback);
        bwTest.post();
        ComposableOperation wifiScan = wifiScanOperation();
        bwTest.uponCompletion(wifiScan);
        final ComposableOperation<HashMap<String, PingStats>> ping = pingOperation();
        wifiScan.uponCompletion(ping);

        // Wire up with IE.
        wifiScan.getFuture().addListener(new Runnable() {
            @Override
            public void run() {
                inferenceEngine.notifyWifiState(networkLayer.getWifiState(),
                        networkLayer.getWifiLinkMode(),
                        networkLayer.getCurrentConnectivityMode());
            }
        }, threadpool.getExecutor());

        ping.getFuture().addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    inferenceEngine.notifyPingStats(ping.getFuture().get(), networkLayer.getIpLayerInfo());
                } catch (Exception e) {
                    Log.w(TAG, "Exception getting ping results");
                }
            }
        }, threadpool.getExecutor());
    }

    private ComposableOperation wifiScanOperation() {
        return new ComposableOperation(threadpool) {
            @Override
            public void post() {
                setFuture(networkLayer.wifiScan());
            }

            @Override
            protected String getName() {
                return "Wifi Scan operation";
            }
        };
    }

    private ComposableOperation<HashMap<String, PingStats>> pingOperation() {
        return new ComposableOperation<HashMap<String, PingStats>>(threadpool) {
            @Override
            public void post() {
                setFuture(networkLayer.startPing());
            }

            @Override
            protected String getName() {
                return "Ping Operation";
            }
        };
    }

    private void cancelBandwidthTest() throws Exception {
        networkLayer.cancelBandwidthTests();
    }
}
