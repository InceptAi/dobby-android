package com.inceptai.dobby.model;

import android.net.DhcpInfo;

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

    public IPLayerInfo(DhcpInfo dhcpInfo) {
        this.dns1 = Utils.intToIp(dhcpInfo.dns1);
        this.dns2 = Utils.intToIp(dhcpInfo.dns2);
        this.gateway = Utils.intToIp(dhcpInfo.ipAddress);
        this.serverAddress = Utils.intToIp(dhcpInfo.serverAddress);
        this.netMask = Utils.intToIp(dhcpInfo.netmask);
        this.leaseDuration = dhcpInfo.leaseDuration;
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
    }
}

