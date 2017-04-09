package com.inceptai.dobby.model;

import android.net.DhcpInfo;

import com.google.gson.Gson;
import com.inceptai.dobby.utils.Utils;

/**
 * Created by vivek on 4/8/17.
 */

public class IPLayerInfo {
    public String dns1;
    public String dns2;
    public String gateway;
    public int leaseDuration;
    public String netMask;
    public String serverAddress;
    public String ownIPAddress;

    public IPLayerInfo(DhcpInfo dhcpInfo) {
        this.dns1 = Utils.intToIp(dhcpInfo.dns1);
        this.dns2 = Utils.intToIp(dhcpInfo.dns2);
        this.gateway = Utils.intToIp(dhcpInfo.gateway);
        this.serverAddress = Utils.intToIp(dhcpInfo.serverAddress);
        this.netMask = Utils.intToIp(dhcpInfo.netmask);
        this.leaseDuration = dhcpInfo.leaseDuration;
        this.ownIPAddress = Utils.intToIp(dhcpInfo.ipAddress);
    }

    public static class IPLayerPingStats {
        public PingStats gatewayPingStats;
        public PingStats dns1PingStats;
        public PingStats dns2PingStats;
        public PingStats externalServerPingStats;

        public IPLayerPingStats(PingStats gatewayPingStats, PingStats dns1PingStats,
                                PingStats dns2PingStats, PingStats externalServerPingStats) {
            this.gatewayPingStats = gatewayPingStats;
            this.dns1PingStats = dns1PingStats;
            this.dns2PingStats = dns2PingStats;
            this.externalServerPingStats = externalServerPingStats;
        }

        public IPLayerPingStats(IPLayerInfo ipLayerInfo, String externalAddressToPing) {
            if (ipLayerInfo != null) {
                this.gatewayPingStats = new PingStats(ipLayerInfo.gateway);
                this.dns1PingStats = new PingStats(ipLayerInfo.dns1);
                this.dns2PingStats = new PingStats(ipLayerInfo.dns2);
                this.externalServerPingStats = new PingStats(externalAddressToPing);
            }
        }

        public String toJson() {
            Gson gson = new Gson();
            String json = gson.toJson(this);
            return json;
        }
    }
}

