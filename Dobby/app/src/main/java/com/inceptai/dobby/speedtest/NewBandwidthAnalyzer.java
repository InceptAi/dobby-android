package com.inceptai.dobby.speedtest;

/**
 * Created by vivek on 3/30/17.
 */

import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.base.Preconditions;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.model.BandwidthStats;
import com.inceptai.dobby.speedtest.BandwithTestCodes.BandwidthTestErrorCodes;
import com.inceptai.dobby.speedtest.BandwithTestCodes.BandwidthTestMode;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.bmartel.speedtest.model.SpeedTestError;

import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * Class contains logic performing bandwidth tests.
 */
@Singleton
public class NewBandwidthAnalyzer {
    private static final int DOWNLOAD_THREADS = 2;
    private static final int UPLOAD_THREADS = 2;
    private static final int REPORT_INTERVAL_MS = 500;

    @IntDef({BandwidthAnalyzerState.STOPPED, BandwidthAnalyzerState.RUNNING,
            BandwidthAnalyzerState.CANCELLING, BandwidthAnalyzerState.CANCELLED})
    @Retention(RetentionPolicy.SOURCE)
    public @interface BandwidthAnalyzerState {
        int STOPPED = 0;
        int RUNNING = 1;
        int CANCELLING = 2;
        int CANCELLED = 4;
    }

    @BandwidthAnalyzerState
    private int bandwidthAnalyzerState;

    private ParseSpeedTestConfig parseSpeedTestConfig;
    private ParseServerInformation parseServerInformation;
    private SpeedTestConfig speedTestConfig;
    private ServerInformation.ServerDetails bestServer;
    private NewDownloadAnalyzer downloadAnalyzer;
    private NewUploadAnalyzer uploadAnalyzer;
    private DobbyThreadpool dobbyThreadpool;
    private DobbyEventBus eventBus;

    private BandwidthTestListener bandwidthTestListener;

    @BandwidthTestMode
    private int testMode;
    //Callbacks
    private ResultsCallback resultsCallback;

    public static class BandwidthTestException extends Exception {
        public static final int BW_EXCEPTION_TEST_ALREADY_RUNNING = 1;
        public static final int BW_EXCEPTION_GETTING_CONFIG_FAILED = 2;
        public static final int BW_EXCEPTION_GETTING_SERVER_INFORMATION_FAILED = 3;
        public static final int BW_EXCEPTION_GETTING_BEST_SERVER_FAILED = 4;
        private int exceptionType = 0;
        public BandwidthTestException(int exceptionType) {
            this.exceptionType = exceptionType;
        }

        @Override
        public String toString() {
            switch(exceptionType) {
                case BW_EXCEPTION_TEST_ALREADY_RUNNING:
                    return "BW Test already running";
                case BW_EXCEPTION_GETTING_CONFIG_FAILED:
                    return "BW Test getting config failed";
                case BW_EXCEPTION_GETTING_SERVER_INFORMATION_FAILED:
                    return "BW Test getting servers failed";
                case BW_EXCEPTION_GETTING_BEST_SERVER_FAILED:
                    return "BW Test getting best server failed";
                default:
                    return "Unknown BW Test Error";
            }
        }
    }

    /**
     * Callback interface for results. More methods to follow.
     */
    public interface ResultsCallback {

        //Config callbacks
        void onConfigFetch(SpeedTestConfig config);
        //Getting nearby servers
        void onServerInformationFetch(ServerInformation serverInformation);
        //Closest servers selected
        void onClosestServersSelected(List<ServerInformation.ServerDetails> closestServers);
        //Best server callbacks
        void onBestServerSelected(ServerInformation.ServerDetails bestServer);

        //Test callbacks
        void onTestFinished(@BandwidthTestMode int testMode, BandwidthStats stats);
        void onTestProgress(@BandwidthTestMode int testMode, double instantBandwidth);

        //Error callback
        void onBandwidthTestError(@BandwidthTestMode int testMode,
                                  @BandwidthTestErrorCodes int errorCode,
                                  @Nullable String errorMessage);
    }

    @Inject
    public NewBandwidthAnalyzer(DobbyThreadpool dobbyThreadpool, DobbyEventBus eventBus) {
        this.bandwidthTestListener = new BandwidthTestListener();
        this.testMode = BandwidthTestMode.IDLE;
        this.eventBus = eventBus;
        this.parseSpeedTestConfig = new ParseSpeedTestConfig(this.bandwidthTestListener);
        this.parseServerInformation = new ParseServerInformation(this.bandwidthTestListener);
        this.dobbyThreadpool = dobbyThreadpool;
        Log.e(TAG, "NEW BANDWIDTH ANALYZER INSTANCE CREATED.");
    }

