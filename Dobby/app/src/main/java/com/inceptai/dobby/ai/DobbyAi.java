package com.inceptai.dobby.ai;

import android.content.Context;
import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.NetworkLayer;
import com.inceptai.dobby.eventbus.DobbyEvent;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.model.PingStats;
import com.inceptai.dobby.speedtest.BandwidthObserver;
import com.inceptai.dobby.speedtest.BandwidthResult;
import com.inceptai.dobby.speedtest.BandwithTestCodes;
import com.inceptai.dobby.utils.DobbyLog;

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
    private boolean useApiAi = false; // We do not use ApiAi for the WifiDoc app.

    @Inject
    NetworkLayer networkLayer;
    @Inject
    DobbyEventBus eventBus;


    public interface ResponseCallback {
        void showResponse(String text);
        void showRtGraph(RtDataSource<Float, Integer> rtDataSource);
    }

    public DobbyAi(Context context, DobbyThreadpool threadpool) {
        this.context = context;
        this.threadpool = threadpool;
        useApiAi = DobbyApplication.isDobbyFlavor();
        inferenceEngine = new InferenceEngine(threadpool.getScheduledExecutorService(), this);
        if (useApiAi) {
            initApiAiClient();
        }
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
        if (useApiAi) {
            apiAiClient.sendTextQuery(text, this);
        } else {
            DobbyLog.w("Ignoring text query for Wifi doc version :" + text);
        }
    }

    public void cleanup() {
        networkLayer.cleanup();
        inferenceEngine.cleanup();
        if (useApiAi) {
            apiAiClient.cleanup();
        }
    }

    private void postBandwidthTestOperation() {
        ComposableOperation operation = bandwidthOperation();
        operation.post();
    }

    private void postAllOperations() {
        ComposableOperation bwTest = bandwidthOperation();
        bwTest.post();
        final ComposableOperation wifiScan = wifiScanOperation();
        bwTest.uponCompletion(wifiScan);
        final ComposableOperation ping = pingOperation();
        wifiScan.uponCompletion(ping);

        // Wire up with IE.
        wifiScan.getFuture().addListener(new Runnable() {
            @Override
            public void run() {
                // Handle failed wifi scan using OperationResult.
                DataInterpreter.WifiGrade wifiGrade = inferenceEngine.notifyWifiState(networkLayer.getWifiState(),
                        networkLayer.getWifiLinkMode(),
                        networkLayer.getCurrentConnectivityMode());
                eventBus.postEvent(DobbyEvent.EventType.WIFI_SCAN_AVAILABLE, wifiGrade);
            }
        }, threadpool.getExecutor());

        ping.getFuture().addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    OperationResult result = ping.getFuture().get();
                    HashMap<String, PingStats> payload = (HashMap<String, PingStats>) result.getPayload();
                    if (payload != null) {
                        DataInterpreter.PingGrade pingGrade = inferenceEngine.notifyPingStats(payload, networkLayer.getIpLayerInfo());
                        if (pingGrade != null) {
                            eventBus.postEvent(DobbyEvent.EventType.PING_INFO_AVAILABLE, pingGrade);
                        }
                    } else {
                        // Error.
                        Log.i(TAG, "Error starting ping.");
                    }
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                    Log.w(TAG, "Exception getting ping results: " + e.getStackTrace().toString());
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

    private ComposableOperation pingOperation() {
        return new ComposableOperation(threadpool) {
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

    private ComposableOperation bandwidthOperation() {
        return new ComposableOperation(threadpool) {
            @Override
            public void post() {
                setFuture(startBandwidthTest());
            }

            @Override
            protected String getName() {
                return "Bandwidth test operation";
            }
        };
    }

    private ListenableFuture<BandwidthResult> startBandwidthTest() {
        Log.i(TAG, "Going to start bandwidth test.");
        @BandwithTestCodes.TestMode
        int testMode = BandwithTestCodes.TestMode.DOWNLOAD_AND_UPLOAD;
        BandwidthObserver observer = networkLayer.startBandwidthTest(testMode);
        if (observer == null) {
            return null;
        }
        observer.setInferenceEngine(inferenceEngine);
        responseCallback.showRtGraph(observer);
        return observer.asFuture();
    }

    private void cancelBandwidthTest() throws Exception {
        networkLayer.cancelBandwidthTests();
    }

    private void initApiAiClient() {
        apiAiClient = new ApiAiClient(context, threadpool);
        apiAiClient.connect();
    }
}
