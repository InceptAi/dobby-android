package com.inceptai.dobby.service.utils;

import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.IntDef;

import com.google.gson.Gson;
import com.inceptai.actionlibrary.utils.ActionLog;
import com.inceptai.dobby.utils.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by vivek on 4/8/17.
 */

public class WifiStateData {
    public static final int ANDROID_INVALID_RSSI = -127;
    private static final int THRESHOLD_FOR_DECLARING_CONNECTION_SETUP_STATE_AS_HANGING_MS = 5000;
    private static final int THRESHOLD_FOR_FLAGGING_FREQUENT_STATE_CHANGES = 5;
    private static final int THRESHOLD_FOR_COUNTING_FREQUENT_STATE_CHANGES_MS = 10000;
    private static final int MAX_AGE_FOR_SIGNAL_UPDATING_MS = 40000;
    private static final int MIN_SNR_FOR_FLAGGING_POOR_CONNECTION_DBM = -90;
    private static final String DEFAULT_MAC_ADDRESS = "02:00:00:00:00:00";
    private static final int SUPPLICANT_LIST_LENGTH = 10;

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
    @IntDef({WifiProblemMode.NO_PROBLEM_DEFAULT_STATE,
            WifiProblemMode.HANGING_ON_DHCP,
            WifiProblemMode.HANGING_ON_AUTHENTICATING,
            WifiProblemMode.HANGING_ON_SCANNING,
            WifiProblemMode.FREQUENT_DISCONNECTIONS,
            WifiProblemMode.UNKNOWN,
            WifiProblemMode.HANGING_ON_CONNECTING,
            WifiProblemMode.DISCONNECTED,
            WifiProblemMode.CONNECTING,
            WifiProblemMode.LOW_SNR,
            WifiProblemMode.PROBLEMATIC_SUPPLICANT_PATTERN,
            WifiProblemMode.ERROR_AUTHENTICATING,
            WifiProblemMode.MAX_MODES})
    public @interface WifiProblemMode {
        int NO_PROBLEM_DEFAULT_STATE = 0;  // Connected and working normally.
        int HANGING_ON_DHCP = 1;
        int HANGING_ON_AUTHENTICATING = 2;
        int HANGING_ON_SCANNING = 3;
        int FREQUENT_DISCONNECTIONS = 4;
        int HANGING_ON_CONNECTING = 5;
        int CONNECTING = 6;
        int DISCONNECTED = 7;
        int LOW_SNR = 8;
        int PROBLEMATIC_SUPPLICANT_PATTERN = 9;
        int ERROR_AUTHENTICATING = 10;
        int UNKNOWN = 11;
        int MAX_MODES = 12;
    }

