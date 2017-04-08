package com.inceptai.dobby.ui;

/**
 * Created by arunesh on 4/7/17.
 */

import android.util.Log;

import com.inceptai.dobby.ai.RtDataSource;

import java.util.LinkedList;
import java.util.List;
import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * Represents the data behind a (real-time) graph.
 */
public class GraphData<T> implements RtDataSource.RtDataListener<T> {

    private LinkedList<T> dataPoints;
    private RtDataSource<T> rtDataSource;
    private DataUpdateListener listener;

    public interface DataUpdateListener {
        void updateAvailable();
    }

    GraphData() {
        dataPoints = new LinkedList<>();
    }

    GraphData(RtDataSource<T> rtDataSource) {
        dataPoints = new LinkedList<>();
        rtDataSource.registerListener(this);
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
        Log.i(TAG, "Update avail: " + dataItem);
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

    public RtDataSource<T> getDataSource() {
        return rtDataSource;
    }

    public int numPoints() {
        return dataPoints.size();
    }

    public List<T> getData() {
        return dataPoints;
    }
}
