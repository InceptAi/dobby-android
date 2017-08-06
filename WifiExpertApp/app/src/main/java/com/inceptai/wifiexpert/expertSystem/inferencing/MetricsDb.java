package com.inceptai.wifiexpert.expertSystem.inferencing;

/**
 * Created by arunesh on 4/19/17.
 */

/**
 * Stores network layer metrics and freshness etc for consumption by IE.
 */
class MetricsDb {
    private DataInterpreter.BandwidthGrade bandwidthGrade;
    private DataInterpreter.PingGrade pingGrade;
    private DataInterpreter.WifiGrade wifiGrade;
    private DataInterpreter.HttpGrade httpGrade;

    //TODO: Create static version of these grade objects.
    MetricsDb() {
        bandwidthGrade = new DataInterpreter.BandwidthGrade();
        pingGrade = new DataInterpreter.PingGrade();
        wifiGrade = new DataInterpreter.WifiGrade();
        httpGrade = new DataInterpreter.HttpGrade();
    }

    void updateBandwidthMetric(@DataInterpreter.MetricType int downloadMetric,
                                      @DataInterpreter.MetricType int uploadMetric) {
        updateDownloadMetric(downloadMetric);
        updateUploadMetric(uploadMetric);
    }

    void cleanup() {
        clearAllGrades();
    }

    void clearAllGrades() {
        clearBandwidthGrade();
        clearHttpGrade();
        clearWifiGrade();
        clearPingGrade();
    }

    void clearUploadBandwidthGrade() {
        bandwidthGrade.clearUpload();
    }

    void clearDownloadBandwidthGrade() {
        bandwidthGrade.clearDownload();
    }


    void updateBandwidthGrade(DataInterpreter.BandwidthGrade bandwidthGrade) {
        this.bandwidthGrade = bandwidthGrade;
        this.bandwidthGrade.updateTimestamp();
    }

    void updateUploadBandwidthGrade(double uploadMbps, @DataInterpreter.MetricType int uploadMetric) {
        this.bandwidthGrade.updateUploadInfo(uploadMbps, uploadMetric);
    }

    void updateDownloadBandwidthGrade(double uploadMbps, @DataInterpreter.MetricType int uploadMetric) {
        this.bandwidthGrade.updateDownloadInfo(uploadMbps, uploadMetric);
    }


    boolean hasValidUpload() {
        return bandwidthGrade.hasValidUpload();
    }

    boolean hasValidDownload() {
        return bandwidthGrade.hasValidDownload();
    }

    boolean hasFreshWifiGrade() {
        return wifiGrade.isFresh();
    }

    boolean hasFreshHttpGrade() {
        return httpGrade.isFresh();
    }

    boolean hasFreshPingGrade() {
        return pingGrade.isFresh();
    }

    double getDownloadMbps() {
        return bandwidthGrade.getDownloadMbps();
    }

    double getUploadMbps() {
        return bandwidthGrade.getUploadMbps();
    }

    void updateWifiGrade(DataInterpreter.WifiGrade wifiGrade) {
        this.wifiGrade = wifiGrade;
        this.wifiGrade.updateTimestamp();
    }

    void updateHttpGrade(DataInterpreter.HttpGrade httpGrade) {
        this.httpGrade = httpGrade;
        this.httpGrade.updateTimestamp();
    }

    void updatePingGrade(DataInterpreter.PingGrade pingGrade) {
        this.pingGrade = pingGrade;
        this.pingGrade.updateTimestamp();
    }

    SuggestionCreator.SuggestionCreatorParams getParamsForSuggestions() {
        SuggestionCreator.SuggestionCreatorParams params = new SuggestionCreator.SuggestionCreatorParams();
        params.wifiGrade = wifiGrade;
        params.bandwidthGrade = bandwidthGrade;
        params.pingGrade = pingGrade;
        params.httpGrade = httpGrade;
        return params;
    }

    private void clearWifiGrade() {
        wifiGrade.clear();
    }

    private void clearPingGrade() {
        pingGrade.clear();
    }

    private void clearHttpGrade() {
        httpGrade.clear();
    }

    private void clearBandwidthGrade() {
        bandwidthGrade.clear();
    }

    private void updateDownloadMetric(@DataInterpreter.MetricType int metric) {
        bandwidthGrade.updateDownloadMetric(metric);
    }

    private void updateUploadMetric(@DataInterpreter.MetricType int metric) {
        bandwidthGrade.updateUploadMetric(metric);
    }
}
