package com.inceptai.dobby.ai;

import android.support.annotation.IntDef;

import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.connectivity.ConnectivityAnalyzer;
import com.inceptai.dobby.database.FailureDatabaseWriter;
import com.inceptai.dobby.database.FailureRecord;
import com.inceptai.dobby.database.InferenceDatabaseWriter;
import com.inceptai.dobby.database.InferenceRecord;
import com.inceptai.dobby.model.DobbyWifiInfo;
import com.inceptai.dobby.model.IPLayerInfo;
import com.inceptai.dobby.model.PingStats;
import com.inceptai.dobby.speedtest.BandwithTestCodes;
import com.inceptai.dobby.utils.DobbyLog;
import com.inceptai.dobby.utils.Utils;
import com.inceptai.dobby.wifi.WifiState;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;


/**
 * Inference engine consumes actions from the NLU engine (ApiAi or local) and creates Actions.
 * It also consumes network metrics as a result of Actions such as bandwidth tests, etc, to further
 * decide the course of action.
 */

public class InferenceEngine {
    private static final String CANNED_RESPONSE = "We are working on it.";

    private static final int MAX_SUGGESTIONS_TO_SHOW = 2;
    private static final double MAX_GAP_IN_SUGGESTION_WEIGHT = 0.2;
    private static final boolean LONG_SUGGESTION_MODE = true;

    private static final int STATE_BANDWIDTH_TEST_NONE = 0;
    private static final int STATE_BANDWIDTH_TEST_REQUESTED = 1;
    private static final int STATE_BANDWIDTH_TEST_RUNNING = 2;
    private static final int STATE_BANDWIDTH_TEST_SUCCESS = 3;
    private static final int STATE_BANDWIDTH_TEST_FAILED = 4;
    private static final int STATE_BANDWIDTH_TEST_CANCELLED = 4;

    @IntDef({Mode.INFERENCE_MODE_HOME_WIFI, Mode.INFERENCE_MODE_PUBLIC_WIFI, Mode.INFERENCE_MODE_TV, Mode.INFERENCE_MODE_UNKNOWN})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Mode {
        int INFERENCE_MODE_TV = 1001;
        int INFERENCE_MODE_HOME_WIFI = 1002;
        int INFERENCE_MODE_PUBLIC_WIFI = 1003;
        int INFERENCE_MODE_UNKNOWN = 1004;
    }

    private Action previousAction = Action.ACTION_NONE;
    private int bandwidthTestState; /* state of the bandwidth test */
    private ScheduledExecutorService scheduledExecutorService;
    private ScheduledFuture<?> bandwidthCheckFuture;
    private ActionListener actionListener;
    private long lastBandwidthUpdateTimestampMs = 0;
    private MetricsDb metricsDb;
    private PossibleConditions currentConditions = PossibleConditions.NOOP_CONDITION;
    private InferenceDatabaseWriter inferenceDatabaseWriter;
    private FailureDatabaseWriter failureDatabaseWriter;
    private DobbyApplication dobbyApplication;

    @Mode
    private int currentMode = Mode.INFERENCE_MODE_UNKNOWN;

    public interface ActionListener {
        void takeAction(Action action);
        void suggestionsAvailable(SuggestionCreator.Suggestion suggestion);
    }

    @IntDef({
            Goal.GOAL_DIAGNOSE_SLOW_INTERNET,
            Goal.GOAL_DIAGNOSE_WIFI_ISSUES,
            Goal.GOAL_DIAGNOSE_JITTERY_STREAMING})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Goal {
        int GOAL_DIAGNOSE_SLOW_INTERNET = 1;
        int GOAL_DIAGNOSE_WIFI_ISSUES = 2;
        int GOAL_DIAGNOSE_JITTERY_STREAMING = 3;
    }

    InferenceEngine(ScheduledExecutorService scheduledExecutorService,
                    ActionListener actionListener,
                    InferenceDatabaseWriter inferenceDatabaseWriter,
                    FailureDatabaseWriter failureDatabaseWriter,
                    DobbyApplication dobbyApplication) {
        bandwidthTestState = STATE_BANDWIDTH_TEST_NONE;
        this.scheduledExecutorService = scheduledExecutorService;
        this.actionListener = actionListener;
        metricsDb = new MetricsDb();
        this.inferenceDatabaseWriter = inferenceDatabaseWriter;
        this.failureDatabaseWriter = failureDatabaseWriter;
        this.dobbyApplication = dobbyApplication;
    }

