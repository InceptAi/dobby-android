package com.inceptai.dobby.speedtest;

/**
 * Created by vivek on 3/30/17.
 */

import android.support.annotation.Nullable;
import android.util.Log;

import com.inceptai.dobby.speedtest.BandwithTestCodes.BandwidthTestErrorCodes;
import com.inceptai.dobby.speedtest.BandwithTestCodes.BandwidthTestMode;

import java.util.List;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.model.SpeedTestError;

import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * Class contains logic performing bandwidth tests.
 */

public class BandwidthAnalyzer {

    private ParseSpeedTestConfig parseSpeedTestConfig;
    private ParseServerInformation parseServerInformation;
    private SpeedTestConfig speedTestConfig;
    private ServerInformation.ServerDetails bestServer;

    private BandwidthTestListener bandwidthTestListener;

    @BandwidthTestMode
    private int testMode;
    @BandwidthTestMode
    private int currentlyTestingMode;
    // Callbacks
    private ResultsCallback resultsCallback;

    /**
     * Callback interface for results. More methods to follow.
     */
    public interface ResultsCallback {

        //Error callback
        void onBandwidthTestError(@BandwidthTestMode int testMode,
                                  @BandwidthTestErrorCodes int errorCode,
                                  @Nullable  String errorMessage);

        //Config callbacks
        void onConfigFetch(SpeedTestConfig config);
        //Getting nearby servers
        void onServerInformationFetch(ServerInformation serverInformation);
        //Closest servers selected
        void onClosestServersSelected(List<ServerInformation.ServerDetails> closestServers);
        //Best server callbacks
        void onBestServerSelected(ServerInformation.ServerDetails bestServer);

        //Test callbacks
        void onTestFinished(@BandwidthTestMode int testMode, SpeedTestReport report);
        void onTestProgress(@BandwidthTestMode int testMode, float percent, SpeedTestReport report);
        void onTestRepeatIntervalReport(@BandwidthTestMode int testMode, SpeedTestReport report);
        void onRepeatTestFinished(@BandwidthTestMode int testMode, SpeedTestReport report);
    }

    private BandwidthAnalyzer(@Nullable ResultsCallback resultsCallback) {
        this.bandwidthTestListener = new BandwidthTestListener();
        this.resultsCallback = resultsCallback;
        this.testMode = BandwidthTestMode.IDLE;
        this.currentlyTestingMode = BandwidthTestMode.IDLE;

        this.parseSpeedTestConfig = new ParseSpeedTestConfig(this.bandwidthTestListener);
        this.parseServerInformation = new ParseServerInformation(this.bandwidthTestListener);
    }

    /**
     * Factory constructor to create an instance
     * @return Instance of BandwidthAnalyzer or null on error.
     */
    @Nullable
    public static BandwidthAnalyzer create(ResultsCallback resultsCallback) {
        return new BandwidthAnalyzer(resultsCallback);
    }

    private ServerInformation.ServerDetails getBestServer(SpeedTestConfig config,
                                                          ServerInformation info) {
        BestServerSelector serverSelector = new BestServerSelector(config, info, null);
        return serverSelector.getBestServer();
    }

    private void performDownload() {
        currentlyTestingMode = BandwidthTestMode.DOWNLOAD;
        DownloadAnalyzer downloadAnalyzer = new DownloadAnalyzer(speedTestConfig.downloadConfig, bestServer, bandwidthTestListener);
        downloadAnalyzer.downloadTestWithOneThread();
    }

