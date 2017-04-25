package com.inceptai.dobby.speedtest;

import android.support.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.inceptai.dobby.ai.InferenceEngine;
import com.inceptai.dobby.ai.RtDataSource;
import com.inceptai.dobby.model.BandwidthStats;
import com.inceptai.dobby.utils.DobbyLog;
import com.inceptai.dobby.utils.Utils;

import java.util.HashSet;
import java.util.List;

/**
 * Created by arunesh on 4/6/17.
 */

public class BandwidthObserver implements NewBandwidthAnalyzer.ResultsCallback, RtDataSource<Float, Integer> {
    @Nullable
    private InferenceEngine inferenceEngine;
    private HashSet<RtDataListener<Float>> listenersUpload;
    private HashSet<RtDataListener<Float>> listenersDownload;
    private HashSet<NewBandwidthAnalyzer.ResultsCallback> resultsCallbacks;
    private boolean testsRunning = false;
    private SettableFuture<BandwidthResult> operationFuture;
    private String clientIsp = Utils.EMPTY_STRING;
    private String clientExternalIp = Utils.EMPTY_STRING;
    private BandwidthResult result;

    @BandwithTestCodes.TestMode
    private int testModeRequested;

    @BandwithTestCodes.TestMode private int testsDone;

    public BandwidthObserver(@BandwithTestCodes.TestMode int testMode) {
        this.testModeRequested = testMode;
        listenersUpload = new HashSet<>();
        listenersDownload = new HashSet<>();
        resultsCallbacks = new HashSet<>();
        markTestsAsRunning();
        operationFuture = SettableFuture.create();
        result = new BandwidthResult(testMode);
        testsDone = BandwithTestCodes.TestMode.IDLE;
    }

    public synchronized void setInferenceEngine(InferenceEngine inferenceEngine) {
        this.inferenceEngine = inferenceEngine;
    }

    public synchronized void onCancelled() {
        // Tests cancelled.
        testsDone();
    }

    public synchronized boolean testsRunning() {
        return testsRunning;
    }

    public synchronized void registerCallback(NewBandwidthAnalyzer.ResultsCallback callback) {
        resultsCallbacks.add(callback);
    }

    public synchronized void unregisterCallback(NewBandwidthAnalyzer.ResultsCallback callback) {
        resultsCallbacks.remove(callback);
    }

    public ListenableFuture<BandwidthResult> asFuture() {
        return operationFuture;
    }

    @BandwithTestCodes.TestMode
    public int getTestModeRequested() {
        return testModeRequested;
    }

    // NewBandwidthAnalyzer.ResultsCallback overrides:

    @Override
    public synchronized void onConfigFetch(SpeedTestConfig config) {
        //Set client info
        if (config != null && config.clientConfig != null) {
            clientIsp = config.clientConfig.isp;
            clientExternalIp = config.clientConfig.ip;
        }
        for (NewBandwidthAnalyzer.ResultsCallback callback : resultsCallbacks) {
            callback.onConfigFetch(config);
        }
    }

    @Override
    public synchronized void onServerInformationFetch(ServerInformation serverInformation) {
        for (NewBandwidthAnalyzer.ResultsCallback callback : resultsCallbacks) {
            callback.onServerInformationFetch(serverInformation);
        }
    }

    @Override
    public synchronized void onClosestServersSelected(List<ServerInformation.ServerDetails> closestServers) {
        for (NewBandwidthAnalyzer.ResultsCallback callback : resultsCallbacks) {
            callback.onClosestServersSelected(closestServers);
        }
    }

    @Override
    public synchronized void onBestServerSelected(ServerInformation.ServerDetails bestServer) {
        for (NewBandwidthAnalyzer.ResultsCallback callback : resultsCallbacks) {
            callback.onBestServerSelected(bestServer);
        }
    }

