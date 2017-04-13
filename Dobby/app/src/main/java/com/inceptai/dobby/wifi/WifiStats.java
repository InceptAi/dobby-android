package com.inceptai.dobby.wifi;

import android.net.wifi.ScanResult;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.inceptai.dobby.model.DobbyWifiInfo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.List;

/**
 * Created by vivek on 4/8/17.
 */

public class WifiStats {
    private static final int MIN_SIGNAL_CHANGE_FOR_CLEARING_CHANNEL_STATS = 10;
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

    public HashMap<Integer, ChannelInfo> channelInfoMap;


    public WifiStats() {
        channelInfoMap = new HashMap<>();
    }

    public void clearWifiConnectionInfo() {
        linkSSID = null;
        linkBSSID = null;
        linkFrequency = 0;
        linkSpeedMbps = 0;
        linkSignal = 0;
        channelInfoMap = new HashMap<>();
    }

    public void updateSignal(int updatedSignal) {
        if (updatedSignal == 0) {
            return;
        }
        if (Math.abs(updatedSignal - linkSignal) >= MIN_SIGNAL_CHANGE_FOR_CLEARING_CHANNEL_STATS) {
            channelInfoMap.clear();
            linkSignal = updatedSignal;
        }
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
}
