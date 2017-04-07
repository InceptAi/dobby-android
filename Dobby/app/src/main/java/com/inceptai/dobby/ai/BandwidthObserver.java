package com.inceptai.dobby.ai;

import android.support.annotation.Nullable;

import com.inceptai.dobby.model.BandwidthStats;
import com.inceptai.dobby.speedtest.BandwidthAggregator;
import com.inceptai.dobby.speedtest.BandwithTestCodes;
import com.inceptai.dobby.speedtest.NewBandwidthAnalyzer;
import com.inceptai.dobby.speedtest.ServerInformation;
import com.inceptai.dobby.speedtest.SpeedTestConfig;

import java.util.HashSet;
import java.util.List;

/**
 * Created by arunesh on 4/6/17.
 */

public class BandwidthObserver implements NewBandwidthAnalyzer.ResultsCallback, RtDataSource<Float> {
    private InferenceEngine inferenceEngine;
    private HashSet<RtDataListener<Float>> listeners;

    public BandwidthObserver(InferenceEngine inferenceEngine) {
        this.inferenceEngine = inferenceEngine;
        listeners = new HashSet<>();
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
    public void onTestFinished(@BandwithTestCodes.BandwidthTestMode int testMode, BandwidthStats stats) {
        inferenceEngine.notifyBandwidthTestResult(stats.getPercentile90());

        for (RtDataListener<Float> listener : listeners) {
            listener.onClose();
        }
        listeners.clear();
    }

    @Override
    public void onTestProgress(@BandwithTestCodes.BandwidthTestMode int testMode, double instantBandwidth) {
        inferenceEngine.notifyBandwidthTestProgress(instantBandwidth);

        for (RtDataListener<Float> listener : listeners) {
            listener.onUpdate((float) (instantBandwidth / 1.0E6));
        }
    }

    @Override
    public void onBandwidthTestError(@BandwithTestCodes.BandwidthTestMode int testMode, @BandwithTestCodes.BandwidthTestErrorCodes int errorCode, @Nullable String errorMessage) {

    }

    @Override
    public void registerListener(RtDataListener<Float> listener) {
        listeners.add(listener);
    }

    @Override
    public void unregisterListener(RtDataListener<Float> listener) {
        listeners.remove(listener);
    }
}
