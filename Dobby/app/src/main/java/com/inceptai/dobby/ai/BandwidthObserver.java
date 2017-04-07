package com.inceptai.dobby.ai;

import android.support.annotation.Nullable;

import com.inceptai.dobby.speedtest.BandwidthAggregator;
import com.inceptai.dobby.speedtest.BandwithTestCodes;
import com.inceptai.dobby.speedtest.NewBandwidthAnalyzer;
import com.inceptai.dobby.speedtest.ServerInformation;
import com.inceptai.dobby.speedtest.SpeedTestConfig;

import java.util.List;

/**
 * Created by arunesh on 4/6/17.
 */

public class BandwidthObserver implements NewBandwidthAnalyzer.ResultsCallback {
    private InferenceEngine inferenceEngine;

    public BandwidthObserver(InferenceEngine inferenceEngine) {
        this.inferenceEngine = inferenceEngine;
    }

    @Override
    public void onConfigFetch(SpeedTestConfig config) {

    }

    @Override
    public void onServerInformationFetch(ServerInformation serverInformation) {

    }

    @Override
    public void onClosestServersSelected(List<ServerInformation.ServerDetails> closestServers) {

    }

    @Override
    public void onBestServerSelected(ServerInformation.ServerDetails bestServer) {

    }

    @Override
    public void onTestFinished(@BandwithTestCodes.BandwidthTestMode int testMode, BandwidthAggregator.BandwidthStats stats) {
        inferenceEngine.notifyBandwidthTestResult(stats.percentile90Throughput);
    }

    @Override
    public void onTestProgress(@BandwithTestCodes.BandwidthTestMode int testMode, double instantBandwidth) {
        inferenceEngine.notifyBandwidthTestProgress(instantBandwidth);
    }

    @Override
    public void onBandwidthTestError(@BandwithTestCodes.BandwidthTestMode int testMode, @BandwithTestCodes.BandwidthTestErrorCodes int errorCode, @Nullable String errorMessage) {

    }
}
