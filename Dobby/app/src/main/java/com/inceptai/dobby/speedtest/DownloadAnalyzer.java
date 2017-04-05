package com.inceptai.dobby.speedtest;

import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.IRepeatListener;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;

/**
 * Created by vivek on 4/1/17.
 */

public class DownloadAnalyzer {
    private final int REPORT_INTERVAL = 1000; //in milliseconds
    private final int[] DOWNLOAD_SIZES = {4000}; //TODO: Add more sizes as per speedtest-cli

    private SpeedTestConfig.DownloadConfig downloadConfig;
    private ServerInformation.ServerDetails serverDetails;
    private String serverUrlPrefix;
    private List<String> fileListToDownload;
    private List<SpeedTestSocket> speedTestSocketList;


    //Results callback
    private ResultsCallback resultsCallback;
    private DownloadTestListener downloadTestListener;

    /**
     * Callback interface for results. More methods to follow.
     */
    public interface ResultsCallback {
        void onDownloadFinished(SpeedTestReport report);
        void onDownloadProgress(float percent, SpeedTestReport report);
        void onDownloadError(SpeedTestError speedTestError, String errorMessage);
        void onDownloadRepeatIntervalReport(SpeedTestReport report);
        void onRepeatDownloadFinish(SpeedTestReport report);
    }

    public DownloadAnalyzer(SpeedTestConfig.DownloadConfig config,
                            ServerInformation.ServerDetails serverDetails,
                            @Nullable ResultsCallback resultsCallback) {
        this.downloadConfig = config;
        this.serverDetails = serverDetails;
        this.fileListToDownload = new ArrayList<String>();
        this.speedTestSocketList = new ArrayList<SpeedTestSocket>();
        this.serverUrlPrefix = getServerUrlForSpeedTest(serverDetails.url);
        this.resultsCallback = resultsCallback;
        this.downloadTestListener = new DownloadTestListener();
        enqueueDownloadUrls();
        initializeSpeedTestTasks();
        registerDownloadListener(this.downloadTestListener);
    }

    /**
     * Factory constructor to create an instance
     * @param config Download config
     * @param serverDetails Best server info
     * @param resultsCallback callback for download results
     * @return
     */
    @Nullable
    public static DownloadAnalyzer create(SpeedTestConfig.DownloadConfig config,
                                          ServerInformation.ServerDetails serverDetails,
                                          ResultsCallback resultsCallback) {
        if (serverDetails.serverId > 0) {
            return new DownloadAnalyzer(config, serverDetails, resultsCallback);
        }
        return null;
    }



