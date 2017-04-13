package com.inceptai.dobby.speedtest;

import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.fake.FakeSpeedTestSocket;

import java.util.concurrent.atomic.AtomicBoolean;

import fr.bmartel.speedtest.SpeedTestSocket;

/**
 * Created by arunesh on 4/10/17.
 */

public class SpeedTestSocketFactory {
    private SpeedTestSocketFactory() {}

    public static SpeedTestSocket newSocket() {
        if (DobbyApplication.USE_FAKES.get()) {
            return new FakeSpeedTestSocket();
        } else {
            return new SpeedTestSocket();
        }
    }
}
