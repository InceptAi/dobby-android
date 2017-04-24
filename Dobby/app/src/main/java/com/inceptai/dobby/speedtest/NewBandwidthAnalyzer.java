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
import com.inceptai.dobby.speedtest.BandwithTestCodes.ErrorCodes;
import com.inceptai.dobby.speedtest.BandwithTestCodes.TestMode;

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
    private static final int DOWNLOAD_THREADS = 1;
    private static final int UPLOAD_THREADS = 1;
    private static final int REPORT_INTERVAL_MS = 250;
    private static final int MAX_AGE_FOR_FRESHNESS_MS = 300000; // 5 mins

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
    private ServerInformation serverInformation;
    private ServerInformation.ServerDetails bestServer;
    private NewDownloadAnalyzer downloadAnalyzer;
    private NewUploadAnalyzer uploadAnalyzer;
    private DobbyThreadpool dobbyThreadpool;
    private DobbyEventBus eventBus;
    private long lastConfigFetchTimestampMs;
    private long lastBestServerDeterminationTimestampMs;

    private BandwidthTestListener bandwidthTestListener;

    @BandwithTestCodes.TestMode
    private int testMode;
    //Callbacks
    private ResultsCallback resultsCallback;

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
        void onTestFinished(@BandwithTestCodes.TestMode int testMode, BandwidthStats stats);
        void onTestProgress(@BandwithTestCodes.TestMode int testMode, double instantBandwidth);

        //Error callback
        void onBandwidthTestError(@BandwithTestCodes.TestMode int testMode,
                                  @ErrorCodes int errorCode,
                                  @Nullable String errorMessage);
    }

    @Inject
    public NewBandwidthAnalyzer(DobbyThreadpool dobbyThreadpool, DobbyEventBus eventBus) {
        this.bandwidthTestListener = new BandwidthTestListener();
        this.testMode = BandwithTestCodes.TestMode.IDLE;
        this.eventBus = eventBus;
        this.parseSpeedTestConfig = new ParseSpeedTestConfig(this.bandwidthTestListener);
        this.parseServerInformation = new ParseServerInformation(this.bandwidthTestListener);
        this.dobbyThreadpool = dobbyThreadpool;
        lastConfigFetchTimestampMs = 0;
        lastBestServerDeterminationTimestampMs = 0;
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

    public void startBandwidthTestSync(@TestMode int testMode) {
        try {
            startBandwidthTest(testMode);
        } catch(Exception e) {
            Log.i(TAG, "Exception starting bandwidth test." + e);
        }
    }

    public void cancelBandwidthTests() {
        markTestsAsCancelling();
        downloadAnalyzer.cancelAllTests();
        uploadAnalyzer.cancelAllTests();
        markTestsAsCancelled();
    }

    public void onConnectivityChangeToOnline() {
        fetchSpeedTestConfigIfNeeded();
        fetchServerConfigAndDetermineBestServerIfNeeded();
    }

    synchronized private void fetchSpeedTestConfigIfNeeded() {
        final String downloadMode = "http";
        //Get config if not fresh
        if (System.currentTimeMillis() - lastConfigFetchTimestampMs > MAX_AGE_FOR_FRESHNESS_MS) {
            speedTestConfig = parseSpeedTestConfig.getConfig(downloadMode);
            if (speedTestConfig == null) {
                if (resultsCallback != null) {
                    resultsCallback.onBandwidthTestError(BandwithTestCodes.TestMode.CONFIG_FETCH,
                            ErrorCodes.ERROR_FETCHING_CONFIG,
                            "Config fetch returned null");
                }
                return;
            }
            //Update lastConfigFetch timestamp
            lastConfigFetchTimestampMs = System.currentTimeMillis();
        } else { //we already have a fresh config. Do nothing. Use speedTestConfig.
            //Inform if anyone is listening for this.
            if (resultsCallback != null) {
                resultsCallback.onConfigFetch(speedTestConfig);
            }
        }
    }

    synchronized private void fetchServerConfigAndDetermineBestServerIfNeeded() {
        //Get best server information if stale
        if (System.currentTimeMillis() - lastBestServerDeterminationTimestampMs > MAX_AGE_FOR_FRESHNESS_MS) {
            serverInformation = parseServerInformation.getServerInfo();
            if (serverInformation == null) {
                if (resultsCallback != null) {
                    resultsCallback.onBandwidthTestError(BandwithTestCodes.TestMode.SERVER_FETCH,
                            ErrorCodes.ERROR_FETCHING_SERVER_INFO,
                            "Server info fetch returned null");
                }
                return;
            }

            //Get best server
            bestServer = getBestServer(speedTestConfig, serverInformation);
            if (bestServer == null) {
                if (resultsCallback != null) {
                    resultsCallback.onBandwidthTestError(BandwithTestCodes.TestMode.SERVER_FETCH,
                            ErrorCodes.ERROR_SELECTING_BEST_SERVER,
                            "best server returned as null");
                }
                return;
            }
            lastBestServerDeterminationTimestampMs = System.currentTimeMillis();
        } else { //Use the existing best server for testing.
            if (resultsCallback != null) {
                resultsCallback.onServerInformationFetch(serverInformation);
                resultsCallback.onBestServerSelected(bestServer);
            }
        }
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
                resultsCallback.onBandwidthTestError(BandwithTestCodes.TestMode.CONFIG_FETCH,
                        ErrorCodes.ERROR_FETCHING_CONFIG,
                        error);
            }
        }

        // Server information callbacks
        public void onServerInformationFetch(ServerInformation serverInformation) {
            Log.v(TAG, "Speed test server information fetched, num servers:" + serverInformation.serverList.size());
            if (resultsCallback != null) {
                if (serverInformation != null && serverInformation.serverList.size() > 0) {
                    resultsCallback.onServerInformationFetch(serverInformation);
                } else {
                    resultsCallback.onBandwidthTestError(BandwithTestCodes.TestMode.SERVER_FETCH,
                            ErrorCodes.ERROR_FETCHING_SERVER_INFO,
                            "Server information returned empty");
                }
            }
        }

        public void onServerInformationFetchError(String error) {
            if (resultsCallback != null) {
                resultsCallback.onBandwidthTestError(BandwithTestCodes.TestMode.SERVER_FETCH,
                        ErrorCodes.ERROR_FETCHING_SERVER_INFO,
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
                resultsCallback.onBandwidthTestError(BandwithTestCodes.TestMode.SERVER_FETCH,
                        ErrorCodes.ERROR_SELECTING_BEST_SERVER, error);
        }

        public void onClosestServersSelected(List<ServerInformation.ServerDetails> closestServers) {
            if (resultsCallback != null)
                resultsCallback.onClosestServersSelected(closestServers);
        }

        @Override
        public void onFinish(@BandwithTestCodes.TestMode int callbackTestMode, BandwidthStats bandwidthStats) {
            Log.v(TAG, "NewBandwidthAnalyzer onFinish");
            if (resultsCallback != null) {
                resultsCallback.onTestFinished(callbackTestMode, bandwidthStats);
            }
            //Do we need to do upload here ?
            if (callbackTestMode == BandwithTestCodes.TestMode.DOWNLOAD && testMode == BandwithTestCodes.TestMode.DOWNLOAD_AND_UPLOAD) {
                dobbyThreadpool.submit(new Runnable() {
                    @Override
                    public void run() {
                        performUpload();
                    }
                });
            } else {
                //Cleanup
                markTestsAsStopped();
            }
        }

        @Override
        public void onProgress(@BandwithTestCodes.TestMode int callbackTestMode, double instantBandwidth) {
            Log.v(TAG, "NewBandwidthAnalyzer onProgress");
            if (resultsCallback != null) {
                resultsCallback.onTestProgress(callbackTestMode, instantBandwidth);
            }
        }

        @Override
        public void onError(@BandwithTestCodes.TestMode int callbackTestMode, SpeedTestError speedTestError, String errorMessage) {
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

    private void performDownload() {
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

    private boolean checkTestStatusAndMarkRunningIfInactive() {
        if (!testsCurrentlyInactive()) {
            if (resultsCallback != null) {
                resultsCallback.onBandwidthTestError(TestMode.STARTING,
                        ErrorCodes.ERROR_TEST_ALREADY_RUNNING,
                        "Tests are already running.");
            }
            return false;
        } else {
            bandwidthAnalyzerState = BandwidthAnalyzerState.RUNNING;
            return true;
        }
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

    public ServerInformation.ServerDetails getLastBestServer() {
        return bestServer;
    }

    /**
     * start the speed test
     */
    private void startBandwidthTest(@BandwithTestCodes.TestMode int testMode) {
        if (!checkTestStatusAndMarkRunningIfInactive()) {
            //Tests are already
            return;
        }
        this.testMode = testMode;
        fetchSpeedTestConfigIfNeeded();
        fetchServerConfigAndDetermineBestServerIfNeeded();
        if (testMode == TestMode.DOWNLOAD_AND_UPLOAD || testMode == BandwithTestCodes.TestMode.DOWNLOAD) {
            performDownload();
        }
        if (testMode == BandwithTestCodes.TestMode.UPLOAD) {
            performUpload();
        }
    }

    @ErrorCodes
    private int convertToBandwidthTestCodes(SpeedTestError error) {
        int errorCodeToReturn = ErrorCodes.ERROR_UNKNOWN;
        switch (error) {
            case INVALID_HTTP_RESPONSE:
                errorCodeToReturn = ErrorCodes.ERROR_INVALID_HTTP_RESPONSE;
                break;
            case SOCKET_ERROR:
                errorCodeToReturn = ErrorCodes.ERROR_SOCKET_ERROR;
                break;
            case SOCKET_TIMEOUT:
                errorCodeToReturn = ErrorCodes.ERROR_SOCKET_TIMEOUT;
                break;
            case CONNECTION_ERROR:
                errorCodeToReturn = ErrorCodes.ERROR_CONNECTION_ERROR;
                break;
        }
        return errorCodeToReturn;
    }
}
