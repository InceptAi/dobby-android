package com.inceptai.dobby.speedtest;

import android.support.annotation.Nullable;

import com.inceptai.dobby.model.BandwidthStats;
import com.inceptai.dobby.speedtest.BandwithTestCodes.BandwidthTestMode;

import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.model.SpeedTestError;

/**
 * Created by vivek on 4/1/17.
 */

public class NewDownloadAnalyzer {
    private final int DOWNLOAD_SIZES = 4000; //TODO: Add more sizes as per speedtest-cli
    private final String DOWNLOAD_FILE = "/speedtest/random" + DOWNLOAD_SIZES + "x" + DOWNLOAD_SIZES + ".jpg";


    private SpeedTestConfig.DownloadConfig downloadConfig;
    private ServerInformation.ServerDetails serverDetails;
    private String serverUrlPrefix;
    private BandwidthAggregator bandwidthAggregator;

    //Results callback
    private ResultsCallback resultsCallback;
    private DownloadTestListener downloadTestListener;

    /**
     * Callback interface for results. More methods to follow.
     */
    public interface ResultsCallback {
        void onFinish(@BandwidthTestMode int testMode, BandwidthStats bandwidthStats);
        void onProgress(@BandwidthTestMode int testMode, double instantBandwidth);
        void onError(@BandwidthTestMode int testMode, SpeedTestError speedTestError, String errorMessage);
    }

    public NewDownloadAnalyzer(SpeedTestConfig.DownloadConfig config,
                               ServerInformation.ServerDetails serverDetails,
                               @Nullable ResultsCallback resultsCallback) {
        this.downloadConfig = config;
        this.serverDetails = serverDetails;
        this.serverUrlPrefix = getServerUrlForSpeedTest(serverDetails.url);
        this.resultsCallback = resultsCallback;
        this.downloadTestListener = new DownloadTestListener();
        this.bandwidthAggregator = new BandwidthAggregator(this.downloadTestListener);
    }

    /**
     * Factory constructor to create an instance
     * @param config Download config
     * @param serverDetails Best server info
     * @param resultsCallback callback for download results
     * @return
     */
    @Nullable
    public static NewDownloadAnalyzer create(SpeedTestConfig.DownloadConfig config,
                                             ServerInformation.ServerDetails serverDetails,
                                             ResultsCallback resultsCallback) {
        if (serverDetails.serverId > 0) {
            return new NewDownloadAnalyzer(config, serverDetails, resultsCallback);
        }
        return null;
    }



    public String getServerUrlForSpeedTest(String urlString) {
        final String suffixToRemove = new String("/speedtest/upload.php");
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


    public void downloadTestWithMultipleThreads(int numThreads, int reportIntervalMs) {
        int threadsToRun = Math.min(numThreads, downloadConfig.threadsPerUrl);
        for (int threadCountIndex = 0; threadCountIndex < threadsToRun; threadCountIndex++) {
            SpeedTestSocket speedTestSocket = this.bandwidthAggregator.getSpeedTestSocket(threadCountIndex);
            speedTestSocket.startDownloadRepeat(serverUrlPrefix, DOWNLOAD_FILE,
                    downloadConfig.testLength * 1000, //converting to ms,
                    reportIntervalMs,
                    this.bandwidthAggregator.getListener(threadCountIndex));
        }
    }

    public boolean cancelAllTests() {
        return this.bandwidthAggregator.cancelActiveSockets();
    }

    private class DownloadTestListener implements BandwidthAggregator.ResultsCallback {
        @Override
        public void onFinish(BandwidthStats stats) {
            if (resultsCallback != null) {
                resultsCallback.onFinish(BandwidthTestMode.DOWNLOAD, stats);
            }
        }

        @Override
        public void onError(SpeedTestError speedTestError, String errorMessage) {
            if (resultsCallback != null) {
                resultsCallback.onError(BandwidthTestMode.DOWNLOAD, speedTestError, errorMessage);
            }
        }

        @Override
        public void onProgress(double instantBandwidth) {
            if (resultsCallback != null) {
                resultsCallback.onProgress(BandwidthTestMode.DOWNLOAD, instantBandwidth);
            }
        }
    }
}
