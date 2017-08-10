package com.inceptai.wifiexpertsystem.ui;


import com.inceptai.wifiexpertsystem.expertSystem.inferencing.DataInterpreter;

/**
 * Created by arunesh on 3/28/17.
 */

public class ChatEntry {

    // These type values are directly returned in RecyclerViewAdapter, so they should start with
    // zero.
    public static final int DOBBY_CHAT= 0;
    public static final int USER_CHAT = 1;
    public static final int EXPERT_CHAT = 2;
    public static final int RT_GRAPH = 3; /* real time graph */
    public static final int BW_RESULTS_GAUGE_CARDVIEW = 4;
    public static final int PING_RESULTS_CARDVIEW = 5;
    public static final int OVERALL_NETWORK_CARDVIEW = 6;


    private static int UNKNOWN = -1;
    private String text;
    private int entryType = UNKNOWN;
    private double uploadMbps, downloadMbps;
    private boolean isTestStatusMessage;
    private String primarySSID;
    private int primarySignal;
    @DataInterpreter.MetricType private int primarySignalMetric;
    private String ispName;
    private String routerIp;
    //TODO replace ping grade with latency values
    private DataInterpreter.PingGrade pingGrade;


    ChatEntry(String text, int entryType) {
        this.text = text;
        this.entryType = entryType;
        isTestStatusMessage = false;
    }

    ChatEntry(String text, int entryType, boolean isTestStatusMessage) {
        this.text = text;
        this.entryType = entryType;
        this.isTestStatusMessage = isTestStatusMessage;
    }

    public String getText() {
        return text;
    }
    public DataInterpreter.PingGrade getPingGrade() {
        return pingGrade;
    }
    public void setPingGrade(DataInterpreter.PingGrade pingGrade) {
        this.pingGrade = pingGrade;
    }

    //package private

    int getEntryType() {
        return entryType;
    }

    void setBandwidthResults(double uploadMbps, double downloadMbps) {
        this.uploadMbps = uploadMbps;
        this.downloadMbps = downloadMbps;
    }

    double getUploadMbps() {
        return uploadMbps;
    }

    double getDownloadMbps() {
        return downloadMbps;
    }

    boolean isTestStatusMessage() { return isTestStatusMessage; }

    String getIspName() {
        return ispName;
    }

    void setIspName(String ispName) {
        this.ispName = ispName;
    }

    String getRouterIp() {
        return routerIp;
    }

    void setRouterIp(String routerIp) {
        this.routerIp = routerIp;
    }

    public String getPrimarySSID() {
        return primarySSID;
    }

    public void setPrimarySSID(String primarySSID) {
        this.primarySSID = primarySSID;
    }

    public int getPrimarySignal() {
        return primarySignal;
    }

    public void setPrimarySignal(int primarySignal) {
        this.primarySignal = primarySignal;
    }

    public int getPrimarySignalMetric() {
        return primarySignalMetric;
    }

    public void setPrimarySignalMetric(int primarySignalMetric) {
        this.primarySignalMetric = primarySignalMetric;
    }
}