    /**
     * Factory constructor to create an instance
     * @return Instance of BandwidthAnalyzer or null on error.
     */
    @Nullable
    public static NewBandwidthAnalyzer create(ResultsCallback resultsCallback, DobbyThreadpool dobbyThreadpool, DobbyEventBus eventBus) {
        Preconditions.checkNotNull(dobbyThreadpool);
        NewBandwidthAnalyzer analyzer = new NewBandwidthAnalyzer(dobbyThreadpool, eventBus);
        analyzer.registerCallback(resultsCallback);
        return analyzer;
    }

    /**
     * Registers new callback -- overrides old listener
     * @param resultsCallback
     */
    public void registerCallback(ResultsCallback resultsCallback) {
        this.resultsCallback = resultsCallback;
    }

    public void startBandwidthTestSafely(@BandwidthTestMode int testMode) {
        try {
            startBandwidthTest(testMode);
        } catch (Exception e) {
            Log.v(TAG, "Exception: " + e);
            finishTests();
        }
    }

    public void cancelBandwidthTests() {
        markTestsAsCancelling();
        downloadAnalyzer.cancelAllTests();
        uploadAnalyzer.cancelAllTests();
        markTestsAsCancelled();
    }

    /**
     * Un Registers callback -- sets to null
     */
    public void unRegisterCallback() {
        this.resultsCallback = null;
    }

    public void cleanup() {
        unRegisterCallback();
    }

    public class BandwidthTestListener implements ParseSpeedTestConfig.ResultsCallback,
            ParseServerInformation.ResultsCallback, NewDownloadAnalyzer.ResultsCallback,
            NewUploadAnalyzer.ResultsCallback {

        //Config callbacks
        public void onConfigFetch(SpeedTestConfig config) {
            Log.v(TAG, "Speed test config fetched");
            if (resultsCallback != null) {
                resultsCallback.onConfigFetch(config);
            }
        }

        public void onConfigFetchError(String error) {
            Log.v(TAG, "Speed test config fetched error: " + error);
            if (resultsCallback != null) {
                resultsCallback.onBandwidthTestError(BandwidthTestMode.CONFIG_FETCH,
                        BandwidthTestErrorCodes.ERROR_FETCHING_CONFIG,
                        error);
            }
        }

        // Server information callbacks
        public void onServerInformationFetch(ServerInformation serverInformation) {
            Log.v(TAG, "Speed test server informatio fetched, num servers:" + serverInformation.serverList.size());
            if (resultsCallback != null) {
                if (serverInformation != null && serverInformation.serverList.size() > 0) {
                    resultsCallback.onServerInformationFetch(serverInformation);
                } else {
                    resultsCallback.onBandwidthTestError(BandwidthTestMode.SERVER_FETCH,
                            BandwidthTestErrorCodes.ERROR_FETCHING_SERVER_INFO,
                            "Server information returned empty");
                }
            }
        }

        public void onServerInformationFetchError(String error) {
            if (resultsCallback != null) {
                resultsCallback.onBandwidthTestError(BandwidthTestMode.SERVER_FETCH,
                        BandwidthTestErrorCodes.ERROR_FETCHING_SERVER_INFO,
                        error);
            }
        }

        //Best server selection
        public void onBestServerSelected(ServerInformation.ServerDetails bestServer) {
            if (resultsCallback != null)
                resultsCallback.onBestServerSelected(bestServer);
        }

        public void onBestServerSelectionError(String error) {
            if (resultsCallback != null)
                resultsCallback.onBandwidthTestError(BandwidthTestMode.SERVER_FETCH,
                        BandwidthTestErrorCodes.ERROR_SELECTING_BEST_SERVER, error);
        }

        public void onClosestServersSelected(List<ServerInformation.ServerDetails> closestServers) {
            if (resultsCallback != null)
                resultsCallback.onClosestServersSelected(closestServers);
        }

        @Override
        public void onFinish(@BandwidthTestMode int callbackTestMode, BandwidthStats bandwidthStats) {
            if (resultsCallback != null) {
                resultsCallback.onTestFinished(callbackTestMode, bandwidthStats);
            }
            //Do we need to do upload here ?
            if (callbackTestMode == BandwidthTestMode.DOWNLOAD && testMode == BandwidthTestMode.DOWNLOAD_AND_UPLOAD) {
                dobbyThreadpool.submit(new Runnable() {
                    @Override
                    public void run() {
                        performUpload();
                    }
                });
            } else {
                //Cleanup
                finishTests();
            }
        }

        @Override
        public void onProgress(@BandwidthTestMode int callbackTestMode, double instantBandwidth) {
            if (resultsCallback != null) {
                resultsCallback.onTestProgress(callbackTestMode, instantBandwidth);
            }
        }

        @Override
        public void onError(@BandwidthTestMode int callbackTestMode, SpeedTestError speedTestError, String errorMessage) {
            if (resultsCallback != null) {
                resultsCallback.onBandwidthTestError(callbackTestMode,
                        convertToBandwidthTestCodes(speedTestError), errorMessage);
            }
        }
    }

    private ServerInformation.ServerDetails getBestServer(SpeedTestConfig config,
                                                          ServerInformation info) {
        BestServerSelector serverSelector = new BestServerSelector(config, info, null);
        return serverSelector.getBestServer();
    }

