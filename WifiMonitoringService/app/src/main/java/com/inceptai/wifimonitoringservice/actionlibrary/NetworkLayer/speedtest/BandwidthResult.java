package com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.speedtest;

import com.inceptai.wifimonitoringservice.actionlibrary.utils.ActionLibraryCodes;

/**
 * Created by arunesh on 4/20/17.
 */

public class BandwidthResult {

    private BandwidthStats uploadStats;
    private BandwidthStats downloadStats;
    @ActionLibraryCodes.BandwidthTestMode
    private int testMode;


    public BandwidthResult(int testMode) {
        this.testMode = testMode;
    }

    public BandwidthStats getUploadStats() {
        return uploadStats;
    }

    public void setUploadStats(BandwidthStats uploadStats) {
        this.uploadStats = uploadStats;
    }

    public BandwidthStats getDownloadStats() {
        return downloadStats;
    }

    public void setDownloadStats(BandwidthStats downloadStats) {
        this.downloadStats = downloadStats;
    }

    public int getTestMode() {
        return testMode;
    }

    public void setTestMode(int testMode) {
        this.testMode = testMode;
    }

}
