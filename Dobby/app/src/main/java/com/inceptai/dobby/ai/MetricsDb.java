package com.inceptai.dobby.ai;

/**
 * Created by arunesh on 4/19/17.
 */

/**
 * Stores network layer metrics and freshness etc for consumption by IE.
 */
public class MetricsDb {
    private static final long MAX_STALENESS_MS = 120 * 1000; // 120 seconds.

    // Bandwidth stats.
    private double uploadMbps;
    private long uploadTimestampMs;
    private double downloadMbps;
    private long downloadTimestampMs;


    MetricsDb() {
        clearDownloadMbps();
        clearUploadMbps();
    }


    public void clearUploadMbps() {
        uploadMbps = -1.0;
        uploadTimestampMs = 0;
    }

    public void clearDownloadMbps() {
        downloadMbps = -1.0;
        downloadTimestampMs = 0;
    }

    public void reportUploadMbps(double uploadMbps){
        this.uploadMbps = uploadMbps;
        uploadTimestampMs = System.currentTimeMillis();
    }

    public void reportDownloadMbps(double downloadMbps) {
        this.downloadMbps = downloadMbps;
        downloadTimestampMs = System.currentTimeMillis();
    }

    public boolean hasValidUpload() {
        return uploadMbps > 0.0 && isFresh(uploadTimestampMs);
    }

    public boolean hasValidDownload() {
        return downloadMbps > 0.0 && isFresh(downloadTimestampMs);
    }

    private static boolean isFresh(long timestampMs) {
        return (System.currentTimeMillis() - timestampMs < MAX_STALENESS_MS);
    }

    public double getUploadMbps() {
        return uploadMbps;
    }

    public double getDownloadMbps() {
        return downloadMbps;
    }
}
