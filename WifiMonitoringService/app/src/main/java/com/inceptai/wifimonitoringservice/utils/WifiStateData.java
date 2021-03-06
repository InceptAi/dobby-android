package com.inceptai.wifimonitoringservice.utils;

import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.IntDef;

import com.google.gson.Gson;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.inceptai.wifimonitoringservice.utils.WifiStateData.SignalStrengthZones.FRINGE;
import static com.inceptai.wifimonitoringservice.utils.WifiStateData.SignalStrengthZones.HIGH;
import static com.inceptai.wifimonitoringservice.utils.WifiStateData.SignalStrengthZones.LOW;
import static com.inceptai.wifimonitoringservice.utils.WifiStateData.SignalStrengthZones.MEDIUM;
import static com.inceptai.wifimonitoringservice.utils.WifiStateData.SignalStrengthZones.UNKNOWN;

/**
 * Created by vivek on 4/8/17.
 */

public class WifiStateData {
    public static final int ANDROID_INVALID_RSSI = -127;
    private static final int THRESHOLD_FOR_DECLARING_CONNECTION_SETUP_STATE_AS_HANGING_MS = 10000;
    private static final int THRESHOLD_FOR_FLAGGING_FREQUENT_STATE_CHANGES = 5;
    private static final int THRESHOLD_FOR_COUNTING_FREQUENT_STATE_CHANGES_MS = 60 * 1000;
    private static final int MAX_AGE_FOR_SIGNAL_UPDATING_MS = 40000;
    private static final int MIN_SNR_FOR_FLAGGING_POOR_CONNECTION_DBM = -90;
    private static final String DEFAULT_MAC_ADDRESS = "02:00:00:00:00:00";
    private static final int SUPPLICANT_LIST_LENGTH = 10;
    private static final String EMPTY_STRING = "";

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({HIGH, MEDIUM,
            LOW, FRINGE,
            UNKNOWN})
    public @interface SignalStrengthZones {
        int HIGH  = -60;
        int MEDIUM = -80;
        int LOW = -100;
        int FRINGE = -140;
        int UNKNOWN = 0;
    }

    private static String getStringFromZone(@SignalStrengthZones int zone) {
        switch (zone) {
            case HIGH:
                return "Excellent";
            case MEDIUM:
                return "Ok";
            case LOW:
                return "Low";
            case FRINGE:
                return "Poor";
            case UNKNOWN:
            default:
                return "Unknown";
        }
    }