    private void  performDownload() {
        if (downloadAnalyzer == null) {
            downloadAnalyzer = new NewDownloadAnalyzer(speedTestConfig.downloadConfig,
                    bestServer, bandwidthTestListener);
        }
        downloadAnalyzer.downloadTestWithMultipleThreads(DOWNLOAD_THREADS,REPORT_INTERVAL_MS);
    }

    private void performUpload() {
        if (uploadAnalyzer == null) {
            uploadAnalyzer = new NewUploadAnalyzer(speedTestConfig.uploadConfig,
                    bestServer, bandwidthTestListener);
        }
        uploadAnalyzer.uploadTestWithMultipleThreads(UPLOAD_THREADS, REPORT_INTERVAL_MS);
    }

    private void finishTests() {
        markTestsAsStopped();
    }

    //Helper functions for state
    private boolean testsCurrentlyRunning() {
        return bandwidthAnalyzerState == BandwidthAnalyzerState.RUNNING;
    }

    private boolean testsCurrentlyInactive() {
        return (bandwidthAnalyzerState == BandwidthAnalyzerState.STOPPED ||
                bandwidthAnalyzerState == BandwidthAnalyzerState.CANCELLED);
    }

    private void markTestsAsRunning() throws BandwidthTestException {
        if (!testsCurrentlyInactive()) {
            throw new BandwidthTestException(BandwidthTestException.BW_EXCEPTION_TEST_ALREADY_RUNNING);
        }
        bandwidthAnalyzerState = BandwidthAnalyzerState.RUNNING;
    }

    private void markTestsAsStopped() {
        bandwidthAnalyzerState = BandwidthAnalyzerState.STOPPED;
    }

    private void markTestsAsCancelled() {
        bandwidthAnalyzerState = BandwidthAnalyzerState.CANCELLED;
    }

    private void markTestsAsCancelling() {
        bandwidthAnalyzerState = BandwidthAnalyzerState.CANCELLING;
    }

    /**
     * start the speed test
     */
    private void startBandwidthTest(@BandwidthTestMode int testMode) throws
            BandwidthTestException {
        markTestsAsRunning();
        final String downloadMode = "http";
        this.testMode = testMode;
        //Get config
        speedTestConfig = parseSpeedTestConfig.getConfig(downloadMode);
        if (speedTestConfig == null) {
            if (resultsCallback != null) {
                resultsCallback.onBandwidthTestError(BandwidthTestMode.CONFIG_FETCH,
                        BandwidthTestErrorCodes.ERROR_FETCHING_CONFIG,
                        "Config fetch returned null");
            }
            throw new BandwidthTestException(BandwidthTestException.BW_EXCEPTION_GETTING_CONFIG_FAILED);
        }
        ServerInformation info = parseServerInformation.getServerInfo();
        if (info == null) {
            if (resultsCallback != null) {
                resultsCallback.onBandwidthTestError(BandwidthTestMode.SERVER_FETCH,
                        BandwidthTestErrorCodes.ERROR_FETCHING_SERVER_INFO,
                        "Server info fetch returned null");
            }
            throw new BandwidthTestException(BandwidthTestException.BW_EXCEPTION_GETTING_SERVER_INFORMATION_FAILED);
        }

        bestServer = getBestServer(speedTestConfig, info);
        if (bestServer == null) {
            if (resultsCallback != null) {
                resultsCallback.onBandwidthTestError(BandwidthTestMode.SERVER_FETCH,
                        BandwidthTestErrorCodes.ERROR_SELECTING_BEST_SERVER,
                        "best server returned as null");
            }
            throw new BandwidthTestException(BandwidthTestException.BW_EXCEPTION_GETTING_BEST_SERVER_FAILED);
        }

        if (testMode == BandwidthTestMode.DOWNLOAD_AND_UPLOAD || testMode == BandwidthTestMode.DOWNLOAD) {
            performDownload();
        }
        if (testMode == BandwidthTestMode.UPLOAD) {
            performUpload();
        }
    }

    @BandwidthTestErrorCodes
    private int convertToBandwidthTestCodes(SpeedTestError error) {
        int errorCodeToReturn = BandwidthTestErrorCodes.ERROR_UNKNOWN;
        switch (error) {
            case INVALID_HTTP_RESPONSE:
                errorCodeToReturn = BandwidthTestErrorCodes.ERROR_INVALID_HTTP_RESPONSE;
                break;
            case SOCKET_ERROR:
                errorCodeToReturn = BandwidthTestErrorCodes.ERROR_SOCKET_ERROR;
                break;
            case SOCKET_TIMEOUT:
                errorCodeToReturn = BandwidthTestErrorCodes.ERROR_SOCKET_TIMEOUT;
                break;
            case CONNECTION_ERROR:
                errorCodeToReturn = BandwidthTestErrorCodes.ERROR_CONNECTION_ERROR;
                break;
        }
        return errorCodeToReturn;
    }
}