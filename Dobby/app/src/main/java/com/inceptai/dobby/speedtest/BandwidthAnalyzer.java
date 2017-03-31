package com.inceptai.dobby.speedtest;

/**
 * Created by vivek on 3/30/17.
 */

import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.base.Preconditions;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;

/**
 * Class contains logic performing bandwidth tests.
 */

public class BandwidthAnalyzer {
    private ResultsCallback resultsCallback;
    private SpeedTestSocket speedTestSocket;

    /**
     * Callback interface for results. More methods to follow.
     */
    public interface ResultsCallback {
        void onDownloadFinished(double speedInBitsPerSec);
        void onDownloadProgress(double percent, double speedInBitsPerSec);
        void onDownloadError(SpeedTestError speedTestError, String errorMessage);

        void onUploadFinished(double speedInBitsPerSec);
        void onUploadProgress(double percent, double speedInBitsPerSec);
        void onUploadError(SpeedTestError speedTestError, String errorMessage);
    }

    private BandwidthAnalyzer(SpeedTestSocket speedTestSocket,
                              @Nullable ResultsCallback resultsCallback) {
        Preconditions.checkNotNull(speedTestSocket);
        this.resultsCallback = resultsCallback;
        this.speedTestSocket = speedTestSocket;
    }

    /**
     * Factory constructor to create an instance
     * @return Instance of BandwidthAnalyzer or null on error.
     */
    @Nullable
    public static BandwidthAnalyzer create(ResultsCallback resultsCallback) {
        SpeedTestSocket speedTestSocket = new SpeedTestSocket();
        if (speedTestSocket != null) {
            return new BandwidthAnalyzer(speedTestSocket, resultsCallback);
        }
        return null;
    }

    /**
     * register listener for download/upload stats
     *
     * @param bandwidthTestListener listener to add
     */
    private void registerBandwidthListener(@Nullable BandwidthTestListener bandwidthTestListener) {
        BandwidthTestListener newListener = null;
        if (bandwidthTestListener != null) {
            newListener = bandwidthTestListener;
        } else {
            newListener = new BandwidthTestListener();
        }
        this.speedTestSocket.addSpeedTestListener(newListener);
    }

    /**
     * start the speed test
     */
    private void startBandwidthTest() {
        this.speedTestSocket.startDownload("2.testdebit.info", "/fichiers/1Mo.dat");
    }


    private class BandwidthTestListener implements ISpeedTestListener {

        /**
         * monitor download process result with transfer rate in bit/s and octet/s.
         *
         * @param report download speed test report
         */
        public void onDownloadFinished(SpeedTestReport report) {
            // called when download is finished
            Log.v("speedtest", "[DL FINISHED] rate in bit/s   : " + report.getTransferRateBit());
            if (resultsCallback != null) {
                resultsCallback.onDownloadFinished(report.getTransferRateBit().doubleValue());
            }
        }

        public void onDownloadProgress(float percent, SpeedTestReport report) {
            // called to notify download progress
            Log.v("speedtest", "[DL PROGRESS] progress : " + percent + "%");
            Log.v("speedtest", "[DL PROGRESS] rate in bit/s   : " + report.getTransferRateBit());
            if (resultsCallback != null) {
                resultsCallback.onDownloadProgress((double)percent, report.getTransferRateBit().doubleValue());
            }
        }

        public void onDownloadError(SpeedTestError speedTestError, String errorMessage) {
            Log.v("speedtest", "[DL ERROR] : " + speedTestError.name());
            Log.v("speedtest", "[DL ERROR MESG] : " + errorMessage);
            if (resultsCallback != null) {
                resultsCallback.onDownloadError(speedTestError, errorMessage);
            }
        }

        public void onUploadFinished(SpeedTestReport report) {
            // called when an upload is finished
            Log.v("speedtest", "[UL FINISHED] rate in bit/s   : " + report.getTransferRateBit());
            if (resultsCallback != null) {
                resultsCallback.onUploadFinished(report.getTransferRateBit().doubleValue());
            }
        }

        public void onUploadError(SpeedTestError speedTestError, String errorMessage) {
            Log.v("speedtest", "[UL ERROR] : " + speedTestError.name());
            Log.v("speedtest", "[UL ERROR] mesg : " + errorMessage);
            if (resultsCallback != null) {
                resultsCallback.onUploadError(speedTestError, errorMessage);
            }
        }

        public void onUploadProgress(float percent, SpeedTestReport report) {
            // called to notify upload progress
            Log.v("speedtest", "[UL PROGRESS] progress : " + percent + "%");
            Log.v("speedtest", "[UL PROGRESS] rate in bit/s   : " + report.getTransferRateBit());
            if (resultsCallback != null) {
                resultsCallback.onDownloadProgress((double)percent, report.getTransferRateBit().doubleValue());
            }
        }

        public void onInterruption() {
        }

    }
}
