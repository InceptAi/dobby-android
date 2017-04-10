package com.inceptai.dobby.speedtest;

import com.inceptai.dobby.fake.FakeSpeedTestSocket;

import fr.bmartel.speedtest.SpeedTestSocket;

/**
 * Created by arunesh on 4/10/17.
 */

public class SpeedTestSocketFactory {
    public static boolean USE_FAKES = true;


    public static SpeedTestSocket newSocket() {
        if (USE_FAKES) {
            return new FakeSpeedTestSocket();
        } else {
            return new SpeedTestSocket();
        }
    }
}
