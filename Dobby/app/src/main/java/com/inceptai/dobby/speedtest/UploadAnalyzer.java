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
 * Created by vivek on 4/2/17.
 */

public class UploadAnalyzer {
    private final String DEFAULT_UPLOAD_URI = new String("/speedtest/upload.php");
    private final int REPORT_INTERVAL = 1000; //in milliseconds
    private final int[] ALL_UPLOAD_SIZES = {32768, 65536, 131072, 262144, 524288, 1048576, 7340032}; //TODO: Add more sizes as per speedtest-cli

    private SpeedTestConfig.UploadConfig uploadConfig;
    private ServerInformation.ServerDetails serverDetails;
    private String serverUrlPrefix;
    private String uploadUri;
    private int[] filesSizesToBeUploaded;
    private List<SpeedTestSocket> speedTestSocketList;

    //Results callback
    private ResultsCallback resultsCallback;
    private UploadTestListener uploadTestListener;

    /**
     * Callback interface for results. More methods to follow.
     */
    public interface ResultsCallback {
        void onUploadFinished(SpeedTestReport report);
        void onUploadProgress(float percent, SpeedTestReport report);
        void onUploadError(SpeedTestError speedTestError, String errorMessage);
        void onUploadRepeatIntervalReport(SpeedTestReport report);
        void onRepeatUploadFinish(SpeedTestReport report);
    }

    public UploadAnalyzer(SpeedTestConfig.UploadConfig config,
                          ServerInformation.ServerDetails serverDetails,
                          @Nullable ResultsCallback resultsCallback) {
        this.uploadConfig = config;
        this.serverDetails = serverDetails;
        this.filesSizesToBeUploaded = new int[this.uploadConfig.maxChunkCount];
        this.speedTestSocketList = new ArrayList<SpeedTestSocket>();
        this.serverUrlPrefix = getServerUrlForUploadTest(serverDetails.url);
        this.resultsCallback = resultsCallback;
        this.uploadUri = getUriForFileUpload(serverDetails.url);
        enqueueUploadUrls();
        initializeSpeedTestTasks();
        this.uploadTestListener = new UploadTestListener();
        registerUploadListener(this.uploadTestListener);
    }

    /**
     * Factory constructor to create an instance
     * @param config Download config
     * @param serverDetails Best server info
     * @param resultsCallback callback for download results
     * @return
     */
    @Nullable
    public static UploadAnalyzer create(SpeedTestConfig.UploadConfig config,
                                          ServerInformation.ServerDetails serverDetails,
                                          ResultsCallback resultsCallback) {
        if (serverDetails.serverId > 0) {
            return new UploadAnalyzer(config, serverDetails, resultsCallback);
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

    public void enqueueUploadUrls() {
        int sizeIndex = 0;
        int uploadSizeStartingIndex = uploadConfig.ratio - 1;
        for (int uploadSizeIndex = uploadSizeStartingIndex; uploadSizeIndex < ALL_UPLOAD_SIZES.length; uploadSizeIndex++) {
            for (int threadCountIndex = 0; threadCountIndex < uploadConfig.threads; threadCountIndex++) {
                this.filesSizesToBeUploaded[sizeIndex++] = ALL_UPLOAD_SIZES[uploadSizeIndex];
                if (sizeIndex >= this.uploadConfig.maxChunkCount) {
                    return;
                }
            }
        }
    }

    public void initializeSpeedTestTasks() {
        for (int threadCountIndex = 0; threadCountIndex < uploadConfig.threads; threadCountIndex++) {
            SpeedTestSocket speedTestSocket = new SpeedTestSocket();
            this.speedTestSocketList.add(speedTestSocket);
        }
    }

    public void startRepeatUploadWithFixedLength(SpeedTestSocket speedTestSocket, String serverUrlPrefix,
                                                 String uploadFileUri, int fixedDownloadLength, int reportInterval,
                                                 int fileSize, UploadTestListener listener) {
        speedTestSocket.addSpeedTestListener(listener);
        speedTestSocket.startUploadRepeat(serverUrlPrefix, uploadFileUri,
                fixedDownloadLength, reportInterval, fileSize, listener);
    }


    public void uploadTestWithOneThread() {
        final int uploadFileSize = 100000; // ~10Mbyte, in bytes
        final int testLengthOneThread = uploadConfig.testLength * 1000; //in milliseconds
        startRepeatUploadWithFixedLength(this.speedTestSocketList.get(0), this.serverUrlPrefix,
                this.uploadUri, testLengthOneThread, //converting to ms
                this.REPORT_INTERVAL, uploadFileSize, this.uploadTestListener);
    }

    public void startUpload() {
        //Start timed download with 4 threads and count all the bytes transferred and time taken
        uploadTestWithOneThread();
    }



    /**
     * register listener for download/upload stats
     *
     * @param uploadTestListener listener to add
     */
    private void registerUploadListener(@Nullable UploadTestListener uploadTestListener) {
        UploadTestListener newListener = null;
        if (uploadTestListener != null) {
            newListener = uploadTestListener;
        } else {
            newListener = new UploadTestListener();
        }
        for (SpeedTestSocket speedTestSocket: speedTestSocketList) {
            speedTestSocket.addSpeedTestListener(newListener);
        }
    }

    private class UploadTestListener implements ISpeedTestListener, IRepeatListener {

        /**
         * monitor download process result with transfer rate in bit/s and octet/s.
         *
         * @param report download speed test report
         */
        @Override
        public void onUploadFinished(SpeedTestReport report) {
            // called when download is finished
            Log.v("speedtest", "[UL FINISHED] rate in bit/s   : " + report.getTransferRateBit());
            if (resultsCallback != null) {
                resultsCallback.onUploadFinished(report);
            }
        }

        @Override
        public void onUploadProgress(float percent, SpeedTestReport report) {
            // called to notify upload progress
            //Log.v("speedtest", "[UL PROGRESS] progress : " + percent + "%");
            //Log.v("speedtest", "[UL PROGRESS] rate in bit/s   : " + report.getTransferRateBit());
            if (resultsCallback != null) {
                resultsCallback.onUploadProgress(percent, report);
            }
        }

        @Override
        public void onUploadError(SpeedTestError speedTestError, String errorMessage) {
            Log.v("speedtest", "[UL ERROR] : " + speedTestError.name());
            Log.v("speedtest", "[UL ERROR MESG] : " + errorMessage);
            if (resultsCallback != null) {
                resultsCallback.onUploadError(speedTestError, errorMessage);
            }
        }

        @Override
        public void onReport(final SpeedTestReport report) {
            if (resultsCallback != null) {
                resultsCallback.onUploadRepeatIntervalReport(report);
            }
        }

        @Override
        public void onFinish(final SpeedTestReport report) {
            // called when repeat task is finished
            // called to notify upload progress
            Log.v("speedtest", "[UL PROGRESS] rate in bit/s   : " + report.getTransferRateBit());
            if (resultsCallback != null) {
                resultsCallback.onRepeatUploadFinish(report);
            }
        }

        @Override
        public void onDownloadFinished(SpeedTestReport report) {
        }

        @Override
        public void onDownloadError(SpeedTestError speedTestError, String errorMessage) {
        }

        @Override
        public void onDownloadProgress(float percent, SpeedTestReport report) {
        }

        @Override
        public void onInterruption() {
        }

    }

}