    private void performUpload() {
        currentlyTestingMode = BandwidthTestMode.UPLOAD;
        UploadAnalyzer uploadAnalyzer = new UploadAnalyzer(speedTestConfig.uploadConfig, bestServer, bandwidthTestListener);
        uploadAnalyzer.uploadTestWithOneThread();
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
            ParseServerInformation.ResultsCallback, DownloadAnalyzer.ResultsCallback,
            UploadAnalyzer.ResultsCallback {

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

        /**
         * monitor download process result with transfer rate in bit/s and octet/s.
         *
         * @param report download speed test report
         */

        //Download callbacks
        public void onDownloadFinished(SpeedTestReport report) {
            if (resultsCallback != null) {
                resultsCallback.onTestFinished(BandwidthTestMode.DOWNLOAD, report);
            }
        }

        public void onDownloadProgress(float percent, SpeedTestReport report) {
            // called to notify download progress
            Log.v("speedtest", "[DL PROGRESS] progress : " + percent + "%");
            Log.v("speedtest", "[DL PROGRESS] rate in bit/s   : " + report.getTransferRateBit());
            if (resultsCallback != null) {
                resultsCallback.onTestProgress(BandwidthTestMode.DOWNLOAD, percent, report);
            }
        }

        public void onDownloadError(SpeedTestError speedTestError, String errorMessage) {
            Log.v("speedtest", "[DL ERROR] : " + speedTestError.name());
            Log.v("speedtest", "[DL ERROR MESG] : " + errorMessage);
            if (resultsCallback != null) {
                resultsCallback.onBandwidthTestError(BandwidthTestMode.DOWNLOAD,
                        convertToBandwidthTestCodes(speedTestError), errorMessage);
            }
        }

        public void onDownloadRepeatIntervalReport(final SpeedTestReport report) {
            // called when a download report is dispatched
            // called to notify download progress
            //Log.v("speedtest", "[DL PROGRESS] rate in bit/s   : " + report.getTransferRateBit());
            if (resultsCallback != null) {
                resultsCallback.onTestRepeatIntervalReport(BandwidthTestMode.DOWNLOAD, report);
            }
        }

        public void onRepeatDownloadFinish(final SpeedTestReport report) {
            // called when repeat task is finished
            // called to notify download progress
            Log.v("speedtest", "[DL PROGRESS] rate in bit/s   : " + report.getTransferRateBit());
            if (resultsCallback != null) {
                resultsCallback.onRepeatTestFinished(BandwidthTestMode.DOWNLOAD, report);
            }
            //Do we need to do upload here ?
            if (testMode == BandwidthTestMode.DOWNLOAD_AND_UPLOAD) {
                performUpload();
            } else {
                //Cleanup
                finishTests();
            }
        }


        //Upload callbacks
        public void onUploadFinished(SpeedTestReport report) {
            // called when an upload is finished
            Log.v("speedtest", "[UL FINISHED] rate in bit/s   : " + report.getTransferRateBit());
            if (resultsCallback != null) {
                resultsCallback.onTestFinished(BandwidthTestMode.UPLOAD, report);
            }
        }

        public void onUploadError(SpeedTestError speedTestError, String errorMessage) {
            Log.v("speedtest", "[UL ERROR] : " + speedTestError.name());
            Log.v("speedtest", "[UL ERROR] mesg : " + errorMessage);
            if (resultsCallback != null) {
                resultsCallback.onBandwidthTestError(BandwidthTestMode.UPLOAD,
                        convertToBandwidthTestCodes(speedTestError), errorMessage);
            }
        }

        public void onUploadProgress(float percent, SpeedTestReport report) {
            // called to notify upload progress
            Log.v("speedtest", "[UL PROGRESS] progress : " + percent + "%");
            Log.v("speedtest", "[UL PROGRESS] rate in bit/s   : " + report.getTransferRateBit());
            if (resultsCallback != null) {
                resultsCallback.onTestProgress(BandwidthTestMode.UPLOAD, percent, report);
            }
        }

        public void onUploadRepeatIntervalReport(final SpeedTestReport report) {
            if (resultsCallback != null) {
                resultsCallback.onTestRepeatIntervalReport(BandwidthTestMode.UPLOAD, report);
            }
        }

        public void onRepeatUploadFinish(final SpeedTestReport report) {
            // called when repeat task is finished
            // called to notify upload progress
            Log.v("speedtest", "[UL PROGRESS] rate in bit/s   : " + report.getTransferRateBit());
            if (resultsCallback != null) {
                resultsCallback.onRepeatTestFinished(BandwidthTestMode.UPLOAD, report);
            }
            //Done with all the tests
            //Cleanup
            finishTests();
        }
        public void onInterruption() {
        }

    }
}