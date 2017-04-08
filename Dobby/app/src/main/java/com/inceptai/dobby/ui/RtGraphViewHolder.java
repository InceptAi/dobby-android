package com.inceptai.dobby.ui;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.inceptai.dobby.R;

import java.util.ArrayList;
import java.util.List;

import lecho.lib.hellocharts.model.Axis;
import lecho.lib.hellocharts.model.Line;
import lecho.lib.hellocharts.model.LineChartData;
import lecho.lib.hellocharts.model.PointValue;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.util.ChartUtils;
import lecho.lib.hellocharts.view.Chart;
import lecho.lib.hellocharts.view.LineChartView;

/**
 * Created by arunesh on 4/7/17.
 */

public class RtGraphViewHolder extends RecyclerView.ViewHolder implements GraphData.DataUpdateListener {
    private static final int MAX_XPOINTS = 50;

    private LineChartView lineChartView;
    private GraphData graphData;
    private LineChartData lineChartData;

    public RtGraphViewHolder(View itemView) {
        super(itemView);
        lineChartView = (LineChartView) itemView.findViewById(R.id.rt_chart);
        lineChartData = new LineChartData();
    }

    private void setupLineChart() {
        List<Line> lineList = new ArrayList<>(1);
        List<Float> rawPoints = graphData.getData();
        List<PointValue> pointList = new ArrayList<>(rawPoints.size());
        for (int i = 0; i < rawPoints.size(); i++) {
            pointList.add(new PointValue(i, rawPoints.get(i)));
        }
        Line line = new Line(pointList);
        line.setColor(ChartUtils.COLOR_ORANGE);
        line.setHasPoints(false);
        lineList.add(line);
        lineChartData.setLines(lineList);
        Axis axisX = new Axis();
        axisX.setLineColor(ChartUtils.COLOR_GREEN);
        axisX.setName("Time");
        Axis axisY = new Axis();
        axisY.setLineColor(ChartUtils.COLOR_GREEN);
        axisY.setName("Bandwidth");
        lineChartData.setAxisYLeft(axisY);
        lineChartData.setAxisXBottom(axisX);
        lineChartView.setLineChartData(lineChartData);
        resetViewport();
    }

    public LineChartView getLineChartView() {
        return lineChartView;
    }

    public void setGraphData(GraphData graphData) {
        this.graphData = graphData;
        graphData.setListener(this);
        setupLineChart();
    }

    public void resetViewport() {
        // Reset viewport height range to (0,100)
        final Viewport v = new Viewport(lineChartView.getMaximumViewport());
        v.bottom = 0;
        v.top = 100;
        v.left = 0;
        v.right = Math.max(MAX_XPOINTS, graphData.numPoints());
        lineChartView.setMaximumViewport(v);
        lineChartView.setCurrentViewport(v);
    }

    /**
     * Unlink data source so that we don't update a recycled view.
     */
    public void unlinkDataSource() {
        graphData.clearListener();
    }

    @Override
    public void updateAvailable() {
        setupLineChart();
    }
}