    @SignalStrengthZones
    private static int getZoneFromSignalStrength(int signal) {
        if (signal == 0) {
            return SignalStrengthZones.UNKNOWN;
        } else if (signal > SignalStrengthZones.HIGH) {
            return SignalStrengthZones.HIGH;
        } else if (signal > SignalStrengthZones.MEDIUM) {
            return SignalStrengthZones.MEDIUM;
        } else if (signal > SignalStrengthZones.LOW) {
            return SignalStrengthZones.LOW;
        } else {
            return SignalStrengthZones.FRINGE;
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({WifiProblemMode.NO_PROBLEM_DEFAULT_STATE,
            WifiProblemMode.HANGING_ON_DHCP,
            WifiProblemMode.HANGING_ON_AUTHENTICATING,
            WifiProblemMode.HANGING_ON_SCANNING,
            WifiProblemMode.FREQUENT_DISCONNECTIONS,
            WifiProblemMode.UNINITIALIZED,
            WifiProblemMode.HANGING_ON_CONNECTING,
            WifiProblemMode.DISCONNECTED_PREMATURELY,
            WifiProblemMode.CONNECTING,
            WifiProblemMode.LOW_SNR,
            WifiProblemMode.PROBLEMATIC_SUPPLICANT_PATTERN,
            WifiProblemMode.INACTIVE_OR_DORMANT_SUPPLICANT_STATE,
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
        int DISCONNECTED_PREMATURELY = 7;
        int LOW_SNR = 8;
        int PROBLEMATIC_SUPPLICANT_PATTERN = 9;
        int INACTIVE_OR_DORMANT_SUPPLICANT_STATE = 10;
        int ERROR_AUTHENTICATING = 11;
        int UNINITIALIZED = 12;
        int MAX_MODES = 13;
    }

    public static boolean isWifiInAnyProblemMode(@WifiProblemMode int mode) {
        return mode != WifiProblemMode.NO_PROBLEM_DEFAULT_STATE;
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
            case WifiProblemMode.DISCONNECTED_PREMATURELY:
                return "DISCONNECTED_PREMATURELY";
            case WifiProblemMode.CONNECTING:
                return "CONNECTING";
            case WifiProblemMode.MAX_MODES:
                return "MAX_MODES";
            case WifiProblemMode.LOW_SNR:
                return "LOW_SNR";
            case WifiProblemMode.PROBLEMATIC_SUPPLICANT_PATTERN:
                return "PROBLEMATIC_SUPPLICANT_PATTERN";
            case WifiProblemMode.INACTIVE_OR_DORMANT_SUPPLICANT_STATE:
                return "INACTIVE_OR_DORMANT_SUPPLICANT_STATE";
            case WifiProblemMode.ERROR_AUTHENTICATING:
                return "ERROR_AUTHENTICATING";
            case WifiProblemMode.UNINITIALIZED:
            default:
                return "UNINITIALIZED";
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
        linkBSSID = EMPTY_STRING;
        linkSSID = EMPTY_STRING;
        macAddress = EMPTY_STRING;
        lastLinkSignalSeenTimestampMs = 0;

        //Other state stats
        wifiProblemMode = WifiProblemMode.UNINITIALIZED;
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
            return WifiProblemMode.UNINITIALIZED;
        }
        @WifiProblemMode int linkMode = WifiProblemMode.NO_PROBLEM_DEFAULT_STATE;
        long currentTimestamp = System.currentTimeMillis();
        @SignalStrengthZones int lastSignalZone = getZoneFromSignalStrength(linkSignal);
        if (linkSignal == 0) {
            linkSignal = updatedSignal;
        } else {
            linkSignal = Utils.computeMovingAverageSignal(updatedSignal, linkSignal,
                    currentTimestamp, lastLinkSignalSeenTimestampMs, MAX_AGE_FOR_SIGNAL_UPDATING_MS);
        }
        if (linkSignal < MIN_SNR_FOR_FLAGGING_POOR_CONNECTION_DBM) {
            linkMode = WifiProblemMode.LOW_SNR;
        } else {
            @SignalStrengthZones int newZone = getZoneFromSignalStrength(linkSignal);
            if (newZone != lastSignalZone) {

            }
        }
        return linkMode;
    }




    public int updateLastWifiEnabledState(int newWifiEnabledState) {
        lastWifiEnabledState = newWifiEnabledState;
        return lastWifiEnabledState;
    }

    @WifiProblemMode
    public int updateWifiDetailedState(NetworkInfo.DetailedState newDetailedState) {
        @WifiProblemMode int wifiProblem = updateDetailedWifiStateInfo(newDetailedState);
        if (newDetailedState == NetworkInfo.DetailedState.DISCONNECTED) {
            clearWifiConnectionInfo();
        }
        return wifiProblem;
    }

    public NetworkInfo.DetailedState getLastWifiDetailedState() {
        return lastWifiDetailedState;
    }

    public void updateWifiConnectionState(NetworkInfo.State newConnectionState) {
        lastWifiConnectionState = newConnectionState;
    }


    public String getPrimaryRouterID() {
        //Be smart here -- return both bssid and ssid -- don't reject just on basis of ssid
        return "BSSID-" + linkBSSID + ",SSID-" + linkSSID;
    }


    public String getPrimaryRouterSSID() {
        return linkSSID;
    }

    public String getPrimaryRouterSignalQuality() {
        @SignalStrengthZones int zone = getZoneFromSignalStrength(getPrimaryLinkSignal());
        if (zone == SignalStrengthZones.UNKNOWN) {
            return EMPTY_STRING;
        }
        return getStringFromZone(zone);
    }

    @WifiProblemMode
    public int updateSupplicantState(SupplicantState supplicantState, int supplicantError)  {
        @WifiProblemMode int problemMode = WifiProblemMode.UNINITIALIZED;
        supplicantStateList.add(supplicantState);
        lastSupplicantState = supplicantState;
        if (checkForProblematicSupplicationPattern()) {
            supplicantStateList.clear();
            problemMode = WifiProblemMode.PROBLEMATIC_SUPPLICANT_PATTERN;
        } else if (supplicantError == WifiManager.ERROR_AUTHENTICATING) {
            problemMode = WifiProblemMode.ERROR_AUTHENTICATING;
        } else if (inInactiveOrDormantState(supplicantState)) {
            problemMode = WifiProblemMode.INACTIVE_OR_DORMANT_SUPPLICANT_STATE;
        }
        return problemMode;
    }

    public boolean isInConnectingState() {
        switch(lastSupplicantState) {
            case AUTHENTICATING:
            case ASSOCIATING:
            case ASSOCIATED:
            case FOUR_WAY_HANDSHAKE:
            case GROUP_HANDSHAKE:
            case COMPLETED:
                return true;
            case DISCONNECTED:
            case INTERFACE_DISABLED:
            case INACTIVE:
            case SCANNING:
            case DORMANT:
            case UNINITIALIZED:
            case INVALID:
            default:
                return false;
        }
    }

    //private methods

    private boolean inInactiveOrDormantState(SupplicantState newSupplicantState) {
        switch (newSupplicantState) {
            case INACTIVE:
            case DORMANT:
            case INTERFACE_DISABLED:
                return true;
        }
        return false;
    }

    private boolean checkForProblematicSupplicationPattern() {
        SupplicantPatterns.SupplicantPattern pattern =
                supplicantStateList.containsPatterns(SupplicantPatterns.getSupplicantPatterns());
        return (pattern != null);
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
            ServiceLog.v("WifiStateData: Setting link signal from  " + this.linkSignal + " to " + linkSignal);
            this.linkSignal = linkSignal;
        }
    }

    private void setLinkSSID(String linkSSID) {
        if (linkSSID != null) {
            ServiceLog.v("WifiStateData: Setting link SSID to " + linkSSID);
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
        ServiceLog.v("WifiStateData: Clearing link info");
        linkSSID = "";
        linkBSSID = "";
        linkFrequency = 0;
        linkSpeedMbps = 0;
        linkSignal = 0;
        lastLinkSignalSeenTimestampMs = 0;
    }

    private int getPrimaryLinkSignal() {
        return linkSignal;
    }

    private boolean checkIfDisconnectedPrematurely() {
        final int GOOD_PRIMARY_AP_SIGNAL_THRESHOLD = -80;
        if (getPrimaryLinkSignal() != 0 && getPrimaryLinkSignal() > GOOD_PRIMARY_AP_SIGNAL_THRESHOLD) {
            return true;
        }
        return false;
    }

    @WifiProblemMode
    private int getCurrentWifiProblemMode() {

        long startTimeMs = getCurrentStateStartTimeMs();
        long currentTimeMs = System.currentTimeMillis();
        NetworkInfo.DetailedState currentState = getCurrentState();
        int numDisconnections = getNumberOfTimesWifiInState(NetworkInfo.DetailedState.DISCONNECTED);
        int numConnections = getNumberOfTimesWifiInState(NetworkInfo.DetailedState.CONNECTING);
        @WifiProblemMode int newWifiProblemMode = WifiProblemMode.UNINITIALIZED;

        //If we are in CONNECTED mode now, then mark this as no issue and return
        if (getCurrentState() == NetworkInfo.DetailedState.CONNECTED) {
            newWifiProblemMode = WifiProblemMode.NO_PROBLEM_DEFAULT_STATE;
        } else if (getCurrentState() == NetworkInfo.DetailedState.DISCONNECTED && checkIfDisconnectedPrematurely()) {
            //If we are in DISCONNECTED mode now, check if we disconnected prematurely
            newWifiProblemMode = WifiProblemMode.DISCONNECTED_PREMATURELY;
        } else if (currentTimeMs - startTimeMs >= THRESHOLD_FOR_DECLARING_CONNECTION_SETUP_STATE_AS_HANGING_MS) {
            switch(currentState) {
                case SCANNING:
                    newWifiProblemMode = WifiProblemMode.HANGING_ON_SCANNING;
                    break;
                case AUTHENTICATING:
                    newWifiProblemMode = WifiProblemMode.HANGING_ON_AUTHENTICATING;
                    break;
                case OBTAINING_IPADDR:
                    newWifiProblemMode = WifiProblemMode.HANGING_ON_DHCP;
                    break;
                case CONNECTING:
                    newWifiProblemMode = WifiProblemMode.HANGING_ON_CONNECTING;
                    break;
            }
        } else if (Math.max(numDisconnections, numConnections) > THRESHOLD_FOR_FLAGGING_FREQUENT_STATE_CHANGES){
            //Check for frequent disconnections
            newWifiProblemMode = WifiProblemMode.FREQUENT_DISCONNECTIONS;
        }

        if (newWifiProblemMode != wifiProblemMode) {
            wifiProblemMode = newWifiProblemMode;
            return wifiProblemMode;
        }
        //No change
        return WifiProblemMode.UNINITIALIZED;
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
                ServiceLog.v("updateDetailedWifiStateInfo current state is: " +
                        detailedWifiState.name() + " last state,lasted: " +
                        lastWifiDetailedState.name() + "," + (currentTimestampMs - lastDetailedWifiStateTimestampMs) + "ms");
            }
            ServiceLog.v("updateDetailedWifiStateInfo updating last wifi state from " + lastWifiDetailedState.name() + " to " + detailedWifiState.name());
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
