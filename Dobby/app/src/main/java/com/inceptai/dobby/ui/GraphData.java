package com.inceptai.dobby.ui;

/**
 * Created by arunesh on 4/7/17.
 */

import com.inceptai.dobby.ai.RtDataSource;
import com.inceptai.dobby.utils.DobbyLog;

import java.util.LinkedList;
import java.util.List;

/**
 * Represents the data behind a (real-time) graph.
 */
public class GraphData<T, U> implements RtDataSource.RtDataListener<T> {

    private LinkedList<T> dataPoints;
    private RtDataSource<T, U> rtDataSource;
    private DataUpdateListener listener;

    public interface DataUpdateListener {
        void updateAvailable();
    }

    GraphData() {
        dataPoints = new LinkedList<>();
    }

    GraphData(RtDataSource<T, U> rtDataSource, U sourceType) {
        dataPoints = new LinkedList<>();
        rtDataSource.registerListener(this, sourceType);
        this.rtDataSource = rtDataSource;
    }

    public void setListener(DataUpdateListener listener) {
        this.listener = listener;
    }

    public void clearListener() {
        listener = null;
    }

    @Override
    public void onUpdate(T dataItem) {
        DobbyLog.i("Update avail: " + dataItem);
        addData(dataItem);
        if (listener != null) {
            listener.updateAvailable();
        }
    }

    @Override
    public void onClose() {
        clearListener();
    }

    void addData(T dataPoint) {
        dataPoints.add(dataPoint);
    }

    public RtDataSource<T, U> getDataSource() {
        return rtDataSource;
    }

    public int numPoints() {
        return dataPoints.size();
    }

    public List<T> getData() {
        return dataPoints;
    }
}
