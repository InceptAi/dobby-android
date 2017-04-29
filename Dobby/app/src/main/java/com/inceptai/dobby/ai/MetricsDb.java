package com.inceptai.dobby.ai;

/**
 * Created by arunesh on 4/19/17.
 */

/**
 * Stores network layer metrics and freshness etc for consumption by IE.
 */
public class MetricsDb {
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

    public void clearAllGrades() {
        clearBandwidthGrade();
        clearHttpGrade();
        clearWifiGrade();
        clearPingGrade();
    }

    public void clearBandwidthGrade() {
        bandwidthGrade.clear();
    }

    public void clearUploadBandwidthGrade() {
        bandwidthGrade.clearUpload();
    }

    public void clearDownloadBandwidthGrade() {
        bandwidthGrade.clearDownload();
    }

    public void clearWifiGrade() {
        wifiGrade.clear();
    }

    public void clearPingGrade() {
        pingGrade.clear();
    }

    public void clearHttpGrade() {
        httpGrade.clear();
    }

    public void updateBandwidthMetric(@DataInterpreter.MetricType int downloadMetric,
                                      @DataInterpreter.MetricType int uploadMetric) {
        updateDownloadMetric(downloadMetric);
        updateUploadMetric(uploadMetric);
    }

    public void updateDownloadMetric(@DataInterpreter.MetricType int metric) {
        bandwidthGrade.updateDownloadMetric(metric);
    }

    public void updateUploadMetric(@DataInterpreter.MetricType int metric) {
        bandwidthGrade.updateUploadMetric(metric);
    }



    public void updateBandwidthGrade(DataInterpreter.BandwidthGrade bandwidthGrade) {
        this.bandwidthGrade = bandwidthGrade;
        this.bandwidthGrade.updateTimestamp();
    }

    public void updateUploadBandwidthGrade(double uploadMbps, @DataInterpreter.MetricType int uploadMetric) {
        this.bandwidthGrade.updateUploadInfo(uploadMbps, uploadMetric);
    }

    public void updateDownloadBandwidthGrade(double uploadMbps, @DataInterpreter.MetricType int uploadMetric) {
        this.bandwidthGrade.updateDownloadInfo(uploadMbps, uploadMetric);
    }


    public boolean hasValidUpload() {
        return bandwidthGrade.hasValidUpload();
    }

    public boolean hasValidDownload() {
        return bandwidthGrade.hasValidDownload();
    }

    public boolean hasFreshWifiGrade() {
        return wifiGrade.isFresh();
    }

    public boolean hasFreshHttpGrade() {
        return httpGrade.isFresh();
    }

    public boolean hasFreshPingGrade() {
        return pingGrade.isFresh();
    }

    public double getDownloadMbps() {
        return bandwidthGrade.getDownloadMbps();
    }

    public double getUploadMbps() {
        return bandwidthGrade.getUploadMbps();
    }

    public void updateWifiGrade(DataInterpreter.WifiGrade wifiGrade) {
        this.wifiGrade = wifiGrade;
        this.wifiGrade.updateTimestamp();
    }

    public void updateHttpGrade(DataInterpreter.HttpGrade httpGrade) {
        this.httpGrade = httpGrade;
        this.httpGrade.updateTimestamp();
    }

    public void updatePingGrade(DataInterpreter.PingGrade pingGrade) {
        this.pingGrade = pingGrade;
        this.pingGrade.updateTimestamp();
    }

    public SuggestionCreator.SuggestionCreatorParams getParamsForSuggestions() {
        SuggestionCreator.SuggestionCreatorParams params = new SuggestionCreator.SuggestionCreatorParams();
        params.wifiGrade = wifiGrade;
        params.bandwidthGrade = bandwidthGrade;
        params.pingGrade = pingGrade;
        params.httpGrade = httpGrade;
        return params;
    }

}
