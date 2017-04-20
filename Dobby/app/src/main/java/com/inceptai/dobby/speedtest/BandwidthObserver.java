package com.inceptai.dobby.speedtest;

import android.support.annotation.Nullable;
import android.util.Log;

import com.inceptai.dobby.ai.InferenceEngine;
import com.inceptai.dobby.ai.RtDataSource;
import com.inceptai.dobby.model.BandwidthStats;

import java.util.HashSet;
import java.util.List;

import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * Created by arunesh on 4/6/17.
 */

public class BandwidthObserver implements NewBandwidthAnalyzer.ResultsCallback, RtDataSource<Float> {
    @Nullable
    private InferenceEngine inferenceEngine;
    private HashSet<RtDataListener<Float>> listeners;
    private HashSet<NewBandwidthAnalyzer.ResultsCallback> resultsCallbacks;
    private boolean testsRunning = false;
    @BandwithTestCodes.BandwidthTestExceptionErrorCodes
    public int bandwidthTestState = BandwithTestCodes.BandwidthTestExceptionErrorCodes.UNKNOWN;

    @BandwithTestCodes.BandwidthTestMode
    private int testMode;

    public BandwidthObserver(@BandwithTestCodes.BandwidthTestMode int testMode) {
        this.testMode = testMode;
        listeners = new HashSet<>();
        resultsCallbacks = new HashSet<>();
        testsStarting();
    }

    public synchronized void setInferenceEngine(InferenceEngine inferenceEngine) {
        this.inferenceEngine = inferenceEngine;
    }

    public synchronized void onCancelled() {
        // Tests cancelled.
        testsRunning = false;
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

    @BandwithTestCodes.BandwidthTestMode
    public int getTestMode() {
        return testMode;
    }

    private synchronized void testsStarting() {
        testsRunning = true;
        if (inferenceEngine != null) {
            inferenceEngine.notifyBandwidthTestStart(testMode);
        }
    }

    // NewBandwidthAnalyzer.ResultsCallback overrides:

    @Override
    public synchronized void onConfigFetch(SpeedTestConfig config) {
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
    public synchronized void onTestFinished(@BandwithTestCodes.BandwidthTestMode int testMode, BandwidthStats stats) {
        Log.v(TAG, "BandwidthObserver onTestFinished");
        if (inferenceEngine != null) {
            inferenceEngine.notifyBandwidthTestResult(testMode, stats.getPercentile90());
        }

        for (RtDataListener<Float> listener : listeners) {
            listener.onClose();
        }
        listeners.clear();
        testsRunning = false;
        for (NewBandwidthAnalyzer.ResultsCallback callback : resultsCallbacks) {
            callback.onTestFinished(testMode, stats);
        }
    }

    @Override
    public synchronized void onTestProgress(@BandwithTestCodes.BandwidthTestMode int testMode, double instantBandwidth) {
        Log.v(TAG, "BandwidthObserver onTestProgress");
        if (inferenceEngine != null) {
            inferenceEngine.notifyBandwidthTestProgress(testMode, instantBandwidth);
        }

        for (RtDataListener<Float> listener : listeners) {
            listener.onUpdate((float) (instantBandwidth / 1.0E6));
        }
        for (NewBandwidthAnalyzer.ResultsCallback callback : resultsCallbacks) {
            callback.onTestProgress(testMode, instantBandwidth);
        }
        testsRunning = false;
        // resultsCallbacks.clear();
    }

    @Override
    public synchronized void onBandwidthTestError(@BandwithTestCodes.BandwidthTestMode int testMode,
                                     @BandwithTestCodes.BandwidthTestErrorCodes int errorCode,
                                     @Nullable String errorMessage) {
        for (NewBandwidthAnalyzer.ResultsCallback callback : resultsCallbacks) {
            callback.onBandwidthTestError(testMode, errorCode, errorMessage);
        }
        testsRunning = false;
    }

    @Override
    public synchronized void registerListener(RtDataListener<Float> listener) {
        listeners.add(listener);
    }

    @Override
    public synchronized void unregisterListener(RtDataListener<Float> listener) {
        listeners.remove(listener);
    }
}
