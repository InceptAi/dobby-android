package com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.speedtest;

import android.support.annotation.Nullable;

import com.inceptai.dobby.model.BandwidthStats;
import com.inceptai.dobby.speedtest.BandwidthTestCodes.TestMode;
import com.inceptai.dobby.utils.DobbyLog;

import java.util.concurrent.ExecutorService;

import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.model.SpeedTestError;


/**
 * Created by vivek on 4/2/17.
 */

public class NewUploadAnalyzer {
    private final String DEFAULT_UPLOAD_URI = new String("/speedtest/upload.php");
    private final int DEFAULT_UPLOAD_FILE_SIZE = 100000; // ~10Mbyte, in bytes
    //private final int[] ALL_UPLOAD_SIZES = {32768, 65536, 131072, 262144, 524288, 1048576, 7340032}; //TODO: Add more sizes as per speedtest-cli


    private SpeedTestConfig.UploadConfig uploadConfig;
    private String serverUrlPrefix;
    private String uploadUri;
    private BandwidthAggregator bandwidthAggregator;

    //Results callback
    private ResultsCallback resultsCallback;

    /**
     * Callback interface for results. More methods to follow.
     */
    public interface ResultsCallback {
        void onFinish(@TestMode int testMode, BandwidthStats bandwidthStats);
        void onProgress(@TestMode int testMode, double instantBandwidth);
        void onError(@TestMode int testMode, SpeedTestError speedTestError, String errorMessage);
    }

    public NewUploadAnalyzer(SpeedTestConfig.UploadConfig config,
                             ServerInformation.ServerDetails serverDetails,
                             @Nullable ResultsCallback resultsCallback) {
        this.uploadConfig = config;
        this.serverUrlPrefix = getServerUrlForUploadTest(serverDetails.url);
        this.resultsCallback = resultsCallback;
        this.uploadUri = getUriForFileUpload(serverDetails.url);
    }

    /**
     * Factory constructor to create an instance
     * @param config Download config
     * @param serverDetails Best server info
     * @param resultsCallback callback for download results
     * @return
     */
    @Nullable
    public static NewUploadAnalyzer create(SpeedTestConfig.UploadConfig config,
                                           ServerInformation.ServerDetails serverDetails,
                                           ResultsCallback resultsCallback) {
        if (serverDetails.serverId > 0) {
            return new NewUploadAnalyzer(config, serverDetails, resultsCallback);
        }
        return null;
    }


    public String getServerUrlForUploadTest(String urlString) {
        final String suffixToRemove = DEFAULT_UPLOAD_URI;
        final String prefixToRemove = new String("http://");
        String prefixToReturn = null;
        if (urlString != null) {
            if (urlString.endsWith(suffixToRemove)) {
                urlString = urlString.substring(0, urlString.length() - suffixToRemove.length());
            }
            if (urlString.startsWith(prefixToRemove)) {
                urlString = urlString.substring(prefixToRemove.length());
            }
            prefixToReturn = urlString;
        }
        return prefixToReturn;
    }

    public String getUriForFileUpload(String urlString) {
        String uriToReturn = null;
        final String defaultUri = DEFAULT_UPLOAD_URI;
        if (urlString != null) {
            if (urlString.endsWith(defaultUri)) {
                uriToReturn = defaultUri;
            }
        }
        return uriToReturn;
    }


    void uploadTestWithMultipleThreads(int numThreads, int reportIntervalMs) {
        if (uploadConfig == null) {
            DobbyLog.e("Upload config is null while starting upload test. Exiting");
            return;
        }
        if (bandwidthAggregator != null) {
            DobbyLog.e("Upload is already in progress. Cancel it first before running again");
            return;
        }
        bandwidthAggregator = new BandwidthAggregator(new UploadTestListener());
        int threadsToRun = Math.min(numThreads, uploadConfig.threads);
        for (int threadCountIndex = 0; threadCountIndex < threadsToRun; threadCountIndex++) {
            SpeedTestSocket speedTestSocket = this.bandwidthAggregator.getSpeedTestSocket(threadCountIndex);
            speedTestSocket.startUploadRepeat(serverUrlPrefix, uploadUri,
                    uploadConfig.testLength * 1000, //converting to ms
                    reportIntervalMs, DEFAULT_UPLOAD_FILE_SIZE,
                    bandwidthAggregator.getListener(threadCountIndex));
            DobbyLog.v("Started upload speedtest with socket: " + speedTestSocket.toString());

        }
    }

    void cancelAllTests(ExecutorService executorService) {
        if (bandwidthAggregator != null) {
            bandwidthAggregator.cancelTestsAndCleanupAsync(executorService);
        }
        bandwidthAggregator = null;
    }

    private void cleanup() {
        if (bandwidthAggregator != null) {
            bandwidthAggregator.cleanUp();
        }
        bandwidthAggregator = null;
    }

    private class UploadTestListener implements BandwidthAggregator.ResultsCallback {
        @Override
        public void onFinish(BandwidthStats stats) {
            if (resultsCallback != null) {
                resultsCallback.onFinish(BandwidthTestCodes.TestMode.UPLOAD, stats);
            }
            cleanup();
        }

        @Override
        public void onError(SpeedTestError speedTestError, String errorMessage) {
            if (resultsCallback != null) {
                resultsCallback.onError(TestMode.UPLOAD, speedTestError, errorMessage);
            }
            cleanup();
        }

        @Override
        public void onProgress(double instantBandwidth) {
            if (resultsCallback != null) {
                resultsCallback.onProgress(TestMode.UPLOAD, instantBandwidth);
            }
        }
    }
}
