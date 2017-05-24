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
import com.inceptai.dobby.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import ai.api.model.Result;

import static com.inceptai.dobby.DobbyApplication.TAG;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_ASK_FOR_BW_TESTS;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_ASK_FOR_LONG_SUGGESTION;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_BANDWIDTH_PING_WIFI_TESTS;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_BANDWIDTH_TEST;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_CANCEL_BANDWIDTH_TEST;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_DEFAULT_FALLBACK;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_DIAGNOSE_SLOW_INTERNET;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_LIST_DOBBY_FUNCTIONS;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_NONE;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_SHOW_LONG_SUGGESTION;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_SHOW_SHORT_SUGGESTION;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_UNKNOWN;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_WELCOME;
import static com.inceptai.dobby.ai.Action.ActionType.ACTION_TYPE_WIFI_CHECK;

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
    private SuggestionCreator.Suggestion lastSuggestion;
    private @Action.ActionType int lastAction;

    @Inject
    NetworkLayer networkLayer;
    @Inject
    DobbyEventBus eventBus;

    public interface ResponseCallback {
        void showResponse(String text);
        void showRtGraph(RtDataSource<Float, Integer> rtDataSource);
        void observeBandwidth(BandwidthObserver observer);
        void cancelTests();
        void showUserActionOptions(List<Integer> userResponseTypes);
        void showBandwidthViewCard(DataInterpreter.BandwidthGrade bandwidthGrade);
        void showNetworkInfoViewCard(DataInterpreter.WifiGrade wifiGrade, String isp, String ip);
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
        lastAction = ACTION_TYPE_UNKNOWN;
    }

    @Action.ActionType
    public int getLastAction() {
        return lastAction;
    }

    public void setLastAction(int lastAction) {
        this.lastAction = lastAction;
    }

    public void setResponseCallback(ResponseCallback responseCallback) {
        this.responseCallback = responseCallback;
    }

    @Override
    public void onResult(final Action action, @Nullable final Result result) {
        // Thread switch (to release any Api.Ai threads).
        threadpool.submit(new Runnable() {
            @Override
            public void run() {
                takeAction(action);
            }
        });
        if (result != null) {
            DobbyLog.i("Got response Action: " + result.toString());
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
        DobbyLog.v("In takeAction with action: " + action.getAction() + " and user resp: " + action.getUserResponse());
        setLastAction(action.getAction());
        showMessageToUser(action.getUserResponse());
        if (responseCallback != null) {
            responseCallback.showUserActionOptions(getPotentialUserResponses(lastAction));
        }
        switch (action.getAction()) {
            case ACTION_TYPE_BANDWIDTH_TEST:
                DobbyLog.i("Starting ACTION BANDWIDTH TEST.");
                //postBandwidthTestOperation();
                postAllOperations();
                if (CLEAR_STATS_EVERY_TIME_USER_ASKS_TO_RUN_TESTS && repeatBwWifiPingAction.getAndSet(true)) {
                    //Clear the ping/wifi cache to get fresh results.
                    clearCache();
                }
                //Action diagnoseAction = inferenceEngine.addGoal(InferenceEngine.Goal.GOAL_DIAGNOSE_SLOW_INTERNET);
                //takeAction(diagnoseAction);
                return;
            case ACTION_TYPE_CANCEL_BANDWIDTH_TEST:
                DobbyLog.i("Starting ACTION CANCEL BANDWIDTH TEST.");
                try {
                    cancelBandwidthTest();
                } catch (Exception e) {
                    DobbyLog.i("Exception while cancelling:" + e);
                }
                break;
            case ACTION_TYPE_DIAGNOSE_SLOW_INTERNET:
                Action newAction = inferenceEngine.addGoal(InferenceEngine.Goal.GOAL_DIAGNOSE_SLOW_INTERNET);
                takeAction(newAction);
                return;
            case ACTION_TYPE_BANDWIDTH_PING_WIFI_TESTS:
                if (CLEAR_STATS_EVERY_TIME_USER_ASKS_TO_RUN_TESTS && repeatBwWifiPingAction.getAndSet(true)) {
                    //Clear the ping/wifi cache to get fresh results.
                    clearCache();
                }
                DobbyLog.i("Going into postAllOperations()");
                postAllOperations();
                break;
            case ACTION_TYPE_SHOW_SHORT_SUGGESTION:
                //Send event for showing short suggestion
                if (lastSuggestion != null) {
                    if (responseCallback != null) {
                        DataInterpreter.BandwidthGrade lastBandwidthGrade = lastSuggestion.suggestionCreatorParams.bandwidthGrade;
                        responseCallback.showBandwidthViewCard(lastBandwidthGrade);
                    }
                    showMessageToUser(lastSuggestion.getTitle());
                }
                sendEvent(ApiAiClient.APIAI_SHORT_SUGGESTION_SHOWN_EVENT);
                break;
            case ACTION_TYPE_SHOW_LONG_SUGGESTION:
                //Show the long suggestion here and send the event
                if (lastSuggestion != null) {
                    //TODO fix this to show list of suggestions nicely.
                    showMessageToUser(lastSuggestion.getLongSuggestionListString());
                }
                sendEvent(ApiAiClient.APIAI_LONG_SUGGESTION_SHOWN_EVENT);
                break;
            case ACTION_TYPE_NONE:
                break;
            case ACTION_TYPE_ASK_FOR_LONG_SUGGESTION:
                break;
            case ACTION_TYPE_WIFI_CHECK:
                if (responseCallback != null) {
                    responseCallback.showNetworkInfoViewCard(getCurrentWifiGrade(), getCurrentIsp(), getCurrentIp());
                }
                if (networkLayer.isWifiOnline()) {
                    sendEvent(ApiAiClient.APIAI_WIFI_ANALYSIS_SHOWN_EVENT);
                }
                break;
            case ACTION_TYPE_LIST_DOBBY_FUNCTIONS:
                break;
            case ACTION_TYPE_ASK_FOR_BW_TESTS:
                break;
            default:
                DobbyLog.i("Unknown Action");
                break;
        }
    }

    @Override
    public void suggestionsAvailable(SuggestionCreator.Suggestion suggestion) {
        DobbyLog.v("Updating last suggestion to:" + suggestion.toString());
        lastSuggestion = suggestion;
        //Create a new action
        Action shortSuggestionAction = new Action(Utils.EMPTY_STRING, ACTION_TYPE_SHOW_SHORT_SUGGESTION);
        takeAction(shortSuggestionAction);
        eventBus.postEvent(DobbyEvent.EventType.SUGGESTIONS_AVAILABLE, suggestion);
    }

    public void sendQuery(String text) {
        if (useApiAi) {
            if (networkLayer.isWifiOnline()) {
                apiAiClient.sendTextQuery(text, null, this);
            } else {
                apiAiClient.processTextQueryOffline(text, null, getLastAction(), this);
            }
        } else {
            DobbyLog.w("Ignoring text query for Wifi doc version :" + text);
        }
    }

    public void sendEvent(String text) {
        if (useApiAi) {
            if (networkLayer.isWifiOnline()) {
                apiAiClient.sendTextQuery(null, text, this);
            } else {
                apiAiClient.processTextQueryOffline(null, text, getLastAction(), this);
            }
        } else {
            DobbyLog.w("Ignoring events for Wifi doc version :" + text);
        }
    }


    public void cleanup() {
        networkLayer.cleanup();
        inferenceEngine.cleanup();
        if (useApiAi) {
            apiAiClient.cleanup();
        }
    }


    private List<Integer> getPotentialUserResponses(@Action.ActionType int lastActionShownToUser) {
        ArrayList<Integer> responseList = new ArrayList<Integer>();
        switch (lastActionShownToUser) {
            case ACTION_TYPE_ASK_FOR_LONG_SUGGESTION:
                responseList.add(UserResponse.ResponseType.YES);
                responseList.add(UserResponse.ResponseType.NO);
                break;
            case ACTION_TYPE_BANDWIDTH_TEST:
                responseList.add(UserResponse.ResponseType.CANCEL);
                break;
            case ACTION_TYPE_WIFI_CHECK:
                //responseList.add(UserResponse.ResponseType.YES);
                //responseList.add(UserResponse.ResponseType.NO);
                responseList.add(UserResponse.ResponseType.RUN_BW_TESTS);
                //responseList.add(UserResponse.ResponseType.LIST_ALL_FUNCTIONS);
                responseList.add(UserResponse.ResponseType.ASK_ABOUT_DOBBY);
                break;
            case ACTION_TYPE_DIAGNOSE_SLOW_INTERNET:
            case ACTION_TYPE_BANDWIDTH_PING_WIFI_TESTS:
                responseList.add(UserResponse.ResponseType.CANCEL);
                break;
            case ACTION_TYPE_SHOW_SHORT_SUGGESTION:
                break;
            case ACTION_TYPE_WELCOME:
            case ACTION_TYPE_NONE:
            case ACTION_TYPE_SHOW_LONG_SUGGESTION:
            case ACTION_TYPE_UNKNOWN:
            case ACTION_TYPE_DEFAULT_FALLBACK:
            case ACTION_TYPE_CANCEL_BANDWIDTH_TEST:
            default:
                responseList.add(UserResponse.ResponseType.RUN_ALL_DIAGNOSTICS);
                responseList.add(UserResponse.ResponseType.RUN_BW_TESTS);
                responseList.add(UserResponse.ResponseType.RUN_WIFI_TESTS);
                //responseList.add(UserResponse.ResponseType.LIST_ALL_FUNCTIONS);
                responseList.add(UserResponse.ResponseType.ASK_ABOUT_DOBBY);
                break;
            case ACTION_TYPE_LIST_DOBBY_FUNCTIONS:
                responseList.add(UserResponse.ResponseType.RUN_ALL_DIAGNOSTICS);
                responseList.add(UserResponse.ResponseType.RUN_BW_TESTS);
                responseList.add(UserResponse.ResponseType.RUN_WIFI_TESTS);
                responseList.add(UserResponse.ResponseType.ASK_ABOUT_DOBBY);
                break;
            case ACTION_TYPE_ASK_FOR_BW_TESTS:
                responseList.add(UserResponse.ResponseType.YES);
                responseList.add(UserResponse.ResponseType.NO);
                responseList.add(UserResponse.ResponseType.RUN_BW_TESTS);
                break;
        }
        return responseList;
    }

    private void showMessageToUser(String messageToShow) {
        if (responseCallback != null && messageToShow != null && ! messageToShow.equals(Utils.EMPTY_STRING)) {
            DobbyLog.v("Showing to user message: " + messageToShow);
            responseCallback.showResponse(messageToShow);
        }
    }

    private void clearCache() {
        if (networkLayer != null) {
            networkLayer.clearStatsCache();
        }
        if (inferenceEngine != null) {
            inferenceEngine.clearConditionsAndMetrics();
        }
        lastSuggestion = null;
    }

    private void postBandwidthTestOperation() {
        ComposableOperation operation = bandwidthOperation();
        operation.post();
    }

    private DataInterpreter.WifiGrade getCurrentWifiGrade() {
        return networkLayer.getCurrentWifiGrade();
    }

    private String getCurrentIsp() {
        return networkLayer.getCachedClientIspIfAvailable();
    }

    private String getCurrentIp() {
        return networkLayer.getCachedExternalIpIfAvailable();
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
        if (responseCallback != null) {
            // responseCallback.showRtGraph(observer);
            responseCallback.observeBandwidth(observer);
        }
        return observer.asFuture();
    }

    private void cancelBandwidthTest() throws Exception {
        networkLayer.cancelBandwidthTests();
        lastSuggestion = null;
        if (responseCallback != null) {
            responseCallback.cancelTests();
        }
    }

    private void initApiAiClient() {
        apiAiClient = new ApiAiClient(context, threadpool);
        apiAiClient.connect();
    }
}
