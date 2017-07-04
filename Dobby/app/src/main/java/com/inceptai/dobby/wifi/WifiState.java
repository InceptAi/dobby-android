package com.inceptai.dobby.wifi;

import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.inceptai.dobby.model.DobbyWifiInfo;
import com.inceptai.dobby.utils.DobbyLog;
import com.inceptai.dobby.utils.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by vivek on 4/8/17.
 */

public class WifiState {
    private static final int MIN_SIGNAL_CHANGE_FOR_CLEARING_CHANNEL_STATS = 20;
    private static final int SNR_BASE_GAP = 10;
    private static final int SNR_MAX_POSITIVE_GAP = 10;
    private static final int SNR_MAX_POSITIVE_GAP_SQ = SNR_MAX_POSITIVE_GAP * SNR_MAX_POSITIVE_GAP;
    private static final int MOVING_AVERAGE_DECAY_FACTOR = 20;
    private static final int THRESHOLD_FOR_DECLARING_CONNECTION_SETUP_STATE_AS_HANGING_MS = 5000;
    private static final int THRESHOLD_FOR_DECLARING_CONNECTION_SETUP_AS_DONE_IF_CONNECTED_FOR_MS = 10000;
    private static final int THRESHOLD_FOR_FLAGGING_FREQUENT_STATE_CHANGES = 5;
    private static final int THRESHOLD_FOR_COUNTING_FREQUENT_STATE_CHANGES_MS = 10000;
    private static final int MAX_AGE_FOR_SIGNAL_UPDATING_MS = 40000;
    private static final int ANDROID_INVALID_RSSI = -127;
    private static final String DEFAULT_MAC_ADDRESS = "02:00:00:00:00:00";

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
            WifiLinkMode.DISCONNECTED, WifiLinkMode.CONNECTING, WifiLinkMode.MAX_MODES})
    public @interface WifiLinkMode {
        int NO_PROBLEM_DEFAULT_STATE = 0;  // Connected and working normally.
        int HANGING_ON_DHCP = 1;
        int HANGING_ON_AUTHENTICATING = 2;
        int HANGING_ON_SCANNING = 3;
        int FREQUENT_DISCONNECTIONS = 4;
        int CONNECTING = 5;
        int DISCONNECTED = 6;
        int UNKNOWN = 7;
        int MAX_MODES = 8;
    }

    public static String wifiLinkModeToString(@WifiLinkMode int mode) {
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
            case WifiLinkMode.DISCONNECTED:
                return "DISCONNECTED";
            case WifiLinkMode.CONNECTING:
                return "CONNECTING";
            case WifiLinkMode.MAX_MODES:
                return "MAX_MODES";
            case WifiLinkMode.UNKNOWN:
            default:
                return "UNKNOWN";
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
    private HashMap<String, Integer> movingSignalAverage;
    private HashMap<String, Long> lastSeenSignalTimestamp;
    private List<WifiStateInfo> wifiStateTransitions;


    @WifiLinkMode
    private int wifiProblemMode;

    public class ChannelInfo {
        public int channelFrequency;
        int numberAPs;
        int similarStrengthAPs;
        int higherStrengthAps;
        int highestStrengthAps;
        int lowerStrengthAps;
        int lowestStrengthAps;
        double contentionMetric;

        ChannelInfo(int channelFrequency) {
            this.channelFrequency = channelFrequency;
        }

        public String toString() {
            return toJson();
        }

        String toJson() {
            Gson gson = new Gson();
            return gson.toJson(this);
        }

        public int getNumberAPs() {
            return numberAPs;
        }

        public int getNumberOfInterferingAPs() {
            return numberAPs;
        }
    }

    public int getWifiProblemMode() {
        return wifiProblemMode;
    }

    public void setCurrentWifiProblemMode(@WifiLinkMode int problemMode) {
        wifiProblemMode = problemMode;
    }

    public HashMap<Integer, Double> getContentionInformation() {
        HashMap<Integer, Double> channelMapToReturn = new HashMap<>();
        for (ChannelInfo channelInfo: channelInfoMap.values()) {
            channelMapToReturn.put(channelInfo.channelFrequency, channelInfo.contentionMetric);
        }
        return channelMapToReturn;
    }

    public HashMap<String, Utils.PercentileStats> getDetailedNetworkStateStats() {
        HashMap<String, Utils.PercentileStats> detailedStats = new HashMap<>();
        detailedStats.put(NetworkInfo.DetailedState.IDLE.name(), getStatsForDetailedState(NetworkInfo.DetailedState.IDLE));
        detailedStats.put(NetworkInfo.DetailedState.SCANNING.name(), getStatsForDetailedState(NetworkInfo.DetailedState.SCANNING));
        detailedStats.put(NetworkInfo.DetailedState.CONNECTING.name(), getStatsForDetailedState(NetworkInfo.DetailedState.CONNECTING));
        detailedStats.put(NetworkInfo.DetailedState.AUTHENTICATING.name(), getStatsForDetailedState(NetworkInfo.DetailedState.AUTHENTICATING));
        detailedStats.put(NetworkInfo.DetailedState.OBTAINING_IPADDR.name(), getStatsForDetailedState(NetworkInfo.DetailedState.OBTAINING_IPADDR));
        detailedStats.put(NetworkInfo.DetailedState.CONNECTED.name(), getStatsForDetailedState(NetworkInfo.DetailedState.CONNECTED));
        detailedStats.put(NetworkInfo.DetailedState.SUSPENDED.name(), getStatsForDetailedState(NetworkInfo.DetailedState.SUSPENDED));
        detailedStats.put(NetworkInfo.DetailedState.DISCONNECTING.name(), getStatsForDetailedState(NetworkInfo.DetailedState.DISCONNECTING));
        detailedStats.put(NetworkInfo.DetailedState.DISCONNECTED.name(), getStatsForDetailedState(NetworkInfo.DetailedState.DISCONNECTED));
        detailedStats.put(NetworkInfo.DetailedState.FAILED.name(), getStatsForDetailedState(NetworkInfo.DetailedState.FAILED));
        detailedStats.put(NetworkInfo.DetailedState.BLOCKED.name(), getStatsForDetailedState(NetworkInfo.DetailedState.BLOCKED));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            detailedStats.put(NetworkInfo.DetailedState.VERIFYING_POOR_LINK.name(), getStatsForDetailedState(NetworkInfo.DetailedState.VERIFYING_POOR_LINK));
            detailedStats.put(NetworkInfo.DetailedState.CAPTIVE_PORTAL_CHECK.name(), getStatsForDetailedState(NetworkInfo.DetailedState.CAPTIVE_PORTAL_CHECK));
        }
        return detailedStats;
    }

    public HashMap<Long, String> getWifiStateTransitionsList() {
        HashMap<Long, String> transitionsToReturn = new HashMap<>();
        //Sort the servers
        Collections.sort(wifiStateTransitions, new Comparator<WifiStateInfo>() {
            @Override
            public int compare(WifiStateInfo w1, WifiStateInfo w2) {
                return (int) (w1.startTimestampMs - w2.startTimestampMs);
            }
        });
        long lastStartTs = 0;
        if (wifiStateTransitions.size() > 0) {
            lastStartTs = wifiStateTransitions.get(0).startTimestampMs;
        }
        for (WifiStateInfo wifiStateInfo: wifiStateTransitions) {
            transitionsToReturn.put(wifiStateInfo.startTimestampMs - lastStartTs, wifiStateInfo.detailedState.name());
            lastStartTs = wifiStateInfo.startTimestampMs;
        }
        return transitionsToReturn;
    }

    private Utils.PercentileStats getStatsForDetailedState(NetworkInfo.DetailedState detailedState) {
        final long TIME_INTERVAL_FOR_DETAILED_STATS_MS = 15 * 60 * 1000;
        List<Double> duration = new ArrayList<>();
        if (detailedWifiStateStats == null || detailedWifiStateStats.get(detailedState) == null) {
            return new Utils.PercentileStats();
        }

        List<WifiStateInfo> list = detailedWifiStateStats.get(detailedState);
        for (WifiStateInfo wifiStateInfo: list) {
            if ((System.currentTimeMillis() - wifiStateInfo.endTimestampMs) < TIME_INTERVAL_FOR_DETAILED_STATS_MS) {
                duration.add((double)(wifiStateInfo.endTimestampMs - wifiStateInfo.startTimestampMs));
            }
        }
        return new Utils.PercentileStats(duration);
    }

    public void updateWifiStats(@Nullable DobbyWifiInfo wifiInfo, @Nullable List<ScanResult> scanResultList) {
        if (wifiInfo != null) {
            setLinkSSID(wifiInfo.getSSID());
            setLinkBSSID(wifiInfo.getBSSID());
            setLinkSignal(wifiInfo.getRssi());
            setMacAddress(wifiInfo.getMacAddress());
            setLinkSpeedMbps(wifiInfo.getLinkSpeed());
        }
        if (scanResultList != null) {
            if (linkFrequency == 0) {
                updateChannelFrequency(scanResultList);
            }
            updateWithScanResult(scanResultList);
        }
    }

    @Override
    public String toString() {
        return toJson();
    }

    public DobbyWifiInfo getLinkInfo(){
        return new DobbyWifiInfo(linkBSSID, linkSSID, macAddress,
                linkSignal, linkSpeedMbps, linkFrequency);
    }

    public HashMap<Integer, ChannelInfo> getChannelInfoMap() {
        final int GAP_FOR_SIMILAR_STRENGTH_DBM = 10;
        final int GAP_FOR_OTHER_STRENGTHS_DBM = 10;
        HashMap<Integer, ChannelInfo> infoToReturn = new HashMap<>();
        //Prepopulate the map with all channels
        //int[] channelList2GHz = Utils.get2GHzNonOverlappingChannelList();
        //int[] channelList2GHz = Utils.get2GHzChannelList();
        int[] channelList2GHzAnd5GHzCombined = Utils.get2GHzAnd5GHzChannelList();
        for (int channelIndex=0; channelIndex < channelList2GHzAnd5GHzCombined.length; channelIndex++) {
            ChannelInfo channelInfo = new ChannelInfo(channelList2GHzAnd5GHzCombined[channelIndex]);
            infoToReturn.put(channelList2GHzAnd5GHzCombined[channelIndex], channelInfo);
        }

        for (Map.Entry<String, Integer> entry : movingSignalAverage.entrySet()) {
            String freqBSSIDCombinedKey = entry.getKey();

            Integer freq = Utils.parseIntWithDefault(0, freqBSSIDCombinedKey.split("-")[0]);
            String BSSID = freqBSSIDCombinedKey.split("-")[1];

            ChannelInfo channelInfo = infoToReturn.get(freq);
            if (channelInfo == null) {
                channelInfo = new ChannelInfo(freq);
                infoToReturn.put(freq, channelInfo);
            }

            //If this is the primary AP, continue, we don't want to include it
            if ((BSSID.equals(linkBSSID) || BSSID.equals(linkSSID))) {
                continue;
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
            channelInfo.numberAPs++;
        }
        return infoToReturn;
    }

    public double getLastDurationForDetailedStateMs(NetworkInfo.DetailedState detailedState) {
        List<WifiStateInfo> list = detailedWifiStateStats.get(detailedState);
        if (list == null || list.isEmpty()) {
            return -1;
        }
        WifiStateInfo wifiStateInfo = list.get(list.size() -1);
        return wifiStateInfo.endTimestampMs - wifiStateInfo.startTimestampMs;
    }

    private void setLinkSignal(int linkSignal) {
        if (linkSignal != ANDROID_INVALID_RSSI) {
            DobbyLog.v("WS: Setting link signal from  " + this.linkSignal + " to " + linkSignal);
            this.linkSignal = linkSignal;
        }
    }

    private void setLinkSSID(String linkSSID) {
        if (linkSSID != null) {
            this.linkSSID = linkSSID;
        }
    }

    private void setLinkBSSID(String linkBSSID) {
        if (linkBSSID != null) {
            this.linkBSSID = linkBSSID;
        }
    }


    private void setMacAddress(String macAddress) {
        if (macAddress != null && !macAddress.equals(DEFAULT_MAC_ADDRESS)) {
            this.macAddress = macAddress;
        }
    }

    private void setLinkFrequency(int linkFrequency) {
        if (linkFrequency > 0) {
            this.linkFrequency = linkFrequency;
        }
    }

    private void setLinkSpeedMbps(int linkSpeedMbps) {
        if (linkSpeedMbps > 0) {
            this.linkSpeedMbps = linkSpeedMbps;
        }
    }

    public void setLastWifiState(NetworkInfo.DetailedState lastWifiState) {
        this.lastWifiState = lastWifiState;
    }

    public void setLastWifiStateTimestampMs(long lastWifiStateTimestampMs) {
        this.lastWifiStateTimestampMs = lastWifiStateTimestampMs;
    }

    //private stuff

    private class WifiStateInfo {
        NetworkInfo.DetailedState detailedState;
        long startTimestampMs;
        long endTimestampMs;

        WifiStateInfo(NetworkInfo.DetailedState detailedState,
                             long startTimestampMs, long endTimestampMs) {
            this.detailedState = detailedState;
            this.startTimestampMs = startTimestampMs;
            this.endTimestampMs = endTimestampMs;
        }

        WifiStateInfo(NetworkInfo.DetailedState detailedState, long startTimestampMs) {
            this.detailedState = detailedState;
            this.startTimestampMs = startTimestampMs;
        }

        String toJson() {
            Gson gson = new Gson();
            return gson.toJson(this);
        }
    }

    WifiState() {
        linkBSSID = Utils.EMPTY_STRING;
        linkSSID = Utils.EMPTY_STRING;
        channelInfoMap = new HashMap<>();
        detailedWifiStateStats = new HashMap<>();
        wifiProblemMode = WifiLinkMode.UNKNOWN;
        lastWifiState = NetworkInfo.DetailedState.IDLE;
        lastWifiStateTimestampMs = 0;
        movingSignalAverage = new HashMap<>();
        lastSeenSignalTimestamp = new HashMap<>();
        wifiStateTransitions = new ArrayList<>();
        wifiStateTransitions.add(new WifiStateInfo(lastWifiState, System.currentTimeMillis()));
    }

    void clearWifiConnectionInfo() {
        linkSSID = Utils.EMPTY_STRING;
        linkBSSID = Utils.EMPTY_STRING;
        linkFrequency = 0;
        linkSpeedMbps = 0;
        linkSignal = 0;
    }

    boolean updateSignal(int updatedSignal) {
        if (updatedSignal == 0) {
            return false;
        }
        if (Math.abs(updatedSignal - linkSignal) >= MIN_SIGNAL_CHANGE_FOR_CLEARING_CHANNEL_STATS) {
            channelInfoMap.clear();
            DobbyLog.v("WS: In updateSignal, new signal " + updatedSignal);
            setLinkSignal(updatedSignal);
            //TODO -- reissue a scan request.
            //Recompute the contention metric here
            //updateWithScanResult(lastWifiScanResult);
            return true;
        }
        return false;
    }

    @WifiLinkMode
    public int getCurrentWifiProblemMode() {
        long startTimeMs = getCurrentStateStartTimeMs();
        long currentTimeMs = System.currentTimeMillis();
        NetworkInfo.DetailedState currentState = getCurrentState();
        int numDisconnections = getNumberOfTimesWifiInState(NetworkInfo.DetailedState.DISCONNECTED);
        int numConnections = getNumberOfTimesWifiInState(NetworkInfo.DetailedState.CONNECTING);
        //If we are in CONNECTED mode now, then mark this as no issue and return
        if (getCurrentState() == NetworkInfo.DetailedState.CONNECTED) {
            wifiProblemMode = WifiLinkMode.NO_PROBLEM_DEFAULT_STATE;
            return wifiProblemMode;
        }
        if (currentTimeMs - startTimeMs >= THRESHOLD_FOR_DECLARING_CONNECTION_SETUP_STATE_AS_HANGING_MS) {
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
        } else if (Math.max(numDisconnections, numConnections) > THRESHOLD_FOR_FLAGGING_FREQUENT_STATE_CHANGES){
            //Check for frequent disconnections
            wifiProblemMode = WifiLinkMode.FREQUENT_DISCONNECTIONS;
        } else {
            switch (currentState) {
                case IDLE:
                case SCANNING:
                case DISCONNECTED:
                case DISCONNECTING:
                case FAILED:
                case BLOCKED:
                case SUSPENDED:
                    wifiProblemMode = WifiLinkMode.DISCONNECTED;
                    break;
                case CONNECTING:
                case AUTHENTICATING:
                case OBTAINING_IPADDR:
                case VERIFYING_POOR_LINK:
                case CAPTIVE_PORTAL_CHECK:
                    wifiProblemMode = WifiLinkMode.CONNECTING;
                    break;
            }
        }
        return wifiProblemMode;
    }

    @WifiLinkMode
    synchronized int updateDetailedWifiStateInfo(NetworkInfo.DetailedState detailedWifiState, long timestampMs) {
        if (lastWifiState != detailedWifiState) {
            wifiStateTransitions.add(new WifiStateInfo(detailedWifiState, System.currentTimeMillis()));
            if (lastWifiStateTimestampMs != 0) {
                WifiStateInfo lastWifiStateInfo = new WifiStateInfo(lastWifiState,
                        lastWifiStateTimestampMs, timestampMs);
                List<WifiStateInfo> currentList = detailedWifiStateStats.get(lastWifiState);
                if (currentList == null) {
                    currentList = new ArrayList<>();
                    detailedWifiStateStats.put(lastWifiState, currentList);
                }
                currentList.add(lastWifiStateInfo);
                DobbyLog.v("updateDetailedWifiStateInfo current state is: " +
                        detailedWifiState.name() + " last state,lasted: " +
                        lastWifiState.name() + "," + (timestampMs - lastWifiStateTimestampMs) + "ms");
            }
            DobbyLog.v("updateDetailedWifiStateInfo updating last wifi state from " + lastWifiState.name() + " to " + detailedWifiState.name());
            lastWifiState = detailedWifiState;
            lastWifiStateTimestampMs = timestampMs;
        } else if (lastWifiStateTimestampMs == 0) {
                lastWifiStateTimestampMs = timestampMs;
        }
        // Update the wifi problem mode if any
        return getCurrentWifiProblemMode();
        // printHashMap();
    }

    private int getNumberOfTimesWifiInState(NetworkInfo.DetailedState detailedState) {
        int count = 0;
        List<WifiStateInfo> list = detailedWifiStateStats.get(detailedState);
        if (list == null) {
            return 0;
        }
        for (WifiStateInfo wifiStateInfo: list) {
            if (wifiStateInfo.endTimestampMs >= (System.currentTimeMillis() - THRESHOLD_FOR_COUNTING_FREQUENT_STATE_CHANGES_MS)) {
                count++;
            }
        }
        return count;
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

    private String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    private void updateWithScanResult(List<ScanResult> scanResultList) {
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
            Long timestamp = lastSeenSignalTimestamp.get(keyForSignalUpdating);
            if (signal == null) {
                movingSignalAverage.put(keyForSignalUpdating, scanResult.level);
                lastSeenSignalTimestamp.put(keyForSignalUpdating, System.currentTimeMillis());
            } else {
                long currentTimestamp = System.currentTimeMillis();
                signal = Utils.computeMovingAverageSignal(scanResult.level, signal, currentTimestamp, timestamp, MAX_AGE_FOR_SIGNAL_UPDATING_MS);
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
        if (linkSSID.isEmpty() && linkBSSID.isEmpty()) {
            return;
        }
        for (ScanResult scanResult: scanResultList) {
            if (scanResult.BSSID.equals(linkBSSID) || scanResult.SSID.equals(linkSSID)) {
                setLinkFrequency(scanResult.frequency);
                break;
            }
        }
    }

    private double computeContention(ScanResult scanResult) {
        if ((linkBSSID.isEmpty() && linkSSID.isEmpty()) || // not initialized
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

    private void printHashMap() {
        for (Map.Entry<NetworkInfo.DetailedState, List<WifiStateInfo>> entry : detailedWifiStateStats.entrySet()) {
            NetworkInfo.DetailedState key = entry.getKey();
            List<WifiStateInfo> wifiStateInfos = entry.getValue();
            StringBuilder sb = new StringBuilder();
            for (WifiStateInfo wifiStateInfo: wifiStateInfos) {
                sb.append(wifiStateInfo.toJson());
            }
            DobbyLog.v("updateDetailedWifiStateInfo4 Key: " + key.name() + " value: " + sb.toString());
        }
    }
}
