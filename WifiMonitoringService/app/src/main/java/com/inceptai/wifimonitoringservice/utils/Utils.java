package com.inceptai.wifimonitoringservice.utils;

/**
 * Created by vivek on 7/11/17.
 */

public class Utils {
    public static int computeMovingAverageSignal(int currentSignal, int previousSignal, long currentSeen, long previousSeen, int maxAge) {
        if (currentSeen == 0) {
            currentSeen = System.currentTimeMillis();
        }
        long age = currentSeen - previousSeen;
        if (previousSeen > 0 && age > 0 && age < maxAge / 2) {
            // Average the RSSI with previously seen instances of this scan result
            double alpha = 0.5 - (double) age / (double) maxAge;
            currentSignal = (int) ((double) currentSignal * (1 - alpha) + (double) previousSignal * alpha);
        }
        return currentSignal;
    }
}