    public void setCurrentMode(@Mode int mode) {
        currentMode = mode;
    }

    public Action addGoal(@Goal int goal) {
        return new Action(Utils.EMPTY_STRING, Action.ActionType.ACTION_TYPE_BANDWIDTH_PING_WIFI_TESTS);
    }

    public void clearConditionsAndMetrics() {
        metricsDb.clearAllGrades();
        currentConditions.clearConditions();
    }


    private String testModeToString(@BandwithTestCodes.TestMode int testMode) {
        String testModeString = "UNKNOWN";
        if (testMode == BandwithTestCodes.TestMode.DOWNLOAD) {
            testModeString = "DOWNLOAD";
        } else if (testMode == BandwithTestCodes.TestMode.UPLOAD) {
            testModeString = "UPLOAD";
        }
        return testModeString;
    }

    public DataInterpreter.WifiGrade notifyWifiState(WifiState wifiState, @WifiState.WifiLinkMode int wifiLinkMode,
                                                     @ConnectivityAnalyzer.WifiConnectivityMode int wifiConnectivityMode) {
        DataInterpreter.WifiGrade wifiGrade = new DataInterpreter.WifiGrade();
        if (wifiState != null) {
            HashMap<Integer, WifiState.ChannelInfo> channelMap = wifiState.getChannelInfoMap();
            DobbyWifiInfo wifiInfo = wifiState.getLinkInfo();
            wifiGrade = DataInterpreter.interpret(channelMap, wifiInfo, wifiLinkMode, wifiConnectivityMode);
        }
        metricsDb.updateWifiGrade(wifiGrade);
        PossibleConditions conditions = InferenceMap.getPossibleConditionsFor(wifiGrade);
        currentConditions.mergeIn(conditions);
        DobbyLog.i("InferenceEngine Wifi Grade: " + wifiGrade.toString());
        DobbyLog.i("InferenceEngine which gives conditions: " + conditions.toString());
        DobbyLog.i("InferenceEngine After merging: " + currentConditions.toString());
        checkAndSendSuggestions();
        return wifiGrade;
    }

    public DataInterpreter.PingGrade notifyPingStats(HashMap<String, PingStats> pingStatsMap, IPLayerInfo ipLayerInfo) {
        DataInterpreter.PingGrade pingGrade = new DataInterpreter.PingGrade();
        if (pingStatsMap != null && ipLayerInfo != null) {
            pingGrade = DataInterpreter.interpret(pingStatsMap, ipLayerInfo);
        }
        metricsDb.updatePingGrade(pingGrade);
        PossibleConditions conditions = InferenceMap.getPossibleConditionsFor(pingGrade);
        currentConditions.mergeIn(conditions);
        DobbyLog.i("InferenceEngine Ping Grade: " + pingGrade.toString());
        DobbyLog.i("InferenceEngine which gives conditions: " + conditions.toString());
        DobbyLog.i("InferenceEngine After merging: " + currentConditions.toString());
        checkAndSendSuggestions();
        return pingGrade;
    }

    public DataInterpreter.HttpGrade notifyGatewayHttpStats(PingStats gatewayHttpStats) {
        DataInterpreter.HttpGrade httpGrade = new DataInterpreter.HttpGrade();
        if (gatewayHttpStats != null) {
            httpGrade = DataInterpreter.interpret(gatewayHttpStats);
        }
        metricsDb.updateHttpGrade(httpGrade);
        PossibleConditions conditions = InferenceMap.getPossibleConditionsFor(httpGrade);
        currentConditions.mergeIn(conditions);
        DobbyLog.i("InferenceEngine httpGradeJson: " + httpGrade.toString());
        DobbyLog.i("InferenceEngine which gives conditions: " + conditions.toString());
        DobbyLog.i("InferenceEngine After merging: " + currentConditions.toString());
        checkAndSendSuggestions();
        return httpGrade;
    }


    public SuggestionCreator.Suggestion suggest() {
        HashMap<Integer, Double> conditionMapToUse = currentConditions.getTopConditionsMap(
                MAX_SUGGESTIONS_TO_SHOW, MAX_GAP_IN_SUGGESTION_WEIGHT);
        return SuggestionCreator.get(conditionMapToUse, metricsDb.getParamsForSuggestions());
    }

