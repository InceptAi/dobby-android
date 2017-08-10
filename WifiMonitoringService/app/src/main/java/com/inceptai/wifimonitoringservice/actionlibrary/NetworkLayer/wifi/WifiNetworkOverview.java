package com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.wifi;

import com.inceptai.wifimonitoringservice.utils.Utils;

/**
 * Created by vivek on 8/10/17.
 */

public class WifiNetworkOverview {
    private String ssid;
    private int signal;
    private String isp;
    private String externalIP;
    private int signalMetric;

    public WifiNetworkOverview(String ssid, int signal, String isp, String externalIP) {
        this.ssid = ssid;
        this.signal = signal;
        this.isp = isp;
        this.externalIP = externalIP;
    }

    public WifiNetworkOverview() {
        this.ssid = Utils.EMPTY_STRING;
        this.isp = Utils.EMPTY_STRING;
        this.externalIP = Utils.EMPTY_STRING;
    }

    public String getSsid() {
        return ssid;
    }

    public void setSsid(String ssid) {
        this.ssid = ssid;
    }

    public int getSignal() {
        return signal;
    }

    public void setSignal(int signal) {
        this.signal = signal;
    }

    public String getIsp() {
        return isp;
    }

    public void setIsp(String isp) {
        this.isp = isp;
    }

    public String getExternalIP() {
        return externalIP;
    }

    public void setExternalIP(String externalIP) {
        this.externalIP = externalIP;
    }

    public int getSignalMetric() {
        return signalMetric;
    }

    public void setSignalMetric(int signalMetric) {
        this.signalMetric = signalMetric;
    }
}
