package com.inceptai.dobby.fake;

import android.content.Context;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.eventbus.DobbyEvent;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.model.DobbyWifiInfo;
import com.inceptai.dobby.utils.Utils;
import com.inceptai.dobby.wifi.WifiAnalyzer;
import com.inceptai.dobby.wifi.WifiState;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * Created by arunesh on 4/11/17.
 */

public class FakeWifiAnalyzer extends WifiAnalyzer {
    private static final long SCAN_LATENCY_MS = 5000;
    public static final FakeWifiScanConfig FAKE_WIFI_SCAN_CONFIG = new FakeWifiScanConfig();

    private static final int CHAN_1_FREQ = 2412;
    private static final int CHAN_6_FREQ = 2437;
    private static final int CHAN_11_FREQ = 2462;

    private ListenableFuture<List<ScanResult>> fakeWifiScanFuture;

    public static class FakeWifiScanConfig {


        public String mainApSSID;
        public int numApsChannelOne, numApsChannelSix, numApsChannelEleven;
        @WifiState.SignalStrengthZones public int signalZoneChannelOne, signalZoneChannelSix, signalZoneChannelEleven, signalZoneMainAp;
        public NetworkInfo.DetailedState lastDetailedState;
        public long lastDetailedStateStartTimestampMs;
        @WifiState.WifiLinkMode
        public int fakeWifiProblemMode;


        public int mainApChannelNumber;


        FakeWifiScanConfig() {
            numApsChannelOne = numApsChannelSix = numApsChannelEleven = 4;
            mainApChannelNumber = 6;
            mainApSSID = "FAKEAP";
            signalZoneChannelOne = WifiState.SignalStrengthZones.MEDIUM;
            signalZoneChannelEleven = WifiState.SignalStrengthZones.MEDIUM;
            signalZoneChannelSix = WifiState.SignalStrengthZones.MEDIUM;
            signalZoneMainAp = WifiState.SignalStrengthZones.MEDIUM;
            lastDetailedState = NetworkInfo.DetailedState.CONNECTED;
            lastDetailedStateStartTimestampMs = System.currentTimeMillis();
            fakeWifiProblemMode = WifiState.WifiLinkMode.NO_PROBLEM_DEFAULT_STATE;
        }
    }

    public FakeWifiAnalyzer(Context context, WifiManager wifiManager,
                            DobbyThreadpool threadpool, DobbyEventBus eventBus) {
        super(context, wifiManager, threadpool, eventBus);
        wifiState.updateWifiStats(generateFakeWifiInfo(), null);
        wifiState.setCurrentWifiProblemMode(FAKE_WIFI_SCAN_CONFIG.fakeWifiProblemMode);
    }