    private void checkAndSendSuggestions() {
        if (metricsDb.hasValidUpload() && metricsDb.hasValidDownload() &&
                metricsDb.hasFreshPingGrade() &&
                metricsDb.hasFreshWifiGrade() &&
                metricsDb.hasFreshHttpGrade()) {
            SuggestionCreator.Suggestion suggestion = suggest();

            if (actionListener != null) {
                DobbyLog.i("Sending suggestions to DobbyAi");
                actionListener.suggestionsAvailable(suggestion);
            }
            //sendResponseOnlyAction(suggestion.getTitle(), Action.ActionType.ACTION_TYPE_SHOW_SHORT_SUGGESTION);
            //Write the suggestion and inferencing parameters to DB
            InferenceRecord newInferenceRecord = createInferenceRecord(suggestion);
            inferenceDatabaseWriter.writeInferenceToDatabase(newInferenceRecord);

        }
    }


    private InferenceRecord createInferenceRecord(SuggestionCreator.Suggestion suggestion) {
        InferenceRecord inferenceRecord = new InferenceRecord();
        HashMap<String, String> phoneInfo = new HashMap<>();
        inferenceRecord.uid = dobbyApplication.getUserUuid();
        inferenceRecord.phoneInfo = dobbyApplication.getPhoneInfo();
        inferenceRecord.appVersion = dobbyApplication.getAppVersion();

        //Assign all the grades
        inferenceRecord.bandwidthGradeJson = suggestion.suggestionCreatorParams.bandwidthGrade.toJson();
        inferenceRecord.wifiGradeJson = suggestion.suggestionCreatorParams.wifiGrade.toJson();
        inferenceRecord.pingGradeJson = suggestion.suggestionCreatorParams.pingGrade.toJson();
        inferenceRecord.httpGradeJson = suggestion.suggestionCreatorParams.httpGrade.toJson();

        //Assign the possible conditions
        inferenceRecord.conditionsUsedForInference = Utils.convertIntegerDoubleHashMapToJsonString(suggestion.conditionsMap);

        //Assign the title and detailed message
        inferenceRecord.titleMessage = suggestion.title;
        inferenceRecord.detailedMessageList = suggestion.longSuggestionList;

        //Assign the timestamp
        inferenceRecord.timestamp = System.currentTimeMillis();
        return inferenceRecord;
    }

    private FailureRecord createFailureRecord(@BandwithTestCodes.TestMode int testMode,
                                                @BandwithTestCodes.ErrorCodes int errorCode,
                                                String errorMessage) {
        FailureRecord failureRecord = new FailureRecord();
        failureRecord.uid = dobbyApplication.getUserUuid();
        failureRecord.phoneInfo = dobbyApplication.getPhoneInfo();
        failureRecord.appVersion = dobbyApplication.getAppVersion();
        failureRecord.errorCode = errorCode;
        failureRecord.testMode = testMode;
        failureRecord.errorMessage = errorMessage;
        //Assign the timestamp
        failureRecord.timestamp = System.currentTimeMillis();
        return failureRecord;
    }


    // Bandwidth test notifications:
    public void notifyBandwidthTestStart(@BandwithTestCodes.TestMode int testMode) {
        if (testMode == BandwithTestCodes.TestMode.UPLOAD) {
            metricsDb.clearUploadBandwidthGrade();
        } else if (testMode == BandwithTestCodes.TestMode.DOWNLOAD) {
            metricsDb.clearDownloadBandwidthGrade();
        }
    }

    public void notifyBandwidthTestProgress(@BandwithTestCodes.TestMode int testMode, double bandwidth) {
        long currentTs = System.currentTimeMillis();
        if ((currentTs - lastBandwidthUpdateTimestampMs) > 500L) {
            // sendResponseOnlyAction(testModeToString(testMode) + " Current Bandwidth: " + String.format("%.2f", bandwidth / 1000000) + " Mbps");
            lastBandwidthUpdateTimestampMs = currentTs;
        }
    }

