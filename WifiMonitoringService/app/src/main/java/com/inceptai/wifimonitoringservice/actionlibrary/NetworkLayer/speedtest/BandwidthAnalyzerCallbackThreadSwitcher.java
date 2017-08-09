package com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.speedtest;

import android.support.annotation.Nullable;

import com.inceptai.wifimonitoringservice.actionlibrary.utils.ActionLibraryCodes;
import com.inceptai.wifimonitoringservice.utils.ServiceLog;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * Created by vivek on 4/26/17.
 */

public class BandwidthAnalyzerCallbackThreadSwitcher implements BandwidthAnalyzer.ResultsCallback {

    private ExecutorService executorService;
    private BandwidthAnalyzer.ResultsCallback resultsCallback;

    BandwidthAnalyzerCallbackThreadSwitcher(ExecutorService service, BandwidthAnalyzer.ResultsCallback delegate) {
        this.executorService = service;
        this.resultsCallback = delegate;
    }

    public static BandwidthAnalyzerCallbackThreadSwitcher wrap(ExecutorService service, BandwidthAnalyzer.ResultsCallback delegate) {
        return new BandwidthAnalyzerCallbackThreadSwitcher(service, delegate);
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
    public void onTestFinished(final @ActionLibraryCodes.BandwidthTestMode int testMode,
                               final BandwidthStats stats) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                resultsCallback.onTestFinished(testMode, stats);
            }
        });
    }

    @Override
    public void onTestProgress(final @ActionLibraryCodes.BandwidthTestMode int testMode,
                               final double instantBandwidth) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                resultsCallback.onTestProgress(testMode, instantBandwidth);
            }
        });
    }

    @Override
    public void onBandwidthTestError(final @ActionLibraryCodes.BandwidthTestMode int testMode,
                                     final @ActionLibraryCodes.ErrorCodes int errorCode,
                                     final @Nullable String errorMessage) {
        ServiceLog.e("CallbackSwitcher: Sending bw test error : mode " + testMode + " errorCode" + errorCode);
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                resultsCallback.onBandwidthTestError(testMode, errorCode, errorMessage);
            }
        });
    }
}
