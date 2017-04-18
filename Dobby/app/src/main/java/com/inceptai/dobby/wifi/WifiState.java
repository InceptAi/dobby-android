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

public class WifiState {
    private static final int MIN_SIGNAL_CHANGE_FOR_CLEARING_CHANNEL_STATS = 20;
    private static final int SNR_BASE_GAP = 10;
    private static final int SNR_MAX_POSITIVE_GAP = 10;
    private static final int SNR_MAX_POSITIVE_GAP_SQ = SNR_MAX_POSITIVE_GAP * SNR_MAX_POSITIVE_GAP;
    private static final int MOVING_AVERAGE_DECAY_FACTOR = 20;
    private static final int THRESHOLD_FOR_DECLARING_CONNECTION_SETUP_STATE_AS_HANGING_MS = 10000;
    private static final int THRESHOLD_FOR_FLAGGING_FREQUENT_STATE_CHANGES = 5;
    private static final int THRESHOLD_FOR_COUNTING_FREQUENT_STATE_CHANGES_MS = 10000;


    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SignalStrengthZones.HIGH, SignalStrengthZones.MEDIUM,
            SignalStrengthZones.LOW, SignalStrengthZones.FRINGE})
    public @interface SignalStrengthZones {
        int HIGH  = -60;
        int MEDIUM = -80;
        int LOW = -100;
        int FRINGE = -140;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({WifiLinkMode.NO_PROBLEM_DEFAULT_STATE, WifiLinkMode.HANGING_ON_DHCP,
            WifiLinkMode.HANGING_ON_AUTHENTICATING, WifiLinkMode.HANGING_ON_SCANNING,
            WifiLinkMode.FREQUENT_DISCONNECTIONS, WifiLinkMode.UNKNOWN,
            WifiLinkMode.MAX_MODES})
    public @interface WifiLinkMode {
        int NO_PROBLEM_DEFAULT_STATE = 0;  // Connected and working normally.
        int HANGING_ON_DHCP = 1;
        int HANGING_ON_AUTHENTICATING = 2;
        int HANGING_ON_SCANNING = 3;
        int FREQUENT_DISCONNECTIONS = 4;
        int UNKNOWN = 5;
        int MAX_MODES = 6;
    }

    public static String getWifiStatsModeName(@WifiLinkMode int mode) {
        switch (mode) {
            case WifiLinkMode.NO_PROBLEM_DEFAULT_STATE:
                return "NO_PROBLEM_DEFAULT_STATE";
            case WifiLinkMode.HANGING_ON_DHCP:
                return "HANGING_ON_DHCP";
            case WifiLinkMode.HANGING_ON_AUTHENTICATING:
                return "HANGING_ON_AUTHENTICATING";
            case WifiLinkMode.HANGING_ON_SCANNING:
                return "HANGING_ON_SCANNING";
            case WifiLinkMode.FREQUENT_DISCONNECTIONS:
                return "FREQUENT_DISCONNECTIONS";
            default:
                return "Unknown";
        }
    }


    private String linkSSID;
    private String linkBSSID;
    private String macAddress;
    private int linkSignal;
    private int linkFrequency;
    private int linkSpeedMbps;

    private NetworkInfo.DetailedState lastWifiState;
    private long lastWifiStateTimestampMs;
    private HashMap<Integer, ChannelInfo> channelInfoMap;
    private HashMap<NetworkInfo.DetailedState, List<WifiStateInfo>> detailedWifiStateStats;
    private List<ScanResult> lastWifiScanResult;
    private HashMap<String, Integer> movingSignalAverage;
    private HashMap<String, Long> lastSeenSignalTimestamp;

    @WifiLinkMode
    private int wifiProblemMode;


    public WifiState() {
        channelInfoMap = new HashMap<>();
        detailedWifiStateStats = new HashMap<>();
        wifiProblemMode = WifiLinkMode.NO_PROBLEM_DEFAULT_STATE;
        lastWifiState = NetworkInfo.DetailedState.IDLE;
        lastWifiStateTimestampMs = 0;
        movingSignalAverage = new HashMap<>();
        lastSeenSignalTimestamp = new HashMap<>();
    }

    public void clearWifiConnectionInfo() {
        linkSSID = null;
        linkBSSID = null;
        linkFrequency = 0;
        linkSpeedMbps = 0;
        linkSignal = 0;
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
            updateInfoWithScanResult(lastWifiScanResult);
            return true;
        }
        return false;
    }

    @WifiLinkMode
    public int getCurrentWifiProblemMode() {
        return wifiProblemMode;
    }

    public HashMap<Integer, Double> getContentionInformation() {
        HashMap<Integer, Double> channelMapToReturn = new HashMap<>();
        for (ChannelInfo channelInfo: channelInfoMap.values()) {
            channelMapToReturn.put(channelInfo.channelFrequency, channelInfo.contentionMetric);
        }
        return channelMapToReturn;
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

    @WifiLinkMode
    private int updateWifiProblemMode() {
        long startTimeMs = getCurrentStateStartTimeMs();
        if (System.currentTimeMillis() - startTimeMs >= THRESHOLD_FOR_DECLARING_CONNECTION_SETUP_STATE_AS_HANGING_MS) {
            NetworkInfo.DetailedState currentState = getCurrentState();
            currentState.name();
            switch(currentState) {
                case SCANNING:
                    wifiProblemMode = WifiLinkMode.HANGING_ON_SCANNING;
                    break;
                case AUTHENTICATING:
                    wifiProblemMode = WifiLinkMode.HANGING_ON_AUTHENTICATING;
                    break;
                case OBTAINING_IPADDR:
                    wifiProblemMode = WifiLinkMode.HANGING_ON_DHCP;
                    break;
            }
        } else if (getNumberOfTimesWifiInState(NetworkInfo.DetailedState.DISCONNECTED,
                THRESHOLD_FOR_COUNTING_FREQUENT_STATE_CHANGES_MS) > THRESHOLD_FOR_FLAGGING_FREQUENT_STATE_CHANGES){
            //Check for frequenct disconnections
            wifiProblemMode = WifiLinkMode.FREQUENT_DISCONNECTIONS;
        }
        return wifiProblemMode;
    }

    @WifiLinkMode
    synchronized protected int updateDetailedWifiStateInfo(NetworkInfo.DetailedState detailedWifiState, long timestampMs) {
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
        //Update the wifi problem mode if any
        return updateWifiProblemMode();
        //printHashMap();
    }

    private int getNumberOfTimesWifiInState(NetworkInfo.DetailedState detailedState, long timeIntervalMs) {
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

    private NetworkInfo.DetailedState getCurrentState() {
        return lastWifiState;
    }

    private long getCurrentStateStartTimeMs() {
        return lastWifiStateTimestampMs;
    }

    private HashMap<NetworkInfo.DetailedState, List<WifiStateInfo>> getStatsHashMap() {
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
            updateInfoWithScanResult(scanResultList);
        }
    }

    private String toJson() {
        Gson gson = new Gson();
        String json = gson.toJson(this);
        return json;
    }

    @Override
    public String toString() {
        return toJson();
    }


    public HashMap<Integer, ChannelInfo> getChannelStats() {
        final int GAP_FOR_SIMILAR_STRENGTH_DBM = 5;
        final int GAP_FOR_OTHER_STRENGTHS_DBM = 10;
        HashMap<Integer, ChannelInfo> infoToReturn = new HashMap<>();
        for (Map.Entry<String, Integer> entry : movingSignalAverage.entrySet()) {
            String freqBSSIDCombinedKey = entry.getKey();

            Integer freq = Utils.parseIntWithDefault(0, freqBSSIDCombinedKey.split("-")[0]);
            ChannelInfo channelInfo = infoToReturn.get(freq);
            if (channelInfo == null) {
                channelInfo = new ChannelInfo(freq);
                infoToReturn.put(freq, channelInfo);
            }

            Integer signalValue = entry.getValue();
            if (signalValue < linkSignal + GAP_FOR_SIMILAR_STRENGTH_DBM &&
                    signalValue >= linkSignal - GAP_FOR_SIMILAR_STRENGTH_DBM) {
                channelInfo.similarStrengthAPs++;
            } else if (signalValue < linkSignal + GAP_FOR_SIMILAR_STRENGTH_DBM + GAP_FOR_OTHER_STRENGTHS_DBM &&
                    signalValue >= linkSignal + GAP_FOR_SIMILAR_STRENGTH_DBM) {
                channelInfo.higherStrengthAps++;
            } else if (signalValue >= linkSignal + GAP_FOR_SIMILAR_STRENGTH_DBM + GAP_FOR_OTHER_STRENGTHS_DBM) {
                channelInfo.highestStrengthAps++;
            } else if (signalValue < linkSignal - GAP_FOR_SIMILAR_STRENGTH_DBM &&
                    signalValue > linkSignal - GAP_FOR_SIMILAR_STRENGTH_DBM - GAP_FOR_OTHER_STRENGTHS_DBM) {
                channelInfo.lowerStrengthAps++;
            } else if (signalValue <= linkSignal - GAP_FOR_SIMILAR_STRENGTH_DBM - GAP_FOR_OTHER_STRENGTHS_DBM) {
                channelInfo.lowestStrengthAps++;
            }
        }
        return infoToReturn;
    }


    public int averageSignal(int currentSignal, int previousSignal, long currentSeen, long previousSeen, int maxAge) {
        if (currentSeen == 0) {
            currentSeen = System.currentTimeMillis();
        }
        long age = currentSeen - previousSeen;
        if (previousSeen > 0 && age > 0 && age < maxAge/2) {
            // Average the RSSI with previously seen instances of this scan result
            double alpha = 0.5 - (double) age / (double) maxAge;
            currentSignal = (int) ((double) currentSignal * (1 - alpha) + (double) previousSignal * alpha);
        }
        return currentSignal;
    }


    private void updateInfoWithScanResult(List<ScanResult> scanResultList) {
        final int MAX_AGE_FOR_SIGNAL_UPDATING_MS = 40000;
        for (ScanResult scanResult : scanResultList) {
            //Update the channel info stuff
            ChannelInfo channelInfo = channelInfoMap.get(scanResult.frequency);
            if (channelInfo == null) {
                channelInfo = new ChannelInfo(scanResult.frequency);
                channelInfoMap.put(scanResult.frequency, channelInfo);
            }
            channelInfo.numberAPs++;
            if (isAPStrong(scanResult.level)) {
                channelInfo.highestStrengthAps++;
            }
            channelInfo.contentionMetric = (channelInfo.contentionMetric * MOVING_AVERAGE_DECAY_FACTOR +  computeContention(scanResult) * (100 - MOVING_AVERAGE_DECAY_FACTOR)) / 100;

            //Update the snr stuff
            String keyForSignalUpdating = scanResult.frequency + "-" + scanResult.BSSID;
            Integer signal = movingSignalAverage.get(keyForSignalUpdating);
            long timestamp = lastSeenSignalTimestamp.get(keyForSignalUpdating);
            if (signal == null) {
                movingSignalAverage.put(keyForSignalUpdating, scanResult.level);
                lastSeenSignalTimestamp.put(keyForSignalUpdating, System.currentTimeMillis());
            } else {
                long currentTimestamp = System.currentTimeMillis();
                signal = averageSignal(scanResult.level, signal, currentTimestamp, timestamp, MAX_AGE_FOR_SIGNAL_UPDATING_MS);
                movingSignalAverage.put(keyForSignalUpdating, signal);
                lastSeenSignalTimestamp.put(keyForSignalUpdating, currentTimestamp);
            }
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
        public int channelFrequency;
        public int numberAPs;
        public int similarStrengthAPs;
        public int higherStrengthAps;
        public int highestStrengthAps;
        public int lowerStrengthAps;
        public int lowestStrengthAps;
        public double contentionMetric;

        public ChannelInfo(int channelFrequency) {
            this.channelFrequency = channelFrequency;
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
