package com.inceptai.dobby.ai;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.NetworkLayer;
import com.inceptai.dobby.database.InferenceDatabaseWriter;
import com.inceptai.dobby.eventbus.DobbyEvent;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.model.PingStats;
import com.inceptai.dobby.speedtest.BandwidthObserver;
import com.inceptai.dobby.speedtest.BandwidthResult;
import com.inceptai.dobby.speedtest.BandwithTestCodes;
import com.inceptai.dobby.utils.DobbyLog;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import ai.api.model.Result;

import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * This class is responsible for managing the user queries and showing the responses by working
 * with the main activity.
 * It can be made to send queries to API AI's server or use a local/another AI system.
 */

public class DobbyAi implements ApiAiClient.ResultListener, InferenceEngine.ActionListener {
    private static final boolean CLEAR_STATS_EVERY_TIME_USER_ASKS_TO_RUN_TESTS = true;
    private Context context;
    private DobbyThreadpool threadpool;
    private ApiAiClient apiAiClient;

    @Nullable
    private ResponseCallback responseCallback;
    private InferenceEngine inferenceEngine;
    private boolean useApiAi = false; // We do not use ApiAi for the WifiDoc app.
    private AtomicBoolean repeatBwWifiPingAction;

    @Inject
    NetworkLayer networkLayer;
    @Inject
    DobbyEventBus eventBus;

    public interface ResponseCallback {
        void showResponse(String text);
        void showRtGraph(RtDataSource<Float, Integer> rtDataSource);
    }

    public DobbyAi(DobbyThreadpool threadpool,
                   InferenceDatabaseWriter inferenceDatabaseWriter,
                   DobbyApplication dobbyApplication) {
        this.context = dobbyApplication.getApplicationContext();
        this.threadpool = threadpool;
        useApiAi = DobbyApplication.isDobbyFlavor();
        inferenceEngine = new InferenceEngine(threadpool.getScheduledExecutorService(),
                this, inferenceDatabaseWriter, dobbyApplication);
        if (useApiAi) {
            initApiAiClient();
        }
        repeatBwWifiPingAction = new AtomicBoolean(false);
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


        DobbyLog.i("Got response Action: " + result.toString());
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
            DobbyLog.i("Starting ACTION BANDWIDTH TEST.");
            postBandwidthTestOperation();
        }

        if (action.getAction() == Action.ActionType.ACTION_TYPE_CANCEL_BANDWIDTH_TEST) {
            DobbyLog.i("Starting ACTION CANCEL BANDWIDTH TEST.");
            try {
                cancelBandwidthTest();
            } catch (Exception e) {
                DobbyLog.i("Exception while cancelling:" + e);
            }
        }
        if (action.getAction() == Action.ActionType.ACTION_TYPE_DIAGNOSE_SLOW_INTERNET) {
            Action newAction = inferenceEngine.addGoal(InferenceEngine.Goal.GOAL_DIAGNOSE_SLOW_INTERNET);
            takeAction(newAction);
            return;
        }
        if (action.getAction() == Action.ActionType.ACTION_TYPE_BANDWIDTH_PING_WIFI_TESTS) {
            if (CLEAR_STATS_EVERY_TIME_USER_ASKS_TO_RUN_TESTS && repeatBwWifiPingAction.getAndSet(true)) {
                //Clear the ping/wifi cache to get fresh results.
                clearCache();
            }
            DobbyLog.i("Going into postAllOperations()");
            postAllOperations();
        }
    }

    @Override
    public void suggestionsAvailable(SuggestionCreator.Suggestion suggestion) {
        eventBus.postEvent(DobbyEvent.EventType.SUGGESTIONS_AVAILABLE, suggestion);
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

    private void clearCache() {
        if (networkLayer != null) {
            networkLayer.clearStatsCache();
        }
        if (inferenceEngine != null) {
            inferenceEngine.clearConditionsAndMetrics();
        }
    }

    private void postBandwidthTestOperation() {
        ComposableOperation operation = bandwidthOperation();
        operation.post();
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
                    DataInterpreter.WifiGrade wifiGrade = inferenceEngine.notifyWifiState(networkLayer.getWifiState(),
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
        @BandwithTestCodes.TestMode
        int testMode = BandwithTestCodes.TestMode.DOWNLOAD_AND_UPLOAD;
        BandwidthObserver observer = networkLayer.startBandwidthTest(testMode);
        if (observer == null) {
            Log.w(TAG, "Null observer returned from NL, abandoning bandwidth test.");
            //Notify inference engine of negative bandwidth numbers or that we don't have valid answers for it.
            threadpool.submit(new Runnable() {
                @Override
                public void run() {
                    inferenceEngine.notifyBandwidthTestError(BandwithTestCodes.ErrorCodes.ERROR_WIFI_OFFLINE, -1.0);
                }
            });
            return null;
        }
        observer.setInferenceEngine(inferenceEngine);
        //responseCallback.showRtGraph(observer);
        if (responseCallback != null) {
            responseCallback.showRtGraph(observer);
        }
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
