package com.inceptai.dobby.ping;

import android.support.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.model.IPLayerInfo;
import com.inceptai.dobby.model.PingStats;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by vivek on 4/8/17.
 */

public class PingAnalyzer {
    public static final int MAX_ASYNC_PING = 10;

    private IPLayerInfo ipLayerInfo;
    private HashMap<String, PingStats> ipLayerPingStats;
    private DobbyThreadpool dobbyThreadpool;
    private PingAction pingAction;
    private PingActionListener pingActionListener;
    private ConcurrentHashMap<String, Boolean> pingsInFlight;
    private SettableFuture<HashMap<String, PingStats>> pingResultsFuture;

    private PingAnalyzer(IPLayerInfo ipLayerInfo, DobbyThreadpool dobbyThreadpool) {
        this.ipLayerInfo = ipLayerInfo;
        pingActionListener = new PingActionListener();
        this.dobbyThreadpool = dobbyThreadpool;
        pingAction = PingAction.create(pingActionListener);
        pingsInFlight = new ConcurrentHashMap<String, Boolean>();
        ipLayerPingStats = new HashMap<>();
        initializePingStats(ipLayerInfo);
    }

    private void initializePingStats(IPLayerInfo ipLayerInfo) {
        ipLayerPingStats.put(ipLayerInfo.gateway, new PingStats(ipLayerInfo.gateway));
        ipLayerPingStats.put(ipLayerInfo.dns1, new PingStats(ipLayerInfo.dns1));
        ipLayerPingStats.put(ipLayerInfo.dns2, new PingStats(ipLayerInfo.dns2));
        ipLayerPingStats.put(ipLayerInfo.referenceExternalAddress1,
                new PingStats(ipLayerInfo.referenceExternalAddress1));
        ipLayerPingStats.put(ipLayerInfo.referenceExternalAddress2,
                new PingStats(ipLayerInfo.referenceExternalAddress2));
    }

    /**
     * Factory constructor for creating an instance
     * @param ipLayerInfo
     * @return
     */
    @Nullable
    public static PingAnalyzer create(IPLayerInfo ipLayerInfo,
                                      DobbyThreadpool dobbyThreadpool) {
        Preconditions.checkNotNull(ipLayerInfo);
        Preconditions.checkNotNull(dobbyThreadpool);
        return new PingAnalyzer(ipLayerInfo, dobbyThreadpool);
    }

    private void schedulePingAndReturn(String[] pingAddressList) {
        final String[] pingAddressListFinal = pingAddressList;
        dobbyThreadpool.submit(new Runnable() {
            @Override
            public void run() {
                pingAction.pingAndReturnStatsList(pingAddressListFinal);
            }
        });
    }

    private void scheduleMultipleAsyncPingAndReturn(String[] pingAddressList) {
        for (String address: pingAddressList) {
            String[] list = {address};
            schedulePingAndReturn(list);
        }
    }

    private boolean checkIfAllPingsDone() {
        boolean pingsAllDone = true;
        for (Boolean value : pingsInFlight.values()) {
            pingsAllDone = pingsAllDone && !value;
        }
        return pingsAllDone;
    }

    public ListenableFuture<HashMap<String, PingStats>> scheduleEssentialPingTestsAsyc() throws Exception {
        if (!checkIfAllPingsDone()) {
            throw new Exception("Ping tests still in progress, cannot start another one");
        }
        if (ipLayerInfo == null) {
            throw new IllegalStateException("Cannot schedule pings when iplayerInfo is null");
        }
        pingsInFlight.clear();
        String[] addressList = {ipLayerInfo.gateway, ipLayerInfo.dns1,ipLayerInfo.dns2,
                ipLayerInfo.referenceExternalAddress1, ipLayerInfo.referenceExternalAddress2};
        for (String address: addressList) {
            pingsInFlight.put(address, true);
        }
        scheduleMultipleAsyncPingAndReturn(addressList);
        pingResultsFuture = SettableFuture.create();
        return pingResultsFuture;
    }

    public HashMap<String, PingStats> getRecentIPLayerPingStats() {
        return ipLayerPingStats;
    }

    public ListenableFuture<HashMap<String, PingStats>> updateIPLayerInfo(IPLayerInfo updatedInfo) throws Exception {
        this.ipLayerInfo = updatedInfo;
        return scheduleEssentialPingTestsAsyc();
    }

    private class PingActionListener implements PingAction.ResultsCallback {

        @Override
        public void onFinish(HashMap<String, PingStats> pingStatsHashMap) {
            if (pingStatsHashMap == null) {
                return;
            }
            for (String key : ipLayerPingStats.keySet()) {
                PingStats returnedValue = pingStatsHashMap.get(key);
                if (returnedValue != null) {
                    ipLayerPingStats.put(key,returnedValue);
                    pingsInFlight.put(key, false);
                }
            }
            if (checkIfAllPingsDone()) {
                //Return the results here
                if (pingResultsFuture != null) {
                    pingResultsFuture.set(ipLayerPingStats);
                }
            }
        }

        @Override
        public void onError(@PingAction.PingErrorCode int errorType, String[] addressList, String errorMessage) {
            for (String address: addressList) {
                pingsInFlight.put(address, false);
            }
        }
    }


}
