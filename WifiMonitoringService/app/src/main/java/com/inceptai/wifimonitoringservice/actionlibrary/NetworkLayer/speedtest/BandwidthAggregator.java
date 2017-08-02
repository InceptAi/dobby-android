package com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.speedtest;

import android.support.annotation.Nullable;

import com.inceptai.wifimonitoringservice.utils.ServiceLog;
import com.inceptai.wifimonitoringservice.utils.Utils;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
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
    private final static int MAX_THREADS = 10;
    private HashMap<Integer, BandwidthInfo> aggregateBandwidthInfo;
    private double currentTransferRate;
    private ResultsCallback resultsCallback;
    private boolean[] testInFlight;
    private AtomicBoolean anyThreadFinished;
    private List<Double> instantBandwidthList;

    /**
     * Callback interface for results. More methods to follow.
     */
    public interface ResultsCallback {
        void onFinish(BandwidthStats stats);
        void onProgress(double instantBandwidth);
        void onError(SpeedTestError speedTestError, String errorMessage);
    }

    BandwidthAggregator(@Nullable ResultsCallback resultsCallback) {
        //TODO: Convert Hashmap to sparseArray for performance
        aggregateBandwidthInfo = new HashMap<>();
        currentTransferRate = 0;
        testInFlight = new boolean[MAX_THREADS];
        Arrays.fill(testInFlight, false);
        anyThreadFinished = new AtomicBoolean(false);
        this.resultsCallback = resultsCallback;
        instantBandwidthList = new ArrayList<>();
    }

    //Get socket
    SpeedTestSocket getSpeedTestSocket(int id) {
        SpeedTestSocket socket = null;
        BandwidthInfo bandwidthInfo = getBandwidthInfo(id);
        if (bandwidthInfo == null) {
            ServiceLog.v("Adding new bandwidth info with id: " + id);
            bandwidthInfo = addNewBandwidthInfo(id);
        }
        socket = bandwidthInfo.speedTestSocket;
        ServiceLog.v("Returning socket with socketid, id " + socket.toString() + "," + id);
        return socket;
    }

    //Get socket
    AggregatorListener getListener(int id) {
        AggregatorListener listener = null;
        BandwidthInfo bandwidthInfo = getBandwidthInfo(id);
        if (bandwidthInfo == null) {
            bandwidthInfo = addNewBandwidthInfo(id);
        }
        listener = bandwidthInfo.aggregatorListener;
        ServiceLog.v("Returning listener with uuid, id " + listener.uuid + "," + id);
        return listener;
    }

    void cancelTestsAndCleanupAsync(ExecutorService executorService) {
        cleanUp();
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                cancelActiveSocketsSync();
            }
        });
    }

    void cleanUp() {
        ServiceLog.v("BA: Calling cleanup");
        for (BandwidthInfo bandwidthInfo: aggregateBandwidthInfo.values()) {
            bandwidthInfo.disableCallbackForListener();
        }
        resultsCallback = null;
    }

    //Get socket
    private void cancelActiveSocketsSync() {
        List<SpeedTestSocket> activeSocketList = getActiveSockets();
        for (SpeedTestSocket socket: activeSocketList) {
            ServiceLog.v("BandwidthAggregator forceStopping speedtest socket: " + socket.toString());
            socket.forceStopTask();
        }
    }

    private BandwidthInfo addNewBandwidthInfo(int id) throws InvalidParameterException {
        if (id > MAX_THREADS) {
            throw new InvalidParameterException("Max thread count is: " + MAX_THREADS);
        }
        BandwidthInfo bandwidthInfo = new BandwidthInfo(id, resultsCallback);
        aggregateBandwidthInfo.put(id, bandwidthInfo);
        bandwidthInfo.speedTestSocket.addSpeedTestListener(bandwidthInfo.aggregatorListener);
        return bandwidthInfo;
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
        BandwidthInfo bandwidthInfo = getBandwidthInfo(id);
        if (bandwidthInfo != null) {
            bandwidthInfo.updateRate(newRate);
        }
        updateCurrentTransferRate();
    }

    private BandwidthInfo getBandwidthInfo(int id) {
        if (aggregateBandwidthInfo != null) {
            return aggregateBandwidthInfo.get(id);
        }
        return null;
    }

    //Test in flight variables
    synchronized  private boolean checkIfAllThreadsDone() {
        for (int threadIndex=0; threadIndex < testInFlight.length; threadIndex++) {
            if (testInFlight[threadIndex]) {
                ServiceLog.v("Thread not done index: " + threadIndex);
                return false;
            }
        }
        ServiceLog.v("All threads are done ");
        return true;
    }

    synchronized private void updateTestAsDone(int id) {
        ServiceLog.v("Coming into updateTestasDone for id: " + id);
        if (id < MAX_THREADS) {
            testInFlight[id] = false;
            ServiceLog.v("Updating listener as done with id: " + id);
        }
    }

    synchronized private void updateTestAsStarted(int id) {
        if (id < MAX_THREADS) {
            testInFlight[id] = true;
            ServiceLog.v("Updating listener as started with id: " + id);
        }
    }

    synchronized private boolean checkIfThreadHasStarted(int id) {
        return testInFlight[id];
    }

    //Get socket
    synchronized private List<SpeedTestSocket> getActiveSockets() {
        List<SpeedTestSocket> activeSocketList = new ArrayList<>();
        for (BandwidthInfo info : aggregateBandwidthInfo.values()) {
            if(checkIfThreadHasStarted(info.id)) {
                activeSocketList.add(info.speedTestSocket);
            }
        }
        return activeSocketList;
    }



    // Generate final stats
    private BandwidthStats getFinalBandwidthStats() {
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

        BandwidthInfo(int id, @Nullable ResultsCallback aggregatorCallback) {
            this.id = id;
            // this.speedTestSocket = new SpeedTestSocket();
            this.speedTestSocket = SpeedTestSocketFactory.newSocket();
            this.aggregatorListener = new AggregatorListener(id, aggregatorCallback);
            this.transferRates = new ArrayList<>();
        }

        void updateRate(double rate) {
            this.transferRates.add(rate);
        }

        void disableCallbackForListener() {
            aggregatorListener.markListenerAsCancelled();
        }
    }

    private class AggregatorListener implements ISpeedTestListener, IRepeatListener {

        private int id;
        private boolean cancelled;
        private ResultsCallback aggregatorCallback;
        private String uuid;


        AggregatorListener(int id, @Nullable ResultsCallback aggregatorCallback) {
            this.id = id;
            this.cancelled = false;
            this.aggregatorCallback = aggregatorCallback;
            uuid = Utils.generateUUID();
            ServiceLog.v("Initializing new AL with id " + id + " uuid " + uuid);
        }

        private void markListenerAsCancelled() {
            cancelled = true;
            aggregatorCallback = null;
        }

        private boolean isListenerCancelled() {
            return cancelled;
        }


        @Override
        public void onReport(final SpeedTestReport report) {
            // called when a download report is dispatched
            // called to notify download progress
            //Update transfer rate
            ServiceLog.v("onReport id " + id + " uuid: " + uuid);
            if (isListenerCancelled()) {
                return;
            }
            updateTestAsStarted(id);
            if (!anyThreadFinished.get()) {
                updateRate(id, report.getTransferRateBit().doubleValue());
            }
            if (aggregatorCallback != null) {
                aggregatorCallback.onProgress(currentTransferRate);
            }
        }

        @Override
        public void onFinish(final SpeedTestReport report) {
            // called when repeat task is finished
            // called to notify download progress
            // If we are trying to cancel, return now.
            ServiceLog.v("OnFinish id: " + id + " uuid: " + uuid);
            updateTestAsDone(id);
            if (isListenerCancelled()) {
                return;
            }
            anyThreadFinished.set(true);
            if (checkIfAllThreadsDone()) {
                if (aggregatorCallback != null) {
                    aggregatorCallback.onFinish(getFinalBandwidthStats());
                }
            }
        }

        @Override
        public void onDownloadFinished(SpeedTestReport report) {
            // called when download is finished
        }

        @Override
        public void onDownloadProgress(float percent, SpeedTestReport report) {
            //Update transfer rate
        }

        @Override
        public void onDownloadError(SpeedTestError speedTestError, String errorMessage) {
            if (aggregatorCallback != null) {
                aggregatorCallback.onError(speedTestError, errorMessage);
            }
        }

        //Upload callbacks
        @Override
        public void onUploadFinished(SpeedTestReport report) {
        }

        @Override
        public void onUploadError(SpeedTestError speedTestError, String errorMessage) {
            if (aggregatorCallback != null) {
                aggregatorCallback.onError(speedTestError, errorMessage);
            }
        }

        @Override
        public void onUploadProgress(float percent, SpeedTestReport report) {
            //No-op
        }

        @Override
        public void onInterruption() {
            ServiceLog.v("onInterruption id " + id + " uuid: " + uuid);
        }

    }


}
