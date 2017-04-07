package com.inceptai.dobby.model;

/**
 * Created by arunesh on 4/6/17.
 */

/**
 * Immutable object that holds the results of an upload/download bandwidth test.
 */
public class BandwidthStats {
    public static final BandwidthStats EMPTY_STATS = new BandwidthStats(0, 0, 0, 0, 0.0, 0.0);
    private final int numThreads;
    private final double max;
    private final double min;
    private final double median;
    private final double percentile90;
    private final double percentile10;

    public BandwidthStats(int numThreads, double max, double min, double median,
                          double percentile90, double percentile10) {
        this.numThreads = numThreads;
        this.max = max;
        this.min = min;
        this.median = median;
        this.percentile90 = percentile90;
        this.percentile10 = percentile10;
    }

    public int getNumThreads() {
        return numThreads;
    }

    public double getMax() {
        return max;
    }

    public double getMin() {
        return min;
    }

    public double getMedian() {
        return median;
    }

    public double getPercentile90() {
        return percentile90;
    }

    public double getPercentile10() {
        return percentile10;
    }
}
