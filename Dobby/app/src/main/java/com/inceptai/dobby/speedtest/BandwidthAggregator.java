package com.inceptai.dobby.speedtest;

import android.support.annotation.Nullable;
import android.util.Log;

import com.inceptai.dobby.utils.Utils;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.IRepeatListener;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;

/**
 * Created by vivek on 4/6/17.
 */

public class BandwidthAggregator {
    private int MAX_THREADS = 10;

    private HashMap<Integer, BandwidthInfo> aggregateBandwidthInfo;
    private double currentTransferRate;
    private ResultsCallback resultsCallback;
    private boolean[] testInFlight;
    private AtomicBoolean anyThreadFinished;

    /**
     * Callback interface for results. More methods to follow.
     */
    public interface ResultsCallback {
        void onFinish(BandwidthStats stats);
        void onProgress(double instantBandwidth);
        void onError(SpeedTestError speedTestError, String errorMessage);
    }

    public BandwidthAggregator(@Nullable ResultsCallback resultsCallback) {
        //TODO: Convert Hashmap to sparseArray for performance
        aggregateBandwidthInfo = new HashMap<>();
        currentTransferRate = 0;
        testInFlight = new boolean[MAX_THREADS];
        Arrays.fill(testInFlight, false);
        anyThreadFinished = new AtomicBoolean(false);
        this.resultsCallback = resultsCallback;
    }

    private BandwidthInfo addNewBandwidthInfo(int id) throws InvalidParameterException {
        if (id > MAX_THREADS) {
            throw new InvalidParameterException("Max thread count is: " + MAX_THREADS);
        }
        BandwidthInfo bandwidthInfo = new BandwidthInfo(id);
        aggregateBandwidthInfo.put(id, bandwidthInfo);
        bandwidthInfo.speedTestSocket.addSpeedTestListener(bandwidthInfo.aggregatorListener);
        return bandwidthInfo;
    }

    //Get socket
    public SpeedTestSocket getSpeedTestSocket(int id) {
        SpeedTestSocket socket = null;
        BandwidthInfo bandwidthInfo = aggregateBandwidthInfo.get(id);
        if (bandwidthInfo == null) {
            bandwidthInfo = addNewBandwidthInfo(id);
        }
        socket = bandwidthInfo.speedTestSocket;
        return socket;
    }

    //Get socket
    public AggregatorListener getListener(int id) {
        AggregatorListener listener = null;
        BandwidthInfo bandwidthInfo = aggregateBandwidthInfo.get(id);
        if (bandwidthInfo == null) {
            bandwidthInfo = addNewBandwidthInfo(id);
        }
        listener = bandwidthInfo.aggregatorListener;
        return listener;
    }


    private void updateCurrentTransferRate() {
        // Iterating over values only
        double cumulativeRate = 0;
        for (BandwidthInfo info : aggregateBandwidthInfo.values()) {
            if (info.transferRates.size() > 0)
                cumulativeRate += info.transferRates.get(info.transferRates.size() - 1);
        }
        currentTransferRate = cumulativeRate;
    }


    private void updateRate(int id, double newRate) {
        BandwidthInfo bandwidthInfo = aggregateBandwidthInfo.get(id);
        if (bandwidthInfo == null) {
            bandwidthInfo = new BandwidthInfo(id);
            aggregateBandwidthInfo.put(id, bandwidthInfo);
        }
        bandwidthInfo.updateRate(newRate);
        updateCurrentTransferRate();
    }

    //Test in flight variables
    synchronized  private boolean checkIfAllThreadsDone() {
        for (int threadIndex=0; threadIndex < testInFlight.length; threadIndex++) {
            if (testInFlight[threadIndex]) {
                return false;
            }
        }
        return true;
    }

    synchronized private void updateTestAsDone(int id) {
        if (id < MAX_THREADS) {
            testInFlight[id] = false;
        }
    }

    synchronized private void updateTestAsStarted(int id) {
        if (id < MAX_THREADS) {
            testInFlight[id] = true;
        }
    }

