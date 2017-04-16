package com.inceptai.dobby.wifi;

import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.inceptai.dobby.model.DobbyWifiInfo;
import com.inceptai.dobby.utils.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.ContentValues.TAG;

/**
 * Created by vivek on 4/8/17.
 */

public class WifiStats {
    private static final int MIN_SIGNAL_CHANGE_FOR_CLEARING_CHANNEL_STATS = 20;
    private static final int SNR_BASE_GAP = 10;
    private static final int SNR_MAX_POSITIVE_GAP = 10;
    private static final int SNR_MAX_POSITIVE_GAP_SQ = SNR_MAX_POSITIVE_GAP * SNR_MAX_POSITIVE_GAP;
    private static final int MOVING_AVERAGE_DECAY_FACTOR = 20;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SignalStrengthZones.HIGH, SignalStrengthZones.MEDIUM,
            SignalStrengthZones.LOW, SignalStrengthZones.FRINGE})
    public @interface SignalStrengthZones {
        int HIGH  = -60;
        int MEDIUM = -80;
        int LOW = -100;
        int FRINGE = -140;
    }

    public String linkSSID;
    public String linkBSSID;
    public String macAddress;
    public int linkSignal;
    public int linkFrequency;
    public int linkSpeedMbps;

    public NetworkInfo.DetailedState lastWifiState;
    public long lastWifiStateTimestampMs;
    public HashMap<Integer, ChannelInfo> channelInfoMap;
    public HashMap<NetworkInfo.DetailedState, List<WifiStateInfo>> detailedWifiStateStats;
    public List<ScanResult> lastWifiScanResult;


    public WifiStats() {
        channelInfoMap = new HashMap<>();
        detailedWifiStateStats = new HashMap<>();
    }

    public void clearWifiConnectionInfo() {
        linkSSID = null;
        linkBSSID = null;
        linkFrequency = 0;
        linkSpeedMbps = 0;
        linkSignal = 0;
        lastWifiState = NetworkInfo.DetailedState.IDLE;
        lastWifiStateTimestampMs = 0;
        channelInfoMap = new HashMap<>();
    }

    public boolean updateSignal(int updatedSignal) {
        if (updatedSignal == 0) {
            return false;
        }
        if (Math.abs(updatedSignal - linkSignal) >= MIN_SIGNAL_CHANGE_FOR_CLEARING_CHANNEL_STATS) {
            channelInfoMap.clear();
            linkSignal = updatedSignal;
            //TODO -- reissue a scan request.
            //Recompute the contention metric here
            updateChannelInfo(lastWifiScanResult);
            return true;
        }
        return false;
    }

    private void printHashMap() {
        for (Map.Entry<NetworkInfo.DetailedState, List<WifiStateInfo>> entry : detailedWifiStateStats.entrySet()) {
            NetworkInfo.DetailedState key = entry.getKey();
            List<WifiStateInfo> wifiStateInfos = entry.getValue();
            StringBuilder sb = new StringBuilder();
            for (WifiStateInfo wifiStateInfo: wifiStateInfos) {
                sb.append(wifiStateInfo.toJson());
            }
            Log.v(TAG, "updateDetailedWifiStateInfo4 Key: " + key.name() + " value: " + sb.toString());
        }
    }

    synchronized public void updateDetailedWifiStateInfo(NetworkInfo.DetailedState detailedWifiState, long timestampMs) {
        //Log.v(TAG, "Coming in updateDetailedWifiStateInfo4 with detailedState " + detailedWifiState.name() + " ts:" + timestampMs);
        if (lastWifiState != detailedWifiState) {
            if (lastWifiStateTimestampMs != 0) {
                WifiStateInfo lastWifiStateInfo = new WifiStateInfo(lastWifiState,
                        lastWifiStateTimestampMs, timestampMs);
                List<WifiStateInfo> currentList = detailedWifiStateStats.get(lastWifiState);
                if (currentList == null) {
                    currentList = new ArrayList<>();
                    detailedWifiStateStats.put(lastWifiState, currentList);
                }
                currentList.add(lastWifiStateInfo);
                Log.v(TAG, "updateDetailedWifiStateInfo current state is: " +
                        detailedWifiState.name() + " last state,lasted: " +
                        lastWifiState.name() + "," + (timestampMs - lastWifiStateTimestampMs) + "ms");
            }
            lastWifiState = detailedWifiState;
            lastWifiStateTimestampMs = timestampMs;
        } else if (lastWifiStateTimestampMs == 0) {
                lastWifiStateTimestampMs = timestampMs;
        }
        //printHashMap();
    }

    public int getNumberOfTimesWifiInState(NetworkInfo.DetailedState detailedState, long timeIntervalMs) {
        int count = 0;
        List<WifiStateInfo> list = detailedWifiStateStats.get(detailedState);
        if (list == null) {
            return 0;
        }
        for (WifiStateInfo wifiStateInfo: list) {
            if (wifiStateInfo.endTimestampMs >= (System.currentTimeMillis() - timeIntervalMs)) {
                count++;
            }
        }
        return count;
    }

    public Utils.PercentileStats getStatsForDetailedState(NetworkInfo.DetailedState detailedState,
                                                          long timeIntervalMs) {
        List<Double> duration = new ArrayList<>();
        List<WifiStateInfo> list = detailedWifiStateStats.get(detailedState);
        if (list == null) {
            return new Utils.PercentileStats();
        }
        for (WifiStateInfo wifiStateInfo: list) {
            if ((System.currentTimeMillis() - wifiStateInfo.endTimestampMs) < timeIntervalMs) {
                duration.add((double)(wifiStateInfo.endTimestampMs - wifiStateInfo.startTimestampMs));
            }
        }
        return new Utils.PercentileStats(duration);
    }

    public double getLastDurationForDetailedStateMs(NetworkInfo.DetailedState detailedState) {
        List<WifiStateInfo> list = detailedWifiStateStats.get(detailedState);
        if (list == null || list.isEmpty()) {
            return -1;
        }
        WifiStateInfo wifiStateInfo = list.get(list.size() -1);
        return wifiStateInfo.endTimestampMs - wifiStateInfo.startTimestampMs;
    }

    public NetworkInfo.DetailedState getCurrentState() {
        return lastWifiState;
    }

    public long getCurrentStateStartTimeMs() {
        return lastWifiStateTimestampMs;
    }

    public HashMap<NetworkInfo.DetailedState, List<WifiStateInfo>> getStatsHashMap() {
        return detailedWifiStateStats;
    }

    public void updateWifiStats(@Nullable DobbyWifiInfo wifiInfo, @Nullable List<ScanResult> scanResultList) {
        if (wifiInfo != null) {
            linkSSID = wifiInfo.getSSID();
            linkBSSID = wifiInfo.getBSSID();
            linkSignal = wifiInfo.getRssi();
            macAddress = wifiInfo.getMacAddress();
            linkSpeedMbps = wifiInfo.getLinkSpeed();
        }
        if (scanResultList != null) {
            lastWifiScanResult = scanResultList;
            if (linkFrequency == 0) {
                updateChannelFrequency(scanResultList);
            }
            updateChannelInfo(scanResultList);
        }
    }

    public String toJson() {
        Gson gson = new Gson();
        String json = gson.toJson(this);
        return json;
    }

    @Override
    public String toString() {
        return toJson();
    }

    private void updateChannelInfo(List<ScanResult> scanResultList) {
        for (ScanResult scanResult : scanResultList) {
            ChannelInfo channelInfo = channelInfoMap.get(scanResult.frequency);
            if (channelInfo == null) {
                channelInfo = new ChannelInfo(scanResult.frequency);
                channelInfoMap.put(scanResult.frequency, channelInfo);
            }
            channelInfo.numberAPs++;
            if (isAPStrong(scanResult.level)) {
                channelInfo.numberStrongAPs++;
            }
            channelInfo.contentionMetric = (channelInfo.contentionMetric * MOVING_AVERAGE_DECAY_FACTOR +  computeContention(scanResult) * (100 - MOVING_AVERAGE_DECAY_FACTOR)) / 100;
        }
    }

    private boolean isAPStrong(int signal) {
        return (signal > SignalStrengthZones.HIGH);
    }

    private double getInterferenceFactor(int channel1, int channel2) {
        if (channel1 == channel2) {
            return 1;
        }
        return 0;
    }

    private double getInterferenceFromOtherChannels(int otherChannel) throws IllegalStateException {
        if (linkFrequency == 0) {
            throw new IllegalStateException("link frequency cannot be 0 for this operation");
        }
        ChannelInfo channelInfo = channelInfoMap.get(otherChannel);
        if (channelInfo == null) {
            return 0;
        }
        return channelInfo.contentionMetric * getInterferenceFactor(linkFrequency, otherChannel);
    }

    private void updateChannelFrequency(List<ScanResult> scanResultList) {
        if (linkSSID == null && linkBSSID == null) {
            return;
        }
        for (ScanResult scanResult: scanResultList) {
            if (scanResult.BSSID.equals(linkBSSID) || scanResult.SSID.equals(linkSSID)) {
                linkFrequency = scanResult.frequency;
                break;
            }
        }
    }

    private double computeContention(ScanResult scanResult) {
        if ((linkBSSID == null && linkSSID == null) || // not initialized
                scanResult.BSSID.equals(linkBSSID) || // We don't want current AP to contribute to contention.
                scanResult.SSID.equals(linkSSID) ) {
            return 0;
        }
        double contention = 0;
        if (Math.abs(scanResult.level - linkSignal) < SNR_BASE_GAP) {
            contention += 0.5;
        }

        if (scanResult.level > linkSignal) {
            contention += Math.pow((scanResult.level - linkSignal), 2) / SNR_MAX_POSITIVE_GAP_SQ;
        } else {
            contention += 1/Math.pow(Math.abs(scanResult.level - linkSignal), 1);
        }

        if (contention > 1) {
            contention = 1;
        }
        //return contention * getInterferenceFactor(scanResult.frequency, linkSignal);
        return contention;
    }



    public class ChannelInfo {
        public double channelNumber;
        public int numberAPs;
        public int numberStrongAPs;
        public double contentionMetric;

        public ChannelInfo(int channelNumber) {
            this.channelNumber = channelNumber;
        }

        public String toJson() {
            Gson gson = new Gson();
            String json = gson.toJson(this);
            return json;
        }
    }


    public class WifiStateInfo {
        public NetworkInfo.DetailedState detailedState;
        public long startTimestampMs;
        public long endTimestampMs;

        public WifiStateInfo(NetworkInfo.DetailedState detailedState,
                             long startTimestampMs, long endTimestampMs) {
            this.detailedState = detailedState;
            this.startTimestampMs = startTimestampMs;
            this.endTimestampMs = endTimestampMs;
        }

        public String toJson() {
            Gson gson = new Gson();
            String json = gson.toJson(this);
            return json;
        }
    }
}
