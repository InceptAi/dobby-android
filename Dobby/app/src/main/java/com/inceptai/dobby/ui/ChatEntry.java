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

    private static int UNKNOWN = -1;
    private String text;
    private int entryType = UNKNOWN;
    private GraphData<Float> graphData;


    public ChatEntry(String text, int entryType) {
        this.text = text;
        this.entryType = entryType;
    }

    public void addGraph(GraphData<Float> graphData) {
        this.graphData = graphData;
    }

    public String getText() {
        return text;
    }

    public int getEntryType() {
        return entryType;
    }

    public GraphData<Float> getGraphData() {
        return graphData;
    }
}