    public static String wifiLinkModeToString(@WifiProblemMode int mode) {
        switch (mode) {
            case WifiProblemMode.NO_PROBLEM_DEFAULT_STATE:
                return "NO_PROBLEM_DEFAULT_STATE";
            case WifiProblemMode.HANGING_ON_DHCP:
                return "HANGING_ON_DHCP";
            case WifiProblemMode.HANGING_ON_AUTHENTICATING:
                return "HANGING_ON_AUTHENTICATING";
            case WifiProblemMode.HANGING_ON_SCANNING:
                return "HANGING_ON_SCANNING";
            case WifiProblemMode.HANGING_ON_CONNECTING:
                return "HANGING_ON_CONNECTING";
            case WifiProblemMode.FREQUENT_DISCONNECTIONS:
                return "FREQUENT_DISCONNECTIONS";
            case WifiProblemMode.DISCONNECTED:
                return "DISCONNECTED";
            case WifiProblemMode.CONNECTING:
                return "CONNECTING";
            case WifiProblemMode.MAX_MODES:
                return "MAX_MODES";
            case WifiProblemMode.LOW_SNR:
                return "LOW_SNR";
            case WifiProblemMode.PROBLEMATIC_SUPPLICANT_PATTERN:
                return "PROBLEMATIC_SUPPLICANT_PATTERN";
            case WifiProblemMode.ERROR_AUTHENTICATING:
                return "ERROR_AUTHENTICATING";
            case WifiProblemMode.UNKNOWN:
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
    private long lastLinkSignalSeenTimestampMs;

    private int lastWifiState;
    private long lastDetailedWifiStateTimestampMs;
    private HashMap<NetworkInfo.DetailedState, List<DetailedWifiStateInfo>> detailedWifiStateStats;
    private HashMap<String, Integer> movingSignalAverage;
    private HashMap<String, Long> lastSeenSignalTimestamp;
    private List<DetailedWifiStateInfo> wifiStateTransitions;
    private SupplicantState lastSupplicantState;
    private FifoList supplicantStateList;

    private int lastWifiEnabledState;
    private NetworkInfo.DetailedState lastWifiDetailedState;
    private NetworkInfo.State lastWifiConnectionState;

    @WifiProblemMode
    private int wifiProblemMode;

    public WifiStateData() {
        //primary link stats
        linkBSSID = Utils.EMPTY_STRING;
        linkSSID = Utils.EMPTY_STRING;
        macAddress = Utils.EMPTY_STRING;
        lastLinkSignalSeenTimestampMs = 0;

        //Other state stats
        wifiProblemMode = WifiProblemMode.UNKNOWN;
        lastWifiState = WifiManager.WIFI_STATE_UNKNOWN;
        lastSupplicantState = SupplicantState.UNINITIALIZED;
        lastDetailedWifiStateTimestampMs = 0;
        lastWifiDetailedState = NetworkInfo.DetailedState.IDLE;

        detailedWifiStateStats = new HashMap<>();
        movingSignalAverage = new HashMap<>();
        lastSeenSignalTimestamp = new HashMap<>();
        wifiStateTransitions = new ArrayList<>();
        wifiStateTransitions.add(new DetailedWifiStateInfo(lastWifiDetailedState, System.currentTimeMillis()));
        supplicantStateList = new FifoList(SUPPLICANT_LIST_LENGTH);
    }

    @WifiProblemMode
    public int updateSignal(int updatedSignal) {
        if (updatedSignal == 0 || updatedSignal == ANDROID_INVALID_RSSI) {
            return WifiProblemMode.UNKNOWN;
        }
        @WifiProblemMode int linkMode = WifiProblemMode.NO_PROBLEM_DEFAULT_STATE;
        long currentTimestamp = System.currentTimeMillis();
        if (linkSignal == 0) {
            linkSignal = updatedSignal;
        } else {
            linkSignal = Utils.computeMovingAverageSignal(updatedSignal, linkSignal, currentTimestamp, lastLinkSignalSeenTimestampMs, MAX_AGE_FOR_SIGNAL_UPDATING_MS);
        }
        if (linkSignal < MIN_SNR_FOR_FLAGGING_POOR_CONNECTION_DBM) {
            linkMode = WifiProblemMode.LOW_SNR;
        }
        return linkMode;
    }

    public int updateLastWifiEnabledState(int newWifiEnabledState) {
        lastWifiEnabledState = newWifiEnabledState;
        return lastWifiEnabledState;
    }

    @WifiProblemMode
    public int updateWifiDetailedState(NetworkInfo.DetailedState newDetailedState) {
        if (newDetailedState == NetworkInfo.DetailedState.DISCONNECTED) {
            clearWifiConnectionInfo();
        }
        return updateDetailedWifiStateInfo(newDetailedState);
    }

    public void updateWifiConnectionState(NetworkInfo.State newConnectionState) {
        lastWifiConnectionState = newConnectionState;
    }

    @WifiProblemMode
    public int updateSupplicantState(SupplicantState supplicantState, int supplicantError)  {
        @WifiProblemMode int problemMode = WifiProblemMode.UNKNOWN;
        supplicantStateList.add(supplicantState);
        if (checkForProblematicSupplicationPattern()) {
            supplicantStateList.clear();
            problemMode = WifiProblemMode.PROBLEMATIC_SUPPLICANT_PATTERN;
        } else if (supplicantError == WifiManager.ERROR_AUTHENTICATING) {
            problemMode = WifiProblemMode.ERROR_AUTHENTICATING;
        }
        return problemMode;
    }

    //private methods

    private boolean checkForProblematicSupplicationPattern() {
        SupplicantPatterns.SupplicantPattern pattern =
                supplicantStateList.containsPatterns(SupplicantPatterns.getSupplicantPatterns());
        return (pattern != null);
    }

    private Utils.PercentileStats getStatsForDetailedState(NetworkInfo.DetailedState detailedState) {
        final long TIME_INTERVAL_FOR_DETAILED_STATS_MS = 15 * 60 * 1000;
        List<Double> duration = new ArrayList<>();
        if (detailedWifiStateStats == null || detailedWifiStateStats.get(detailedState) == null) {
            return new Utils.PercentileStats();
        }

        List<DetailedWifiStateInfo> list = detailedWifiStateStats.get(detailedState);
        for (DetailedWifiStateInfo detailedWifiStateInfo : list) {
            if ((System.currentTimeMillis() - detailedWifiStateInfo.endTimestampMs) < TIME_INTERVAL_FOR_DETAILED_STATS_MS) {
                duration.add((double)(detailedWifiStateInfo.endTimestampMs - detailedWifiStateInfo.startTimestampMs));
            }
        }
        return new Utils.PercentileStats(duration);
    }

    public void updatePrimaryLinkInfo(WifiInfo wifiInfo) {
        if (wifiInfo != null) {
            setLinkSSID(wifiInfo.getSSID());
            setLinkBSSID(wifiInfo.getBSSID());
            setLinkSignal(wifiInfo.getRssi());
            setMacAddress(wifiInfo.getMacAddress());
            setLinkSpeedMbps(wifiInfo.getLinkSpeed());
        }
    }

    private double getLastDurationForDetailedStateMs(NetworkInfo.DetailedState detailedState) {
        List<DetailedWifiStateInfo> list = detailedWifiStateStats.get(detailedState);
        if (list == null || list.isEmpty()) {
            return -1;
        }
        DetailedWifiStateInfo detailedWifiStateInfo = list.get(list.size() -1);
        return detailedWifiStateInfo.endTimestampMs - detailedWifiStateInfo.startTimestampMs;
    }

    private void setLinkSignal(int linkSignal) {
        if (linkSignal != ANDROID_INVALID_RSSI) {
            ActionLog.v("WS: Setting link signal from  " + this.linkSignal + " to " + linkSignal);
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

    //private stuff

    private class DetailedWifiStateInfo {
        NetworkInfo.DetailedState detailedState;
        long startTimestampMs;
        long endTimestampMs;

        DetailedWifiStateInfo(NetworkInfo.DetailedState detailedState,
                              long startTimestampMs, long endTimestampMs) {
            this.detailedState = detailedState;
            this.startTimestampMs = startTimestampMs;
            this.endTimestampMs = endTimestampMs;
        }

        DetailedWifiStateInfo(NetworkInfo.DetailedState detailedState, long startTimestampMs) {
            this.detailedState = detailedState;
            this.startTimestampMs = startTimestampMs;
        }

        String toJson() {
            Gson gson = new Gson();
            return gson.toJson(this);
        }
    }

    private void clearWifiConnectionInfo() {
        linkSSID = Utils.EMPTY_STRING;
        linkBSSID = Utils.EMPTY_STRING;
        linkFrequency = 0;
        linkSpeedMbps = 0;
        linkSignal = 0;
        lastLinkSignalSeenTimestampMs = 0;
    }

    @WifiProblemMode
    private int getCurrentWifiProblemMode() {
        long startTimeMs = getCurrentStateStartTimeMs();
        long currentTimeMs = System.currentTimeMillis();
        NetworkInfo.DetailedState currentState = getCurrentState();
        int numDisconnections = getNumberOfTimesWifiInState(NetworkInfo.DetailedState.DISCONNECTED);
        int numConnections = getNumberOfTimesWifiInState(NetworkInfo.DetailedState.CONNECTING);
        //If we are in CONNECTED mode now, then mark this as no issue and return
        if (getCurrentState() == NetworkInfo.DetailedState.CONNECTED) {
            wifiProblemMode = WifiProblemMode.NO_PROBLEM_DEFAULT_STATE;
            return wifiProblemMode;
        }
        if (currentTimeMs - startTimeMs >= THRESHOLD_FOR_DECLARING_CONNECTION_SETUP_STATE_AS_HANGING_MS) {
            switch(currentState) {
                case SCANNING:
                    wifiProblemMode = WifiProblemMode.HANGING_ON_SCANNING;
                    break;
                case AUTHENTICATING:
                    wifiProblemMode = WifiProblemMode.HANGING_ON_AUTHENTICATING;
                    break;
                case OBTAINING_IPADDR:
                    wifiProblemMode = WifiProblemMode.HANGING_ON_DHCP;
                    break;
            }
        } else if (Math.max(numDisconnections, numConnections) > THRESHOLD_FOR_FLAGGING_FREQUENT_STATE_CHANGES){
            //Check for frequent disconnections
            wifiProblemMode = WifiProblemMode.FREQUENT_DISCONNECTIONS;
        } else {
            switch (currentState) {
                case IDLE:
                case SCANNING:
                case DISCONNECTED:
                case DISCONNECTING:
                case FAILED:
                case BLOCKED:
                case SUSPENDED:
                    wifiProblemMode = WifiProblemMode.DISCONNECTED;
                    break;
                case CONNECTING:
                case AUTHENTICATING:
                case OBTAINING_IPADDR:
                case VERIFYING_POOR_LINK:
                case CAPTIVE_PORTAL_CHECK:
                    wifiProblemMode = WifiProblemMode.CONNECTING;
                    break;
            }
        }
        return wifiProblemMode;
    }

    @WifiProblemMode
    synchronized private int updateDetailedWifiStateInfo(NetworkInfo.DetailedState detailedWifiState) {
        long currentTimestampMs = System.currentTimeMillis();
        if (lastWifiDetailedState != detailedWifiState) {
            wifiStateTransitions.add(new DetailedWifiStateInfo(detailedWifiState, System.currentTimeMillis()));
            if (lastDetailedWifiStateTimestampMs != 0) {
                DetailedWifiStateInfo lastDetailedWifiStateInfo = new DetailedWifiStateInfo(lastWifiDetailedState,
                        lastDetailedWifiStateTimestampMs, currentTimestampMs);
                List<DetailedWifiStateInfo> currentList = detailedWifiStateStats.get(lastWifiDetailedState);
                if (currentList == null) {
                    currentList = new ArrayList<>();
                    detailedWifiStateStats.put(lastWifiDetailedState, currentList);
                }
                currentList.add(lastDetailedWifiStateInfo);
                ActionLog.v("updateDetailedWifiStateInfo current state is: " +
                        detailedWifiState.name() + " last state,lasted: " +
                        lastWifiDetailedState.name() + "," + (currentTimestampMs - lastDetailedWifiStateTimestampMs) + "ms");
            }
            ActionLog.v("updateDetailedWifiStateInfo updating last wifi state from " + lastWifiDetailedState.name() + " to " + detailedWifiState.name());
            lastDetailedWifiStateTimestampMs = currentTimestampMs;
        } else if (lastDetailedWifiStateTimestampMs == 0) {
                lastDetailedWifiStateTimestampMs = currentTimestampMs;
        }
        lastWifiDetailedState = detailedWifiState;
        // Update the wifi problem mode if any
        return getCurrentWifiProblemMode();
    }

    private int getNumberOfTimesWifiInState(NetworkInfo.DetailedState detailedState) {
        int count = 0;
        List<DetailedWifiStateInfo> list = detailedWifiStateStats.get(detailedState);
        if (list == null) {
            return 0;
        }
        for (DetailedWifiStateInfo detailedWifiStateInfo : list) {
            if (detailedWifiStateInfo.endTimestampMs >= (System.currentTimeMillis() - THRESHOLD_FOR_COUNTING_FREQUENT_STATE_CHANGES_MS)) {
                count++;
            }
        }
        return count;
    }

    private NetworkInfo.DetailedState getCurrentState() {
        return lastWifiDetailedState;
    }

    private long getCurrentStateStartTimeMs() {
        return lastDetailedWifiStateTimestampMs;
    }
}
