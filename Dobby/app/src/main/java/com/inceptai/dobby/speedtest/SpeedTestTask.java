package com.inceptai.dobby.speedtest;

/**
 * Created by vivek on 3/30/17.
 */

import android.os.AsyncTask;
import android.util.Log;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;

public class SpeedTestTask extends AsyncTask<Void, Void, String> {

    @Override
    public String doInBackground(Void... params) {

        SpeedTestSocket speedTestSocket = new SpeedTestSocket();

        // add a listener to wait for speedtest completion and progress
        speedTestSocket.addSpeedTestListener(new ISpeedTestListener() {

            @Override
            public void onDownloadFinished(SpeedTestReport report) {
                // called when download is finished
                Log.v("speedtest", "[DL FINISHED] rate in octet/s : " + report.getTransferRateOctet());
                Log.v("speedtest", "[DL FINISHED] rate in bit/s   : " + report.getTransferRateBit());
            }

            @Override
            public void onDownloadError(SpeedTestError speedTestError, String errorMessage) {
                // called when a download error occur
            }

            @Override
            public void onUploadFinished(SpeedTestReport report) {
                // called when an upload is finished
                Log.v("speedtest", "[UL FINISHED] rate in octet/s : " + report.getTransferRateOctet());
                Log.v("speedtest", "[UL FINISHED] rate in bit/s   : " + report.getTransferRateBit());
            }

            @Override
            public void onUploadError(SpeedTestError speedTestError, String errorMessage) {
                // called when an upload error occur
            }

            @Override
            public void onDownloadProgress(float percent, SpeedTestReport report) {
                // called to notify download progress
                Log.v("speedtest", "[DL PROGRESS] progress : " + percent + "%");
                Log.v("speedtest", "[DL PROGRESS] rate in octet/s : " + report.getTransferRateOctet());
                Log.v("speedtest", "[DL PROGRESS] rate in bit/s   : " + report.getTransferRateBit());
            }

            @Override
            public void onUploadProgress(float percent, SpeedTestReport report) {
                // called to notify upload progress
                Log.v("speedtest", "[UL PROGRESS] progress : " + percent + "%");
                Log.v("speedtest", "[UL PROGRESS] rate in octet/s : " + report.getTransferRateOctet());
                Log.v("speedtest", "[UL PROGRESS] rate in bit/s   : " + report.getTransferRateBit());
            }

            @Override
            public void onInterruption() {
                // triggered when forceStopTask is called
            }
        });

        speedTestSocket.startDownload("2.testdebit.info", "/fichiers/1Mo.dat");

        return null;
    }
}