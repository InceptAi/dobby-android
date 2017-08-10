package com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ping;

import android.net.DhcpInfo;

import com.google.common.collect.ImmutableMap;
import com.inceptai.wifimonitoringservice.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by vivek on 4/8/17.
 */

public class IPLayerInfo {
    public static final String DEFAULT_EXTERNAL_ADDRESS1 = "www.google.com";
    public static final String DEFAULT_EXTERNAL_ADDRESS2 = "www.cnn.com";
    public static final String LEVEL3_PRIMARY_DNS_SERVER = "209.244.0.3";
    public static final String LEVEL3_SECONDARY_DNS_SERVER = "209.244.0.4";
    public static final String GOOGLE_PRIMARY_DNS_SERVER = "8.8.8.8";
    public static final String GOOGLE_SECONDARY_DNS_SERVER = "8.8.4.4";
    public static final String VERISIGN_PRIMARY_DNS_SERVER = "64.6.64.6";
    public static final String VERISIGN_SECONDARY_DNS_SERVER = "64.6.65.6";
    public static final Map<String, String> dnsIpToName = ImmutableMap.of(
            LEVEL3_PRIMARY_DNS_SERVER, "Level3 PRIMARY_DNS",
            GOOGLE_PRIMARY_DNS_SERVER, "Google PRIMARY_DNS",
            VERISIGN_PRIMARY_DNS_SERVER, "Verisign");

    public String dns1;
    public String dns2;
    public String gateway;
    public int leaseDuration;
    public String netMask;
    public String serverAddress;
    public String ownIPAddress;
    public String referenceExternalAddress1;
    public String referenceExternalAddress2;
    public String publicDns1;
    public String publicDns2;

    private void initialize(DhcpInfo dhcpInfo,
                       String referenceExternalAddress1,
                       String referenceExternalAddress2) {
        this.dns1 = Utils.intToIp(dhcpInfo.dns1);
        this.dns2 = Utils.intToIp(dhcpInfo.dns2);
        this.gateway = Utils.intToIp(dhcpInfo.gateway);
        this.serverAddress = Utils.intToIp(dhcpInfo.serverAddress);
        this.netMask = Utils.intToIp(dhcpInfo.netmask);
        this.leaseDuration = dhcpInfo.leaseDuration;
        this.ownIPAddress = Utils.intToIp(dhcpInfo.ipAddress);
        this.referenceExternalAddress1 = referenceExternalAddress1;
        this.referenceExternalAddress2 = referenceExternalAddress2;
        publicDns1 = LEVEL3_PRIMARY_DNS_SERVER;
        publicDns2 = GOOGLE_PRIMARY_DNS_SERVER;
        if (publicDns1.equals(this.dns1)) {
            publicDns1 = VERISIGN_PRIMARY_DNS_SERVER;
        } else if (publicDns2.equals(this.dns1)) {
            publicDns2 = VERISIGN_PRIMARY_DNS_SERVER;
        }
    }

    public IPLayerInfo(DhcpInfo dhcpInfo,
                       String referenceExternalAddress1,
                       String referenceExternalAddress2) {
        initialize(dhcpInfo, referenceExternalAddress1, referenceExternalAddress2);
    }

    public IPLayerInfo() {
        dns1 = Utils.EMPTY_STRING;
        dns2 = Utils.EMPTY_STRING;
        gateway = Utils.EMPTY_STRING;
        netMask = Utils.EMPTY_STRING;
        serverAddress = Utils.EMPTY_STRING;
        ownIPAddress = Utils.EMPTY_STRING;
        referenceExternalAddress1 = Utils.EMPTY_STRING;
        referenceExternalAddress2 = Utils.EMPTY_STRING;
        publicDns1 = Utils.EMPTY_STRING;
        publicDns2 = Utils.EMPTY_STRING;
    }

    public IPLayerInfo(DhcpInfo dhcpInfo) {
        initialize(dhcpInfo, DEFAULT_EXTERNAL_ADDRESS1, DEFAULT_EXTERNAL_ADDRESS2);
    }

    public IPLayerInfo(String ownIPAddress, String gateway, String primaryDns,
                       String externalDns, String externalServer) {
        this.ownIPAddress = ownIPAddress;
        this.gateway = gateway;
        this.referenceExternalAddress1 = externalServer;
        this.dns1 = primaryDns;
        this.publicDns1 = externalDns;
    }

    public List<String> getIPAddressList() {
        List<String> ipAddressList = new ArrayList<>();
        ipAddressList.add(ownIPAddress);
        ipAddressList.add(gateway);
        ipAddressList.add(dns1);
        ipAddressList.add(publicDns1);
        ipAddressList.add(referenceExternalAddress1);
        return ipAddressList;
    }

    @PingStats.IPAddressType
    public int getTypeForAddress(String ipAddress) {
        @PingStats.IPAddressType int type = PingStats.IPAddressType.UNKNOWN;
        if (ipAddress.equals(ownIPAddress)) {
            type = PingStats.IPAddressType.PRIMARY_IP;
        } else if (ipAddress.equals(dns1) || ipAddress.equals(dns2)) {
            type = PingStats.IPAddressType.PRIMARY_DNS;
        } else if (ipAddress.equals(gateway)) {
            type = PingStats.IPAddressType.GATEWAY;
        } else if (ipAddress.equals(publicDns1) || ipAddress.equals(publicDns2)) {
            type = PingStats.IPAddressType.EXTERNAL_DNS;
        } else if (ipAddress.equals(referenceExternalAddress1) || ipAddress.equals(referenceExternalAddress2)) {
            type = PingStats.IPAddressType.EXTERNAL_SERVER;
        }
        return type;
    }

}