    @Override
    public synchronized void onTestFinished(@BandwithTestCodes.TestMode int testMode, BandwidthStats stats) {
        DobbyLog.v("BandwidthObserver onTestFinished");
        if (inferenceEngine != null) {
            inferenceEngine.notifyBandwidthTestResult(testMode, stats.getPercentile90(), clientIsp, clientExternalIp);
        }

        HashSet<RtDataListener<Float>> listenerSet = testMode == BandwithTestCodes.TestMode.UPLOAD ? listenersUpload : listenersDownload;
        for (RtDataListener<Float> listener : listenerSet) {
            listener.onClose();
        }

        for (NewBandwidthAnalyzer.ResultsCallback callback : resultsCallbacks) {
            callback.onTestFinished(testMode, stats);
        }
        if (testMode == BandwithTestCodes.TestMode.UPLOAD) {
            result.setUploadStats(stats);
        } else if (testMode == BandwithTestCodes.TestMode.DOWNLOAD) {
            result.setDownloadStats(stats);
        }

        if (areTestsDone(testMode)) {
            testsDone();
        }
    }

    @Override
    public synchronized void onTestProgress(@BandwithTestCodes.TestMode int testMode, double instantBandwidth) {
        DobbyLog.v("BandwidthObserver onTestProgress");
        if (inferenceEngine != null) {
            inferenceEngine.notifyBandwidthTestProgress(testMode, instantBandwidth);
        }

        HashSet<RtDataListener<Float>> listenerSet = testMode == BandwithTestCodes.TestMode.UPLOAD ? listenersUpload : listenersDownload;
        for (RtDataListener<Float> listener : listenerSet) {
            listener.onUpdate((float) (instantBandwidth / 1.0E6));
        }
        for (NewBandwidthAnalyzer.ResultsCallback callback : resultsCallbacks) {
            callback.onTestProgress(testMode, instantBandwidth);
        }
    }

    @Override
    public synchronized void onBandwidthTestError(@BandwithTestCodes.TestMode int testMode,
                                     @BandwithTestCodes.ErrorCodes int errorCode,
                                     @Nullable String errorMessage) {
        //TODO: Inform the inference engine that we encountered an error during bandwidth tests.
        for (NewBandwidthAnalyzer.ResultsCallback callback : resultsCallbacks) {
            callback.onBandwidthTestError(testMode, errorCode, errorMessage);
        }

        for (RtDataListener<Float> listener : listenersDownload) {
            listener.onClose();
        }

        for (RtDataListener<Float> listener : listenersUpload) {
            listener.onClose();
        }


        testsDone();
    }

    @Override
    public synchronized void registerListener(RtDataListener<Float> listener, Integer sourceType) {
        if (sourceType == BandwithTestCodes.TestMode.UPLOAD) {
            listenersUpload.add(listener);
        } else if (sourceType == BandwithTestCodes.TestMode.DOWNLOAD) {
            listenersDownload.add(listener);
        }
    }

    @Override
    public synchronized void unregisterListener(RtDataListener<Float> listener) {
        listenersUpload.remove(listener);
        listenersDownload.remove(listener);
    }

    private void testsDone() {
        if (operationFuture != null) {
            boolean setResult = operationFuture.set(result);
            DobbyLog.v("Setting bwtest result was " + setResult);
        }
        testsRunning = false;
        listenersUpload.clear();
        listenersDownload.clear();
        resultsCallbacks.clear();
    }

    private synchronized void markTestsAsRunning() {
        testsRunning = true;
        if (inferenceEngine != null) {
            inferenceEngine.notifyBandwidthTestStart(testModeRequested);
        }
    }

    private boolean areTestsDone(@BandwithTestCodes.TestMode int testModeDone) {
        if (testsDone == BandwithTestCodes.TestMode.IDLE) {
            testsDone = testModeDone;
            return testsDone == testModeRequested;
        }

        if (testsDone == BandwithTestCodes.TestMode.UPLOAD && testModeDone == BandwithTestCodes.TestMode.DOWNLOAD) {
            testsDone = BandwithTestCodes.TestMode.DOWNLOAD_AND_UPLOAD;
        }

        if (testsDone == BandwithTestCodes.TestMode.DOWNLOAD && testModeDone == BandwithTestCodes.TestMode.UPLOAD) {
            testsDone = BandwithTestCodes.TestMode.DOWNLOAD_AND_UPLOAD;
        }
        return testsDone == testModeRequested;
    }
}
