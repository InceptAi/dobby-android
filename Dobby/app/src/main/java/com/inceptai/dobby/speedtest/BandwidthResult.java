package com.inceptai.dobby.speedtest;

import com.inceptai.dobby.model.BandwidthStats;

/**
 * Created by arunesh on 4/20/17.
 */

public class BandwidthResult {

    private BandwidthStats uploadStats;
    private BandwidthStats downloadStats;
    @BandwidthTestCodes.TestMode
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
}
