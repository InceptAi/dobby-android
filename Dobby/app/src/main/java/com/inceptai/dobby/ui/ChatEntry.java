package com.inceptai.dobby.ui;

import android.support.annotation.Nullable;

import com.inceptai.dobby.ai.DataInterpreter;

import lecho.lib.hellocharts.model.LineChartData;

/**
 * Created by arunesh on 3/28/17.
 */

public class ChatEntry {

    // These type values are directly returned in RecyclerViewAdapter, so they should start with
    // zero.
    public static final int DOBBY_CHAT= 0;
    public static final int USER_CHAT = 1;
    public static final int RT_GRAPH = 2; /* real time graph */
    public static final int BW_RESULTS_GAUGE_CARDVIEW = 3;
    public static final int PING_RESULTS_CARDVIEW = 4;
    public static final int OVERALL_NETWORK_CARDVIEW = 5;

    private static int UNKNOWN = -1;
    private String text;
    private int entryType = UNKNOWN;
    private GraphData<Float, Integer> graphData;
    private double uploadMbps, downloadMbps;

    private DataInterpreter.PingGrade pingGrade;

    // Overall network card has 3 data items:
    private DataInterpreter.WifiGrade wifiGrade;
    private String ispName;
    private String routerIp;

    public ChatEntry(String text, int entryType) {
        this.text = text;
        this.entryType = entryType;
    }

    public void addGraph(GraphData<Float, Integer> graphData) {
        this.graphData = graphData;
    }

    public String getText() {
        return text;
    }

    public int getEntryType() {
        return entryType;
    }

    public GraphData<Float, Integer> getGraphData() {
        return graphData;
    }

    public void setBandwidthResults(double uploadMbps, double downloadMbps) {
        this.uploadMbps = uploadMbps;
        this.downloadMbps = downloadMbps;
    }

    public double getUploadMbps() {
        return uploadMbps;
    }

    public double getDownloadMbps() {
        return downloadMbps;
    }

    public DataInterpreter.PingGrade getPingGrade() {
        return pingGrade;
    }

    public void setPingGrade(DataInterpreter.PingGrade pingGrade) {
        this.pingGrade = pingGrade;
    }

    public DataInterpreter.WifiGrade getWifiGrade() {
        return wifiGrade;
    }

    public void setWifiGrade(DataInterpreter.WifiGrade wifiGrade) {
        this.wifiGrade = wifiGrade;
    }

    public String getIspName() {
        return ispName;
    }

    public void setIspName(String ispName) {
        this.ispName = ispName;
    }

    public String getRouterIp() {
        return routerIp;
    }

    public void setRouterIp(String routerIp) {
        this.routerIp = routerIp;
    }
}
