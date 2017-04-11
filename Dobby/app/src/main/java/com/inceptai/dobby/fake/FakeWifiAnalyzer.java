package com.inceptai.dobby.fake;

import android.net.DhcpInfo;
import android.net.wifi.ScanResult;
import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.utils.Utils;
import com.inceptai.dobby.wifi.WifiStats;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * Created by arunesh on 4/11/17.
 */

public class FakeWifiAnalyzer {
    private static final long SCAN_LATENCY_MS = 5000;
    public static final FakeWifiScanConfig FAKE_WIFI_SCAN_CONFIG = new FakeWifiScanConfig();

    private static final int CHAN_1_FREQ = 2412;
    private static final int CHAN_6_FREQ = 2437;
    private static final int CHAN_11_FREQ = 2462;

    private ListenableFuture<List<ScanResult>> wifiScanFuture;
    private WifiStats fakeWifiStats;
    private DhcpInfo fakeDhcpInfo;
    private DobbyThreadpool threadpool;

    public static class FakeWifiScanConfig {
        public int numApsChannelOne, numApsChannelSix, numApsChannelEleven;
        @WifiStats.SignalStrengthZones public int signalZoneChannelOne, signalZoneChannelSix, signalZoneChannelEleven;

        FakeWifiScanConfig() {
            numApsChannelOne = numApsChannelSix = numApsChannelEleven = 4;
            signalZoneChannelOne = WifiStats.SignalStrengthZones.MEDIUM;
            signalZoneChannelEleven = WifiStats.SignalStrengthZones.MEDIUM;
            signalZoneChannelSix = WifiStats.SignalStrengthZones.MEDIUM;
        }
    }

    public FakeWifiAnalyzer(DobbyThreadpool threadpool) {
        this.threadpool = threadpool;
    }

    public ListenableFuture<List<ScanResult>> startWifiScan() {
        wifiScanFuture = threadpool.getListeningScheduledExecutorService().schedule(new Callable<List<ScanResult>>() {
            @Override
            public List<ScanResult> call() {
                return generateFakeWifiScan();
            }
        }, SCAN_LATENCY_MS, TimeUnit.MILLISECONDS);
        return wifiScanFuture;
    }


    public List<ScanResult> generateFakeWifiScan() {
        List<ScanResult> list = new LinkedList<>();
        for (int i = 0; i < FAKE_WIFI_SCAN_CONFIG.numApsChannelOne; i++) {
            list.add(getFakeScanResult(FAKE_WIFI_SCAN_CONFIG.signalZoneChannelOne, CHAN_1_FREQ));
        }

        for (int i = 0; i < FAKE_WIFI_SCAN_CONFIG.numApsChannelSix; i++) {
            list.add(getFakeScanResult(FAKE_WIFI_SCAN_CONFIG.signalZoneChannelSix, CHAN_6_FREQ));
        }
        for (int i = 0; i < FAKE_WIFI_SCAN_CONFIG.numApsChannelOne; i++) {
            list.add(getFakeScanResult(FAKE_WIFI_SCAN_CONFIG.signalZoneChannelEleven, CHAN_11_FREQ));
        }
        return list;
    }

//
//    public DhcpInfo getDhcpInfo() {
//        return wifiManager.getDhcpInfo();
//    }
//
//    public WifiStats getWifiStats() {
//        return wifiStats;
//    }

    private ScanResult getFakeScanResult(@WifiStats.SignalStrengthZones int zone, int channelFreq) {

        String ssid = randomBssid();
        int level = getLevel(zone);
//        ScanResult result =
//        ScanResult scanResult = new ScanResult(ssid, ssid, hessid,
//                /* int anqpDomainId */ 0,
//                /* String caps */ Utils.EMPTY_STRING,
//                /* level */ level,
//                frequency,
//                /* long tsf */ 0L,
//                /* int distCm */ 0,
//                /* int distSdCm */ 0,
//                /* int channelWidth*/ 22,
//                /* int centerFreq0*/ centerFreq0,
//                /* int centerFreq1 */ centerFreq1,
//                /* boolean is80211McRTTResponder */ false);
        ScanResult result = null;
        try {
            result = ScanResult.class.newInstance();
            result.BSSID = ssid;
            result.SSID = ssid;
            result.level = level;
            result. frequency = channelFreq;
            result.centerFreq0 = channelFreq;
            result.centerFreq1 = channelFreq;
            result.channelWidth = ScanResult.CHANNEL_WIDTH_20MHZ;
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
            Log.e(TAG, "Unable to generate fake ScanResult");
        }
        return result;
    }

    private static String randomBssid() {
        Random random = Utils.getRandom();
        return String.format("%02X:%02X:%02X:%02X:%02X:%02X",
                random.nextInt(16),
                random.nextInt(16),
                random.nextInt(16),
                random.nextInt(16),
                random.nextInt(16),
                random.nextInt(16));
    }

    private static int getLevel(@WifiStats.SignalStrengthZones int zone) {
        if (zone == WifiStats.SignalStrengthZones.HIGH) {
            return getRandomSignal(WifiStats.SignalStrengthZones.HIGH, -30);
        } else if (zone == WifiStats.SignalStrengthZones.MEDIUM) {
            return getRandomSignal(WifiStats.SignalStrengthZones.MEDIUM, WifiStats.SignalStrengthZones.HIGH);

        } else if (zone == WifiStats.SignalStrengthZones.LOW) {
            return getRandomSignal(WifiStats.SignalStrengthZones.LOW, WifiStats.SignalStrengthZones.MEDIUM);
        } else if (zone == WifiStats.SignalStrengthZones.FRINGE) {
            return getRandomSignal(WifiStats.SignalStrengthZones.FRINGE, WifiStats.SignalStrengthZones.LOW);
        }
        return -150;
    }

    private static int getRandomSignal(int low, int high) {
        int range = high - low;
        return low + Utils.getRandom().nextInt(range);
    }
}
