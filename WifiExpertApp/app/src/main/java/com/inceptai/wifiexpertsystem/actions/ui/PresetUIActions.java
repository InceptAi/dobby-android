package com.inceptai.wifiexpertsystem.actions.ui;

/**
 * Created by vivek on 11/15/17.
 */

public interface PresetUIActions {
    void turnWifiOn();
    void turnWifiOff();
    void resetNetworkSettings();
    void turnBluetoothOn();
    void turnBluetoothOff();
    void alwaysKeepWifiOnDuringSleep();
    void neverKeepWifiOnDuringSleep();
    void keepWifiOnDuringSleepWhenPluggedIn();
    void switchTo2GHzBandOnly();
    void switchTo5GHzBandOnly();
    void switchToAutomaticFrequencyBandSelection();
    void useOpenWifiAutomatically();
    void turnOffUsingOpenWifiAutomatically();
}
