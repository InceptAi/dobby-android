package com.inceptai.dobby.speedtest;

/**
 * Created by vivek on 3/30/17.
 */

import android.support.annotation.Nullable;
import android.util.Log;

import com.inceptai.dobby.model.BandwidthStats;
import com.inceptai.dobby.speedtest.BandwithTestCodes.BandwidthTestErrorCodes;
import com.inceptai.dobby.speedtest.BandwithTestCodes.BandwidthTestMode;

import java.util.List;

import fr.bmartel.speedtest.model.SpeedTestError;

import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * Class contains logic performing bandwidth tests.
 */

public class NewBandwidthAnalyzer {
    private static final int DOWNLOAD_THREADS = 1;
    private static final int UPLOAD_THREADS = 1;

    private ParseSpeedTestConfig parseSpeedTestConfig;
    private ParseServerInformation parseServerInformation;
    private SpeedTestConfig speedTestConfig;
    private ServerInformation.ServerDetails bestServer;
    private NewDownloadAnalyzer downloadAnalyzer;
    private NewUploadAnalyzer uploadAnalyzer;

    private BandwidthTestListener bandwidthTestListener;

    @BandwidthTestMode
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
        void onTestFinished(@BandwidthTestMode int testMode, BandwidthStats stats);
        void onTestProgress(@BandwidthTestMode int testMode, double instantBandwidth);

        //Error callback
        void onBandwidthTestError(@BandwidthTestMode int testMode,
                                  @BandwidthTestErrorCodes int errorCode,
                                  @Nullable String errorMessage);
    }

    private NewBandwidthAnalyzer(@Nullable ResultsCallback resultsCallback) {
        this.bandwidthTestListener = new BandwidthTestListener();
        this.resultsCallback = resultsCallback;
        this.testMode = BandwidthTestMode.IDLE;
        this.parseSpeedTestConfig = new ParseSpeedTestConfig(this.bandwidthTestListener);
        this.parseServerInformation = new ParseServerInformation(this.bandwidthTestListener);
    }

    /**
     * Factory constructor to create an instance
     * @return Instance of BandwidthAnalyzer or null on error.
     */
    @Nullable
    public static NewBandwidthAnalyzer create(ResultsCallback resultsCallback) {
        return new NewBandwidthAnalyzer(resultsCallback);
    }

    private ServerInformation.ServerDetails getBestServer(SpeedTestConfig config,
                                                          ServerInformation info) {
        BestServerSelector serverSelector = new BestServerSelector(config, info, null);
        return serverSelector.getBestServer();
    }

    private void  performDownload() {
        downloadAnalyzer = new NewDownloadAnalyzer(speedTestConfig.downloadConfig,
                bestServer, bandwidthTestListener);
        downloadAnalyzer.downloadTestWithMultipleThreads(DOWNLOAD_THREADS);
    }

    private void performUpload() {
        uploadAnalyzer = new NewUploadAnalyzer(speedTestConfig.uploadConfig,
                bestServer, bandwidthTestListener);
        uploadAnalyzer.uploadTestWithMultipleThreads(UPLOAD_THREADS);
    }


    private void finishTests() {
        this.speedTestConfig = null;
        this.bestServer = null;
    }

    /**
     * start the speed test
     */
    public void startBandwidthTest(@BandwidthTestMode int testMode) {
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
            return;
        }
        ServerInformation info = parseServerInformation.getServerInfo();
        if (info == null) {
            if (resultsCallback != null) {
                resultsCallback.onBandwidthTestError(BandwidthTestMode.SERVER_FETCH,
                        BandwidthTestErrorCodes.ERROR_FETCHING_SERVER_INFO,
                        "Server info fetch returned null");
            }
            return;
        }

        bestServer = getBestServer(speedTestConfig, info);
        if (bestServer == null) {
            if (resultsCallback != null) {
                resultsCallback.onBandwidthTestError(BandwidthTestMode.SERVER_FETCH,
                        BandwidthTestErrorCodes.ERROR_SELECTING_BEST_SERVER,
                        "best server returned as null");
            }
            return;
        }

        if (testMode == BandwidthTestMode.DOWNLOAD_AND_UPLOAD || testMode == BandwidthTestMode.DOWNLOAD) {
            performDownload();
        }
        if (testMode == BandwidthTestMode.UPLOAD) {
            performUpload();
        }
    }

    public void cancelBandwidthTests() {
        downloadAnalyzer.cancelAllTests();
        uploadAnalyzer.cancelAllTests();
        finishTests();
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
                        error);            }
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
                performUpload();
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
}