    /**
     * Factory constructor to create an instance
     *
     * @param context Application context.
     * @return Instance of WifiAnalyzer or null on error.
     */
    @Nullable
    public static FakeWifiAnalyzer create(Context context, DobbyThreadpool threadpool, DobbyEventBus eventBus) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            return new FakeWifiAnalyzer(context.getApplicationContext(), wifiManager, threadpool, eventBus);
        }
        return null;
    }

    @Override
    protected void updateWifiStatsDetailedState(NetworkInfo.DetailedState detailedState) {
        @WifiState.WifiLinkMode int problemMode = FAKE_WIFI_SCAN_CONFIG.fakeWifiProblemMode;
        wifiState.setCurrentWifiProblemMode(problemMode);
        @DobbyEvent.EventType int eventTypeToBroadcast = convertWifiStateProblemToDobbyEventType(problemMode);
        if (eventTypeToBroadcast != DobbyEvent.EventType.WIFI_STATE_UNKNOWN) {
            eventBus.postEvent(new DobbyEvent(eventTypeToBroadcast));
        }
    }

    @Override
    public ListenableFuture<List<ScanResult>> startWifiScan() {
        fakeWifiScanFuture = threadpool.getListeningScheduledExecutorService().schedule(new Callable<List<ScanResult>>() {
            @Override
            public List<ScanResult> call() {
                List<ScanResult> wifiScan = generateFakeWifiScan();
                wifiState.updateWifiStats(generateFakeWifiInfo(), wifiScan);
                return  wifiScan;
            }
        }, SCAN_LATENCY_MS, TimeUnit.MILLISECONDS);
        return fakeWifiScanFuture;
    }

    public List<ScanResult> generateFakeWifiScan() {
        List<ScanResult> list = new LinkedList<>();
        for (int i = 0; i < FAKE_WIFI_SCAN_CONFIG.numApsChannelOne; i++) {
            list.add(getFakeScanResult(FAKE_WIFI_SCAN_CONFIG.signalZoneChannelOne, CHAN_1_FREQ));
        }

        for (int i = 0; i < FAKE_WIFI_SCAN_CONFIG.numApsChannelSix; i++) {
            list.add(getFakeScanResult(FAKE_WIFI_SCAN_CONFIG.signalZoneChannelSix, CHAN_6_FREQ));
        }
        for (int i = 0; i < FAKE_WIFI_SCAN_CONFIG.numApsChannelEleven; i++) {
            list.add(getFakeScanResult(FAKE_WIFI_SCAN_CONFIG.signalZoneChannelEleven, CHAN_11_FREQ));
        }
        //Add a scan result for the main AP.
        list.add(getFakeScanResultForMainAP());
        return list;
    }

    @Override
    protected void updateWifiStatsWithWifiInfo(WifiInfo info) {
        wifiState.updateWifiStats(generateFakeWifiInfo(), null);
    }

    private ScanResult getFakeScanResult(@WifiState.SignalStrengthZones int zone, int channelFreq) {

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

    private ScanResult getFakeScanResultForMainAP() {

        String ssid = FAKE_WIFI_SCAN_CONFIG.mainApSSID;
        int level = getLevel(FAKE_WIFI_SCAN_CONFIG.signalZoneMainAp);
        int channelFreq = CHAN_1_FREQ;
        switch(FAKE_WIFI_SCAN_CONFIG.mainApChannelNumber) {
            case 1:
                channelFreq = CHAN_1_FREQ;
                break;
            case 6:
                channelFreq = CHAN_6_FREQ;
                break;
            case 11:
                channelFreq = CHAN_11_FREQ;
                break;
        }
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


    public DobbyWifiInfo generateFakeWifiInfo() {
        final int linkSpeedMbps = 36;
        return new DobbyWifiInfo(FAKE_WIFI_SCAN_CONFIG.mainApSSID, FAKE_WIFI_SCAN_CONFIG.mainApSSID,
                randomBssid(), getLevel(FAKE_WIFI_SCAN_CONFIG.signalZoneMainAp), linkSpeedMbps);
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

    private static int getLevel(@WifiState.SignalStrengthZones int zone) {
        if (zone == WifiState.SignalStrengthZones.HIGH) {
            return getRandomSignal(WifiState.SignalStrengthZones.HIGH, -30);
        } else if (zone == WifiState.SignalStrengthZones.MEDIUM) {
            return getRandomSignal(WifiState.SignalStrengthZones.MEDIUM, WifiState.SignalStrengthZones.HIGH);

        } else if (zone == WifiState.SignalStrengthZones.LOW) {
            return getRandomSignal(WifiState.SignalStrengthZones.LOW, WifiState.SignalStrengthZones.MEDIUM);
        } else if (zone == WifiState.SignalStrengthZones.FRINGE) {
            return getRandomSignal(WifiState.SignalStrengthZones.FRINGE, WifiState.SignalStrengthZones.LOW);
        }
        return -150;
    }

    private static int getRandomSignal(int low, int high) {
        int range = high - low;
        return low + Utils.getRandom().nextInt(range);
    }
}