    //Generate final stats
    public BandwidthStats getFinalBandwidthStats() {
        BandwidthStats stats = new BandwidthStats();
        int numThreads = 0;
        List<Double> combinedRates = new ArrayList<>();
        // Iterating over values only
        for (BandwidthInfo info : aggregateBandwidthInfo.values()) {
            numThreads++;
            combinedRates.addAll(info.transferRates);
        }
        if (combinedRates.size() > 0) {
            Collections.sort(combinedRates);
            double min = combinedRates.get(0);
            double max = combinedRates.get(combinedRates.size() - 1);
            double median = Utils.computePercentileFromSortedList(combinedRates, 50);
            double percentile90 = Utils.computePercentileFromSortedList(combinedRates, 90);
            double percentile10 = Utils.computePercentileFromSortedList(combinedRates, 10);
            stats = new BandwidthStats(numThreads, max, min,
                    median, percentile90, percentile10);
        }
        return stats;
    }


    public static class BandwidthStats {
        public int threads;
        public double maxThroughput;
        public double minThroughput;
        public double medianThroughput;
        public double percentile90Throughput;
        public double percentile10Throughput;

        public BandwidthStats(int threads, double max, double min, double median,
                              double percentile90, double percentile10) {
            this.threads = threads;
            this.maxThroughput = max;
            this.minThroughput = min;
            this.medianThroughput = median;
            this.percentile90Throughput = percentile90;
            this.percentile10Throughput = percentile10;
        }

        public BandwidthStats() {
            this.threads = 0;
            this.maxThroughput = -1;
            this.minThroughput = -1;
            this.medianThroughput = -1;
            this.percentile90Throughput = -1;
            this.percentile10Throughput = -1;
        }
    }

    private class BandwidthInfo {
        private int id;
        private SpeedTestSocket speedTestSocket;
        private AggregatorListener aggregatorListener;
        private List<Double> transferRates;

        public BandwidthInfo(int id) {
            this.id = id;
            this.speedTestSocket = new SpeedTestSocket();
            this.aggregatorListener = new AggregatorListener(id);
            this.transferRates = new ArrayList<>();
        }

        public void updateRate(double rate) {
            this.transferRates.add(rate);
        }
    }


    public class AggregatorListener implements ISpeedTestListener, IRepeatListener {

        private int id;

        public AggregatorListener(int id) {
            this.id = id;
        }

        public int getListenerId() {
            return id;
        }


        /**
         * monitor download process result with transfer rate in bit/s and octet/s.
         *
         * @param report download speed test report
         */
        @Override
        public void onDownloadFinished(SpeedTestReport report) {
            // called when download is finished
            Log.v("speedtest", "[DL FINISHED] rate in bit/s   : " + report.getTransferRateBit());
        }

        @Override
        public void onDownloadProgress(float percent, SpeedTestReport report) {
            //Update transfer rate
        }

        @Override
        public void onDownloadError(SpeedTestError speedTestError, String errorMessage) {
            if (resultsCallback != null) {
                resultsCallback.onError(speedTestError, errorMessage);
            }
        }

        @Override
        public void onReport(final SpeedTestReport report) {
            // called when a download report is dispatched
            // called to notify download progress
            //Update transfer rate
            updateTestAsStarted(id);
            if (!anyThreadFinished.get()) {
                updateRate(id, report.getTransferRateBit().doubleValue());
            }
            if (resultsCallback != null) {
                resultsCallback.onProgress(currentTransferRate);
            }
        }

        @Override
        public void onFinish(final SpeedTestReport report) {
            // called when repeat task is finished
            // called to notify download progress
            anyThreadFinished.set(true);
            updateTestAsDone(id);
            if (resultsCallback != null && checkIfAllThreadsDone()) {
                resultsCallback.onFinish(getFinalBandwidthStats());
            }
        }

        @Override
        public void onUploadFinished(SpeedTestReport report) {
            Log.v("speedtest", "[UL FINISHED] rate in bit/s   : " + report.getTransferRateBit());
        }

        @Override
        public void onUploadError(SpeedTestError speedTestError, String errorMessage) {
            if (resultsCallback != null) {
                resultsCallback.onError(speedTestError, errorMessage);
            }
        }

        @Override
        public void onUploadProgress(float percent, SpeedTestReport report) {
            //No-op
        }

        @Override
        public void onInterruption() {
        }

    }
}
