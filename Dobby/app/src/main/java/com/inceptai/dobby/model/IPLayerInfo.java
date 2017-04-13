package com.inceptai.dobby.model;

import android.net.DhcpInfo;

import com.inceptai.dobby.utils.Utils;

/**
 * Created by vivek on 4/8/17.
 */

public class IPLayerInfo {
    public static final String DEFAULT_EXTERNAL_ADDRESS1 = "www.google.com";
    public static final String DEFAULT_EXTERNAL_ADDRESS2 = "www.cnn.com";

    public String dns1;
    public String dns2;
    public String gateway;
    public int leaseDuration;
    public String netMask;
    public String serverAddress;
    public String ownIPAddress;
    public String referenceExternalAddress1;
    public String referenceExternalAddress2;

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
    }

    public IPLayerInfo(DhcpInfo dhcpInfo,
                       String referenceExternalAddress1,
                       String referenceExternalAddress2) {
        initialize(dhcpInfo, referenceExternalAddress1, referenceExternalAddress2);
    }

    public IPLayerInfo(DhcpInfo dhcpInfo) {
        initialize(dhcpInfo, DEFAULT_EXTERNAL_ADDRESS1, DEFAULT_EXTERNAL_ADDRESS2);
    }

}
