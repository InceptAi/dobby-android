package com.inceptai.dobby.speedtest;

import android.support.annotation.Nullable;

import com.inceptai.dobby.speedtest.BandwithTestCodes.BandwidthTestMode;

import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.model.SpeedTestError;


/**
 * Created by vivek on 4/2/17.
 */

public class NewUploadAnalyzer {
    private final String DEFAULT_UPLOAD_URI = new String("/speedtest/upload.php");
    private final int REPORT_INTERVAL_MS = 1000; //in milliseconds
    private final int DEFAULT_UPLOAD_FILE_SIZE = 100000; // ~10Mbyte, in bytes
    //private final int[] ALL_UPLOAD_SIZES = {32768, 65536, 131072, 262144, 524288, 1048576, 7340032}; //TODO: Add more sizes as per speedtest-cli


    private SpeedTestConfig.UploadConfig uploadConfig;
    private String serverUrlPrefix;
    private String uploadUri;
    private BandwidthAggregator bandwidthAggregator;

    //Results callback
    private ResultsCallback resultsCallback;
    private UploadTestListener uploadTestListener;

    /**
     * Callback interface for results. More methods to follow.
     */
    public interface ResultsCallback {
        void onFinish(@BandwidthTestMode int testMode, BandwidthAggregator.BandwidthStats bandwidthStats);
        void onProgress(@BandwidthTestMode int testMode, double instantBandwidth);
        void onError(@BandwidthTestMode int testMode, SpeedTestError speedTestError, String errorMessage);
    }

    public NewUploadAnalyzer(SpeedTestConfig.UploadConfig config,
                             ServerInformation.ServerDetails serverDetails,
                             @Nullable ResultsCallback resultsCallback) {
        this.uploadConfig = config;
        this.serverUrlPrefix = getServerUrlForUploadTest(serverDetails.url);
        this.resultsCallback = resultsCallback;
        this.uploadUri = getUriForFileUpload(serverDetails.url);
        this.uploadTestListener = new UploadTestListener();
        this.bandwidthAggregator = new BandwidthAggregator(this.uploadTestListener);
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


    public void uploadTestWithMultipleThreads(int numThreads) {
        int threadsToRun = Math.min(numThreads, uploadConfig.threads);
        for (int threadCountIndex = 0; threadCountIndex < threadsToRun; threadCountIndex++) {
            SpeedTestSocket speedTestSocket = this.bandwidthAggregator.getSpeedTestSocket(threadCountIndex);
            speedTestSocket.startUploadRepeat(serverUrlPrefix, uploadUri,
                    uploadConfig.testLength * 1000, //converting to ms
                    REPORT_INTERVAL_MS, DEFAULT_UPLOAD_FILE_SIZE,
                    bandwidthAggregator.getListener(threadCountIndex));
        }
    }

    private class UploadTestListener implements BandwidthAggregator.ResultsCallback {
        @Override
        public void onFinish(BandwidthAggregator.BandwidthStats stats) {
            if (resultsCallback != null) {
                resultsCallback.onFinish(BandwidthTestMode.UPLOAD, stats);
            }
        }

        @Override
        public void onError(SpeedTestError speedTestError, String errorMessage) {
            if (resultsCallback != null) {
                resultsCallback.onError(BandwidthTestMode.UPLOAD, speedTestError, errorMessage);
            }
        }

        @Override
        public void onProgress(double instantBandwidth) {
            if (resultsCallback != null) {
                resultsCallback.onProgress(BandwidthTestMode.UPLOAD, instantBandwidth);
            }
        }
    }
}
