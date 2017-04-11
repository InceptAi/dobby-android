package com.inceptai.dobby.speedtest;

import com.inceptai.dobby.fake.FakeSpeedTestSocket;

import java.util.concurrent.atomic.AtomicBoolean;

import fr.bmartel.speedtest.SpeedTestSocket;

/**
 * Created by arunesh on 4/10/17.
 */

public class SpeedTestSocketFactory {
    public static AtomicBoolean sUseFakes = new AtomicBoolean(true);

    private SpeedTestSocketFactory() {}

    public static void setUseFakes(boolean value) {
        sUseFakes.set(value);
    }

    public static SpeedTestSocket newSocket() {
        if (sUseFakes.get()) {
            return new FakeSpeedTestSocket();
        } else {
            return new SpeedTestSocket();
        }
    }
}
