package com.inceptai.dobby.model;

import android.net.wifi.WifiInfo;

/**
 * Created by vivek on 4/12/17.
 */

public class DobbyWifiInfo {
    private String bssid;
    private String ssid;
    private String mac;
    private int rssi;
    private int speed;
    private int frequency;

    public DobbyWifiInfo(String bssid, String ssid, String mac, int rssi, int speed) {
        this.bssid = bssid;
        this.ssid = ssid;
        this.rssi = rssi;
        this.speed = speed;
        this.mac = mac;
        this.frequency = 0;
    }

    public DobbyWifiInfo(String bssid, String ssid, String mac, int rssi, int speed, int frequency) {
        this.bssid = bssid;
        this.ssid = ssid;
        this.rssi = rssi;
        this.speed = speed;
        this.mac = mac;
        this.frequency = frequency;
    }

    public DobbyWifiInfo(WifiInfo info) {
        this.bssid = info.getBSSID();
        this.ssid = info.getSSID();
        this.rssi = info.getRssi();
        this.speed = info.getLinkSpeed();
        this.mac = info.getMacAddress();
    }

    public String getBSSID() {
        return bssid;
    }

    public String getSSID() {
        return ssid;
    }

    public int getRssi() {
        return rssi;
    }

    public int getLinkSpeed() {
        return speed;
    }

    public String getMacAddress() {
        return mac;
    }

    public int getFrequency() {
        return frequency;
    }
}
