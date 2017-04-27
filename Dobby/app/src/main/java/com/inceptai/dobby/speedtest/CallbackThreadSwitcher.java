package com.inceptai.dobby.speedtest;

import android.support.annotation.Nullable;

import com.inceptai.dobby.model.BandwidthStats;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Created by vivek on 4/26/17.
 */

public class CallbackThreadSwitcher implements NewBandwidthAnalyzer.ResultsCallback {

    private ExecutorService executorService;
    private NewBandwidthAnalyzer.ResultsCallback resultsCallback;

    CallbackThreadSwitcher(ExecutorService service, NewBandwidthAnalyzer.ResultsCallback delegate) {
        this.executorService = service;
        this.resultsCallback = delegate;
    }

    public static CallbackThreadSwitcher wrap(ExecutorService service, NewBandwidthAnalyzer.ResultsCallback delegate) {
        return new CallbackThreadSwitcher(service, delegate);
    }

    @Override
    public void onConfigFetch(final SpeedTestConfig config) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                resultsCallback.onConfigFetch(config);
            }
        });
    }

    @Override
    public void onServerInformationFetch(final ServerInformation serverInformation) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                resultsCallback.onServerInformationFetch(serverInformation);
            }
        });
    }

    @Override
    public void onClosestServersSelected(final List<ServerInformation.ServerDetails> closestServers) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                resultsCallback.onClosestServersSelected(closestServers);
            }
        });
    }

    @Override
    public void onBestServerSelected(final ServerInformation.ServerDetails bestServer) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                resultsCallback.onBestServerSelected(bestServer);
            }
        });
    }

    @Override
    public void onTestFinished(final @BandwithTestCodes.TestMode int testMode,
                               final BandwidthStats stats) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                resultsCallback.onTestFinished(testMode, stats);
            }
        });
    }

    @Override
    public void onTestProgress(final @BandwithTestCodes.TestMode int testMode,
                               final double instantBandwidth) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                resultsCallback.onTestProgress(testMode, instantBandwidth);
            }
        });
    }

    @Override
    public void onBandwidthTestError(final @BandwithTestCodes.TestMode int testMode,
                                     final @BandwithTestCodes.ErrorCodes int errorCode,
                                     final @Nullable String errorMessage) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                resultsCallback.onBandwidthTestError(testMode, errorCode, errorMessage);
            }
        });
    }
}
