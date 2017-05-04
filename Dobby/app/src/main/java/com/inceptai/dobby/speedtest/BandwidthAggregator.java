package com.inceptai.dobby.speedtest;

import android.support.annotation.Nullable;

import com.inceptai.dobby.model.BandwidthStats;
import com.inceptai.dobby.utils.DobbyLog;
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
    private List<Double> instantBandwidthList;
    private AtomicBoolean cancelling;

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
        instantBandwidthList = new ArrayList<>();
        cancelling = new AtomicBoolean(false);
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
        instantBandwidthList.add(currentTransferRate);
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
                DobbyLog.v("Thread not done index: " + threadIndex);
                return false;
            }
        }
        DobbyLog.v("All threads are done ");
        return true;
    }

    synchronized private void updateTestAsDone(int id) {
        DobbyLog.v("Coming into updateTestasDone for id: " + id);
        if (id < MAX_THREADS) {
            testInFlight[id] = false;
            DobbyLog.v("Updating listener as done with id: " + id);
        }
    }

    synchronized private void updateTestAsStarted(int id) {
        if (id < MAX_THREADS) {
            testInFlight[id] = true;
            DobbyLog.v("Updating listener as started with id: " + id);
        }
    }

    synchronized private boolean checkIfThreadHasStarted(int id) {
        return testInFlight[id];
    }

    //Get socket
    synchronized public List<SpeedTestSocket> getActiveSockets() {
        List<SpeedTestSocket> activeSocketList = new ArrayList<>();
        for (BandwidthInfo info : aggregateBandwidthInfo.values()) {
            if(checkIfThreadHasStarted(info.id)) {
                activeSocketList.add(info.speedTestSocket);
            }
        }
        return activeSocketList;
    }

    private void cleanUpOnCancellationOrFinish() {
        aggregateBandwidthInfo.clear();
        instantBandwidthList.clear();
        currentTransferRate = 0;
        Arrays.fill(testInFlight, false);
        anyThreadFinished.set(false);
        cancelling.set(false);
    }

    //Get socket
    public boolean cancelActiveSockets() {
        cancelling.set(true);
        final int WAIT_TIMEOUT_FOR_CANCELLATION_MS = 50;
        List<SpeedTestSocket> activeSocketList = getActiveSockets();
        for (SpeedTestSocket socket: activeSocketList) {
            socket.forceStopTask();
        }
        //blocking call
        while(!checkIfAllThreadsDone()) {
            try {
                Thread.sleep(WAIT_TIMEOUT_FOR_CANCELLATION_MS);                 //1000 milliseconds is one second.
            } catch(InterruptedException e) {
                DobbyLog.v("Interrupted while sleeping: " + e);
                Thread.currentThread().interrupt();
                return false;
            }
        }
        cleanUpOnCancellationOrFinish();
        return true;
    }

    // Generate final stats
    public BandwidthStats getFinalBandwidthStats() {
        BandwidthStats stats = BandwidthStats.EMPTY_STATS;
        int numThreads = 0;
        // Iterating over values only
        for (BandwidthInfo info : aggregateBandwidthInfo.values()) {
            numThreads++;
        }
        if (instantBandwidthList.size() > 0) {
            Collections.sort(instantBandwidthList);
            double min = instantBandwidthList.get(0);
            double max = instantBandwidthList.get(instantBandwidthList.size() - 1);
            double median = Utils.computePercentileFromSortedList(instantBandwidthList, 50);
            double percentile90 = Utils.computePercentileFromSortedList(instantBandwidthList, 90);
            double percentile75 = Utils.computePercentileFromSortedList(instantBandwidthList, 75);
            double percentile10 = Utils.computePercentileFromSortedList(instantBandwidthList, 10);
            stats = new BandwidthStats(numThreads, max, min,
                    median, percentile90, percentile75, percentile10);
        }
        return stats;
    }

    private class BandwidthInfo {
        private int id;
        private SpeedTestSocket speedTestSocket;
        private AggregatorListener aggregatorListener;
        private List<Double> transferRates;

        public BandwidthInfo(int id) {
            this.id = id;
            // this.speedTestSocket = new SpeedTestSocket();
            this.speedTestSocket = SpeedTestSocketFactory.newSocket();
            this.aggregatorListener = new AggregatorListener(id);
            this.transferRates = new ArrayList<>();
        }

        public void updateRate(double rate) {
            this.transferRates.add(rate);
        }
    }


    public class AggregatorListener implements ISpeedTestListener, IRepeatListener {

        private int id;
        private boolean cancelled;


        public AggregatorListener(int id) {
            this.id = id;
            this.cancelled = false;
        }

        public int getListenerId() {
            return id;
        }

        private void markListenerAsCancelled() {
            cancelled = true;
        }

        private boolean isListenerCancelled() {
            return cancelled;
        }


        @Override
        public void onReport(final SpeedTestReport report) {
            // called when a download report is dispatched
            // called to notify download progress
            //Update transfer rate
            DobbyLog.v("onReport id " + id);
            if (isListenerCancelled()) {
                return;
            }
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
            // If we are trying to cancel, return now.
            DobbyLog.v("OnFinish id: " + id);
            updateTestAsDone(id);
            if (isListenerCancelled()) {
                return;
            }
            anyThreadFinished.set(true);
            if (checkIfAllThreadsDone()) {
                if (resultsCallback != null) {
                    resultsCallback.onFinish(getFinalBandwidthStats());
                }
                cleanUpOnCancellationOrFinish();
            }
        }

        @Override
        public void onDownloadFinished(SpeedTestReport report) {
            // called when download is finished
            DobbyLog.v("SpeedTest: [DL FINISHED] rate in bit/s   : " + report.getTransferRateBit());
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

        //Upload callbacks
        @Override
        public void onUploadFinished(SpeedTestReport report) {
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
            DobbyLog.v("onInterruption id " + id);
            if (cancelling.get()) {
                markListenerAsCancelled();
            }
            //It is not really done here -- this is a temp fix.
            //updateTestAsDone(id);
        }

    }
}
