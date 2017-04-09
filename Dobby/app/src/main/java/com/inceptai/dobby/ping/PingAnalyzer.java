package com.inceptai.dobby.ping;

import android.support.annotation.Nullable;
import android.util.Log;

import com.inceptai.dobby.model.IPLayerInfo;
import com.inceptai.dobby.model.PingStats;

/**
 * Created by vivek on 4/8/17.
 */

public class PingAnalyzer {
    public static final String DEFAULT_EXTERNAL_SERVER_ADDRESS = "www.google.com";

    private IPLayerInfo ipLayerInfo;
    private String externalServerPingAddress;
    private IPLayerInfo.IPLayerPingStats ipLayerPingStats;


    public PingAnalyzer(IPLayerInfo ipLayerInfo) {
        this.ipLayerInfo = ipLayerInfo;
        externalServerPingAddress = DEFAULT_EXTERNAL_SERVER_ADDRESS;
    }

    public PingAnalyzer(IPLayerInfo ipLayerInfo, String externalServerPingAddress) {
        this.ipLayerInfo = ipLayerInfo;
        this.externalServerPingAddress = externalServerPingAddress;
    }


    /**
     * Factory constructor for creating an instance
     * @param ipLayerInfo
     * @param externalServerPingAddress
     * @return
     */
    @Nullable
    public static PingAnalyzer create(IPLayerInfo ipLayerInfo, @Nullable String externalServerPingAddress) {
        return new PingAnalyzer(ipLayerInfo, externalServerPingAddress);
    }


    public IPLayerInfo.IPLayerPingStats getRecentIPLayerPingStats() {
        return ipLayerPingStats;
    }

    public void updateIPLayerInfo(IPLayerInfo updatedInfo) {
        this.ipLayerInfo = updatedInfo;
        performAllPingTests();
    }

    public PingStats performGatewayPing() {
        PingStats pingStats = new PingStats(ipLayerInfo.gateway);
        try {
            pingStats = PingAction.pingAndReturnStats(ipLayerInfo.gateway);
            ipLayerPingStats.gatewayPingStats = pingStats;
        } catch (PingAction.PingActionException e) {
            Log.v("PingAnalyzer", "Exception while pinging gw: " + e);
        }
        return pingStats;
    }

    public PingStats performDNS1Ping() {
        PingStats pingStats = new PingStats(ipLayerInfo.dns1);
        try {
            pingStats = PingAction.pingAndReturnStats(ipLayerInfo.dns1);
            ipLayerPingStats.dns1PingStats = pingStats;
        } catch (PingAction.PingActionException e) {
            Log.v("PingAnalyzer", "Exception while pinging dns1: " + e);
        }
        return pingStats;
    }

    public PingStats performDNS2Ping() {
        PingStats pingStats = new PingStats(ipLayerInfo.dns2);
        try {
            pingStats = PingAction.pingAndReturnStats(ipLayerInfo.dns2);
            ipLayerPingStats.dns2PingStats = pingStats;
        } catch (PingAction.PingActionException e) {
            Log.v("PingAnalyzer", "Exception while pinging dns2: " + e);
        }
        return pingStats;
    }

    public PingStats performExternalServerPing() {
        PingStats pingStats = new PingStats(externalServerPingAddress);
        try {
            pingStats = PingAction.pingAndReturnStats(externalServerPingAddress);
            ipLayerPingStats.externalServerPingStats = pingStats;
        } catch (PingAction.PingActionException e) {
            Log.v("PingAnalyzer", "Exception while pinging external addr: " + e);
        }
        return pingStats;
    }

    public IPLayerInfo.IPLayerPingStats performAllPingTests() {
        if (ipLayerInfo != null) {
            performGatewayPing();
            performDNS1Ping();
            performDNS2Ping();
            performExternalServerPing();
        }
        return ipLayerPingStats;
    }
}
