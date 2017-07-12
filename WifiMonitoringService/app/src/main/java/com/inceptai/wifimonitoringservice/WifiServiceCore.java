package com.inceptai.wifimonitoringservice;

import android.content.Context;
import android.support.annotation.Nullable;

import com.inceptai.wifimonitoringservice.monitors.PeriodicCheckMonitor;
import com.inceptai.wifimonitoringservice.monitors.ScreenStateMonitor;
import com.inceptai.wifimonitoringservice.monitors.WifiStateMonitor;
import com.inceptai.wifimonitoringservice.utils.ServiceLog;

import java.lang.ref.WeakReference;

/**
 * Created by vivek on 7/10/17.
 */

public class WifiServiceCore implements
        WifiStateMonitor.WifiStateCallback,
        ScreenStateMonitor.ScreenStateCallback,
        PeriodicCheckMonitor.PeriodicCheckCallback {
    //Key components
    private WifiStateMonitor wifiStateMonitor;
    private ScreenStateMonitor screenStateMonitor;
    private ServiceActionTaker serviceActionTaker;
    private PeriodicCheckMonitor periodicCheckMonitor;
    private ServiceThreadPool serviceThreadPool;

    private static WifiServiceCore WIFI_SERVICE_CORE;


    private WifiServiceCore(Context context, ServiceThreadPool serviceThreadPool) {
        WeakReference<Context> weakContext = new WeakReference<Context>(context);
        this.serviceThreadPool = serviceThreadPool;
        wifiStateMonitor = new WifiStateMonitor(weakContext.get());
        screenStateMonitor = new ScreenStateMonitor(weakContext.get());
        periodicCheckMonitor = new PeriodicCheckMonitor(weakContext.get());
        serviceActionTaker = new ServiceActionTaker(weakContext.get(), serviceThreadPool.getExecutor(), serviceThreadPool.getScheduledExecutorServiceForActions());
    }

    /**
     * Factory constructor to create an instance
     *
     * @param context Application context.
     * @return Instance of WifiAnalyzer or null on error.
     */
    @Nullable
    public static WifiServiceCore create(Context context, ServiceThreadPool serviceThreadPool) {
        if (context == null) {
            return null;
        }
        if (WIFI_SERVICE_CORE == null) {
            return new WifiServiceCore(context, serviceThreadPool);
        }
        return WIFI_SERVICE_CORE;
    }

    public void startMonitoring() {
        wifiStateMonitor.registerCallback(this);
        screenStateMonitor.registerCallback(this);
    }

    public void cleanup() {
        wifiStateMonitor.unregisterCallback();
        screenStateMonitor.unregisterCallback();
        periodicCheckMonitor.disableCheck();
        serviceActionTaker.unregisterCallback();
        serviceThreadPool.shutdown();
    }

    //Overrides for periodic check
    @Override
    public void checkFired() {
        //Perform wifi connectivity test
    }

    //Overrides for screen state check
    @Override
    public void onScreenStateOn() {

    }

    @Override
    public void onScreenStateOff() {

    }


    //Overrides for wifi state
    @Override
    public void wifiStateEnabled() {
        ServiceLog.v("Wifi enabled");
    }

    @Override
    public void wifiStateDisabled() {

    }

    @Override
    public void wifiStateDisconnected() {

    }

    @Override
    public void wifiStateConnected() {

    }

    //Problems
    @Override
    public void wifiPrimaryAPSignalLow() {
        //scan and reconnect to other stronger AP if available.
    }

    @Override
    public void wifiStateHangingOnScanning() {
        //Router not visible -- toggle wifi and re-associate.

    }

    @Override
    public void wifiStateHangingOnObtainingIPAddress() {
        //Disconnect and then re-associate with same router

    }

    @Override
    public void wifiStateHangingOnAuthenticating() {
        //Disconnect and  try to reconnect
    }

    @Override
    public void wifiStateFrequentDropOff() {
        //If ping ponging -- then increase the priority of one network significantly
        //If just one wifi and signal is strong -- check smart network switch
        //If signal is on the fringe, demote this network and connect to a better network if possible.
    }

    @Override
    public void wifiStateErrorAuthenticating() {
        //Tell user that passphrase is wrong

    }

    @Override
    public void wifiStateProblematicSupplicantPattern() {
        //Toggle wifi
    }

    @Override
    public void wifiNetworkConnected() {
        //Perform connectivity test
    }

    @Override
    public void wifiNetworkDisconnected() {
        //Check wifi signal -- if strong signal to a network which was validated to have Internet, then trigger reconnect -- aggressive mode

    }

    @Override
    public void wifiNetworkInvalidOrInactiveOrDormant() {
        //Toggle wifi
    }
}
