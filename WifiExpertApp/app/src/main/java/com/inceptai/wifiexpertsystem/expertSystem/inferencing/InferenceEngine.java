package com.inceptai.wifiexpertsystem.expertSystem.inferencing;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.support.annotation.IntDef;

import com.inceptai.wifiexpertsystem.DobbyApplication;
import com.inceptai.wifiexpertsystem.database.writer.FailureDatabaseWriter;
import com.inceptai.wifiexpertsystem.database.model.FailureRecord;
import com.inceptai.wifiexpertsystem.database.writer.InferenceDatabaseWriter;
import com.inceptai.wifiexpertsystem.database.model.InferenceRecord;
import com.inceptai.wifiexpertsystem.utils.DobbyLog;
import com.inceptai.wifiexpertsystem.utils.Utils;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ConnectivityTester;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ping.PingStats;
import com.inceptai.wifimonitoringservice.actionlibrary.utils.ActionLibraryCodes;
import com.inceptai.wifimonitoringservice.utils.WifiStateData;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

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

    public void clearConditionsAndMetrics() {
        metricsDb.clearAllGrades();
        currentConditions.clearConditions();
    }

    synchronized public DataInterpreter.WifiGrade notifyWifiState(int primaryApSignal,
                                                                  String primaryApSSID,
                                                                  int primaryApLinkSpeed,
                                                                  int primaryApChannel,
                                                                  int primaryApChannelInterferingAps,
                                                                  List<ScanResult> scanResultList,
                                                                  List<WifiConfiguration> wifiConfigurationList,
                                                                  HashMap<Long, String> networkStateTransitions,
                                                                  @WifiStateData.WifiProblemMode int wifiProblemMode,
                                                                  @ConnectivityTester.WifiConnectivityMode int wifiConnectivityMode) {
        DataInterpreter.WifiGrade wifiGrade = DataInterpreter.interpret(primaryApSignal, primaryApSSID,
                primaryApLinkSpeed, primaryApChannel, primaryApChannelInterferingAps, scanResultList,
                wifiConfigurationList, networkStateTransitions, wifiProblemMode, wifiConnectivityMode);
        metricsDb.updateWifiGrade(wifiGrade);
        PossibleConditions conditions = InferenceMap.getPossibleConditionsFor(wifiGrade);
        currentConditions.mergeIn(conditions);
        DobbyLog.i("InferenceEngine Wifi Grade: " + wifiGrade.toString());
        DobbyLog.i("InferenceEngine which gives conditions: " + conditions.toString());
        DobbyLog.i("InferenceEngine After merging: " + currentConditions.toString());
        checkAndSendSuggestions();
        return wifiGrade;
    }

    synchronized public DataInterpreter.PingGrade notifyPingStats(HashMap<String, PingStats> pingStatsMap) {
        DataInterpreter.PingGrade pingGrade = DataInterpreter.interpret(pingStatsMap);
        metricsDb.updatePingGrade(pingGrade);
        PossibleConditions conditions = InferenceMap.getPossibleConditionsFor(pingGrade);
        currentConditions.mergeIn(conditions);
        DobbyLog.i("InferenceEngine Ping Grade: " + pingGrade.toString());
        DobbyLog.i("InferenceEngine which gives conditions: " + conditions.toString());
        DobbyLog.i("InferenceEngine After merging: " + currentConditions.toString());
        checkAndSendSuggestions();
        return pingGrade;
    }

    synchronized public DataInterpreter.HttpGrade notifyGatewayHttpStats(PingStats gatewayHttpStats) {
        DataInterpreter.HttpGrade httpGrade = DataInterpreter.interpret(gatewayHttpStats);
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

    // Bandwidth test notifications:
    public void notifyBandwidthTestStart(@ActionLibraryCodes.BandwidthTestMode int testMode) {
        if (testMode == ActionLibraryCodes.BandwidthTestMode.UPLOAD) {
            metricsDb.clearUploadBandwidthGrade();
        } else if (testMode == ActionLibraryCodes.BandwidthTestMode.DOWNLOAD) {
            metricsDb.clearDownloadBandwidthGrade();
        }
    }

    public void notifyBandwidthTestProgress(@ActionLibraryCodes.BandwidthTestMode int testMode, double bandwidth) {
        long currentTs = System.currentTimeMillis();
        if ((currentTs - lastBandwidthUpdateTimestampMs) > 500L) {
            // sendResponseOnlyAction(testModeToString(testMode) + " Current Bandwidth: " + String.format("%.2f", bandwidth / 1000000) + " Mbps");
            lastBandwidthUpdateTimestampMs = currentTs;
        }
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
        inferenceRecord.appVersion = DobbyApplication.getAppVersion();

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

    private FailureRecord createFailureRecord(@ActionLibraryCodes.BandwidthTestMode int testMode,
                                              @ActionLibraryCodes.ErrorCodes int errorCode,
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

    synchronized public DataInterpreter.BandwidthGrade notifyBandwidthTestResult(@ActionLibraryCodes.BandwidthTestMode int testMode,
                                                                                 double bandwidth,
                                                                                 String clientIsp,
                                                                                 String clientExternalIp,
                                                                                 double lat, double lon,
                                                                                 String bestServerName,
                                                                                 String bestServerCountry,
                                                                                 double bestServerLatency) {
        DataInterpreter.BandwidthGrade bandwidthGrade = new DataInterpreter.BandwidthGrade();
        lastBandwidthUpdateTimestampMs = 0;

        if (testMode == ActionLibraryCodes.BandwidthTestMode.UPLOAD) {
            metricsDb.updateUploadBandwidthGrade(bandwidth * 1.0e-6, DataInterpreter.MetricType.UNKNOWN);
        } else if (testMode == ActionLibraryCodes.BandwidthTestMode.DOWNLOAD) {
            metricsDb.updateDownloadBandwidthGrade(bandwidth * 1.0e-6, DataInterpreter.MetricType.UNKNOWN);
        }

        if (metricsDb.hasValidDownload() && metricsDb.hasValidUpload()) {
            bandwidthGrade = DataInterpreter.interpret(
                    metricsDb.getDownloadMbps(),
                    metricsDb.getUploadMbps(),
                    clientIsp,
                    clientExternalIp,
                    lat,
                    lon,
                    bestServerName,
                    bestServerCountry,
                    bestServerLatency,
                    ActionLibraryCodes.ErrorCodes.NO_ERROR);
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


    synchronized public DataInterpreter.BandwidthGrade notifyBandwidthTestError(@ActionLibraryCodes.BandwidthTestMode int testMode,
                                                                                @ActionLibraryCodes.ErrorCodes int errorCode,
                                                                                String errorMessage,
                                                                                double bandwidth) {
        lastBandwidthUpdateTimestampMs = 0;
        DataInterpreter.BandwidthGrade bandwidthGrade = DataInterpreter.interpret(
                bandwidth, bandwidth,
                Utils.EMPTY_STRING, Utils.EMPTY_STRING,
                0, 0,
                Utils.EMPTY_STRING, Utils.EMPTY_STRING,
                0.0,
                errorCode);
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
        metricsDb.cleanup();
    }

    private void writeFailureToDatabase(@ActionLibraryCodes.BandwidthTestMode int testMode,
                                        @ActionLibraryCodes.ErrorCodes int errorCode,
                                        String errorMessage) {
        //Write failure to database
        FailureRecord newFailureRecord = createFailureRecord(testMode, errorCode, errorMessage);
        failureDatabaseWriter.writeFailureToDatabase(newFailureRecord);
    }

}