    public String getServerUrlForSpeedTest(String urlString) {
        final String suffixToRemove = new String("/speedtest/upload.php");
        final String prefixToRemove = new String("http://");
        String prefixToReturn = null;
        if (urlString != null) {
            //File fileWithUrlPath = new File(urlString);
            //prefixToReturn = fileWithUrlPath.getParent();
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

    public void enqueueDownloadUrls() {
        for (int size: DOWNLOAD_SIZES) {
            for (int threadCountIndex = 0; threadCountIndex < downloadConfig.threadsPerUrl; threadCountIndex++) {
                String fileToDownload = "/speedtest/random" + size + "x" + size + ".jpg";
                this.fileListToDownload.add(fileToDownload);
            }
        }
    }

    public void initializeSpeedTestTasks() {
        for (int threadCountIndex=0; threadCountIndex < downloadConfig.threadsPerUrl; threadCountIndex++) {
            SpeedTestSocket speedTestSocket = new SpeedTestSocket();
            this.speedTestSocketList.add(speedTestSocket);
        }
    }

    public void startRepeatDownloadWithFixedLength(SpeedTestSocket speedTestSocket, String serverUrlPrefix,
                                                   String fileName, int fixedDownloadLength,
                                                   int reportInterval, DownloadTestListener listener) {
        speedTestSocket.addSpeedTestListener(listener);
        speedTestSocket.startDownloadRepeat(serverUrlPrefix, fileName,
                fixedDownloadLength, reportInterval,  listener);
        //speedTestSocket.startDownload("sf-speedtest.race.com", "/speedtest/random4000x4000.jpg");
        //speedTestSocket.startDownload(serverUrl, fileName);
    }


    public void downloadTestWithOneThread() {
        final int size = 4000; // in bytes
        final int reportInterval = 1000; //in milliseconds
        final int testLengthOneThread = 4 * downloadConfig.testLength * 1000; //in milliseconds
        String fileToDownload = "/speedtest/random" + size + "x" + size + ".jpg";
        //SpeedTestSocket speedTestSocket = new SpeedTestSocket();
        startRepeatDownloadWithFixedLength(this.speedTestSocketList.get(0), this.serverUrlPrefix,
                fileToDownload, testLengthOneThread, //converting to ms
                this.REPORT_INTERVAL, this.downloadTestListener);
    }

    public void startDownload() {
        //Start timed download with 4 threads and count all the bytes transferred and time taken
        downloadTestWithOneThread();
    }

    /**
     * register listener for download/upload stats
     *
     * @param downloadTestListener listener to add
     */
    private void registerDownloadListener(@Nullable DownloadTestListener downloadTestListener) {
        DownloadTestListener newListener = null;
        if (downloadTestListener != null) {
            newListener = downloadTestListener;
        } else {
            newListener = new DownloadTestListener();
        }
        for (SpeedTestSocket speedTestSocket: speedTestSocketList) {
            speedTestSocket.addSpeedTestListener(newListener);
        }
    }

    private class DownloadTestListener implements ISpeedTestListener, IRepeatListener {

        /**
         * monitor download process result with transfer rate in bit/s and octet/s.
         *
         * @param report download speed test report
         */
        @Override
        public void onDownloadFinished(SpeedTestReport report) {
            // called when download is finished
            Log.v("speedtest", "[DL FINISHED] rate in bit/s   : " + report.getTransferRateBit());
            if (resultsCallback != null) {
                resultsCallback.onDownloadFinished(report);
            }
        }

        @Override
        public void onDownloadProgress(float percent, SpeedTestReport report) {
            // called to notify download progress
            //Log.v("speedtest", "[DL PROGRESS] progress : " + percent + "%");
            //Log.v("speedtest", "[DL PROGRESS] rate in bit/s   : " + report.getTransferRateBit());
            if (resultsCallback != null) {
                resultsCallback.onDownloadProgress(percent, report);
            }
        }

        @Override
        public void onDownloadError(SpeedTestError speedTestError, String errorMessage) {
            Log.v("speedtest", "[DL ERROR] : " + speedTestError.name());
            Log.v("speedtest", "[DL ERROR MESG] : " + errorMessage);
            if (resultsCallback != null) {
                resultsCallback.onDownloadError(speedTestError, errorMessage);
            }
        }

        @Override
        public void onReport(final SpeedTestReport report) {
            // called when a download report is dispatched
            // called to notify download progress
            //Log.v("speedtest", "[DL PROGRESS] rate in bit/s   : " + report.getTransferRateBit());
            if (resultsCallback != null) {
                resultsCallback.onDownloadRepeatIntervalReport(report);
            }
        }

        @Override
        public void onFinish(final SpeedTestReport report) {
            // called when repeat task is finished
            // called to notify download progress
            Log.v("speedtest", "[DL PROGRESS] rate in bit/s   : " + report.getTransferRateBit());
            if (resultsCallback != null) {
                resultsCallback.onRepeatDownloadFinish(report);
            }
        }

        @Override
        public void onUploadFinished(SpeedTestReport report) {
        }

        @Override
        public void onUploadError(SpeedTestError speedTestError, String errorMessage) {
        }

        @Override
        public void onUploadProgress(float percent, SpeedTestReport report) {
        }

        @Override
        public void onInterruption() {
        }


    }
}
