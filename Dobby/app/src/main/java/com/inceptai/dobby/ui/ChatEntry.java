package com.inceptai.dobby.ui;

import android.support.annotation.Nullable;

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

    private static int UNKNOWN = -1;
    private String text;
    private int entryType = UNKNOWN;
    private GraphData<Float, Integer> graphData;
    private double uploadMbps, downloadMbps;

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
}