    public DataInterpreter.BandwidthGrade notifyBandwidthTestResult(@BandwithTestCodes.TestMode int testMode,
                                                                    double bandwidth,
                                                                    String clientIsp,
                                                                    String clientExternalIp) {
        DataInterpreter.BandwidthGrade bandwidthGrade = new DataInterpreter.BandwidthGrade();
        /*
        if (bandwidth >= 0) {
            //sendResponseOnlyAction(testModeToString(testMode) + " Overall Bandwidth = " + String.format("%.2f", bandwidth / 1000000) + " Mbps");
        } else {
            //sendResponseOnlyAction(testModeToString(testMode) + " Bandwidth error -- can't do bandwidth test.");
        }
        */
        lastBandwidthUpdateTimestampMs = 0;

        if (testMode == BandwithTestCodes.TestMode.UPLOAD) {
            metricsDb.updateUploadBandwidthGrade(bandwidth * 1.0e-6, DataInterpreter.MetricType.UNKNOWN);
        } else if (testMode == BandwithTestCodes.TestMode.DOWNLOAD) {
            metricsDb.updateDownloadBandwidthGrade(bandwidth * 1.0e-6, DataInterpreter.MetricType.UNKNOWN);
        }

        if (metricsDb.hasValidDownload() && metricsDb.hasValidUpload()) {
            bandwidthGrade = DataInterpreter.interpret(metricsDb.getDownloadMbps(),
                    metricsDb.getUploadMbps(), clientIsp, clientExternalIp, BandwithTestCodes.ErrorCodes.NO_ERROR);
            //Update the bandwidth grade, overwriting earlier info.
            metricsDb.updateBandwidthGrade(bandwidthGrade);
            PossibleConditions conditions = InferenceMap.getPossibleConditionsFor(bandwidthGrade);
            currentConditions.mergeIn(conditions);
            DobbyLog.i("InferenceEngine bandwidthGradeJson: " + bandwidthGrade.toString());
            DobbyLog.i("InferenceEngine which gives conditions: " + conditions.toString());
            DobbyLog.i("InferenceEngine After merging: " + currentConditions.toString());
            checkAndSendSuggestions();
        }
        return bandwidthGrade;
    }


    public DataInterpreter.BandwidthGrade notifyBandwidthTestError(@BandwithTestCodes.TestMode int testMode,
                                                                   @BandwithTestCodes.ErrorCodes int errorCode,
                                                                   String errorMessage,
                                                                   double bandwidth) {
        lastBandwidthUpdateTimestampMs = 0;
        DataInterpreter.BandwidthGrade bandwidthGrade = DataInterpreter.interpret(bandwidth, bandwidth,
                Utils.EMPTY_STRING, Utils.EMPTY_STRING, errorCode);
        metricsDb.updateBandwidthGrade(bandwidthGrade);
        PossibleConditions conditions = InferenceMap.getPossibleConditionsFor(bandwidthGrade);
        currentConditions.mergeIn(conditions);
        DobbyLog.i("InferenceEngine bandwidthGradeJson: " + bandwidthGrade.toString());
        DobbyLog.i("InferenceEngine which gives conditions: " + conditions.toString());
        DobbyLog.i("InferenceEngine After merging: " + currentConditions.toString());
        checkAndSendSuggestions();
        //Write failure to database
        writeFailureToDatabase(testMode, errorCode, errorMessage);
        return bandwidthGrade;
    }


    public void cleanup() {
        if (bandwidthCheckFuture!= null && !bandwidthCheckFuture.isDone()) {
            bandwidthCheckFuture.cancel(true);
            bandwidthCheckFuture = null;
        }
        previousAction = Action.ACTION_NONE;
    }

    private void writeFailureToDatabase(@BandwithTestCodes.TestMode int testMode,
                                        @BandwithTestCodes.ErrorCodes int errorCode,
                                        String errorMessage) {
        //Write failure to database
        FailureRecord newFailureRecord = createFailureRecord(testMode, errorCode, errorMessage);
        failureDatabaseWriter.writeFailureToDatabase(newFailureRecord);
    }

    private void updateBandwidthState(int toState) {
        if (bandwidthCheckFuture != null) {
            bandwidthCheckFuture.cancel(true);
        }
        bandwidthCheckFuture = null;
        if (toState == STATE_BANDWIDTH_TEST_REQUESTED ||
                toState == STATE_BANDWIDTH_TEST_RUNNING) {
            bandwidthCheckFuture = scheduledExecutorService.schedule(new Runnable() {
                @Override
                public void run() {
                    bandwidthTestStateCheck();
                }
            }, 100, TimeUnit.MILLISECONDS);
        }
    }

    private void bandwidthTestStateCheck() {
        // Timeouts etc.
    }

    private void sendResponseOnlyAction(String response, @Action.ActionType int action) {
        if (actionListener == null) {
            DobbyLog.w("Attempting to send action to non-existent listener");
            return;
        }
        if (response == null || response.isEmpty()) {
            response = CANNED_RESPONSE;
        }
        actionListener.takeAction(new Action(response, action));
    }
}
