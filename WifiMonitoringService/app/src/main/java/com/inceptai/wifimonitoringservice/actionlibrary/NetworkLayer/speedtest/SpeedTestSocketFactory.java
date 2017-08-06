package com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.speedtest;

import fr.bmartel.speedtest.SpeedTestSocket;

/**
 * Created by arunesh on 4/10/17.
 */

public class SpeedTestSocketFactory {
    private SpeedTestSocketFactory() {}

    public static SpeedTestSocket newSocket() {
            return new SpeedTestSocket();
    }
}
