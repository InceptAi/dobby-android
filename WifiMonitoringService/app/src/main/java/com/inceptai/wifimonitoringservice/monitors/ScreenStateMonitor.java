package com.inceptai.wifimonitoringservice.monitors;

/**
 * Created by vivek on 7/9/17.
 */

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.PowerManager;

import com.inceptai.wifimonitoringservice.actionlibrary.utils.ActionLog;

import java.util.concurrent.atomic.AtomicBoolean;

import static android.content.Context.POWER_SERVICE;

public class ScreenStateMonitor {
    /*
	 * This should only ever be instantiated from a service. Clients can be
	 * whatever, so long as they implement the interface, and use the
	 * setOnScreenStateListener and unsetOnScreenStateListener methods
	 */
    private static final int SCREEN_EVENT_OFF = 0;
    private static final int SCREEN_EVENT_ON = 1;
    private ScreenStateCallback screenStateCallback;
    private ScreenStateIntentReceiver screenStateIntentReceiver;
    private AtomicBoolean screenStateReceiverRegistered;
    private Context context;

    public interface ScreenStateCallback {
        void onScreenStateOn();
        void onScreenStateOff();
    }

    public ScreenStateMonitor(Context context) {
			/*
			 * Register for screen state events
			 *
			 * Note: this Initializer must be used if you want to receive the
			 * intent broadcast: must use the unregister method appropriately in
			 * the context where you instantiated it or leak receiver
			 */
        this.context = context;
        screenStateIntentReceiver = new ScreenStateIntentReceiver();
        screenStateReceiverRegistered = new AtomicBoolean(false);
    }

    public void registerCallback(ScreenStateCallback screenStateCallback) {
        this.screenStateCallback = screenStateCallback;
        if (screenStateReceiverRegistered.compareAndSet(false, true)) {
            registerReceiver();
            //Switch thread here.
            if (screenStateCallback != null) {
                if (isScreenCurrentlyOn()) {
                    screenStateCallback.onScreenStateOn();
                } else {
                    screenStateCallback.onScreenStateOff();
                }
            }
        }
    }

    public void unregisterCallback() {
        this.screenStateCallback = null;
        if (screenStateReceiverRegistered.compareAndSet(true, false)) {
            unregisterReceiver();
        }
    }


    //Discover the public methods -- do we need any
    private void registerReceiver() {
        IntentFilter intentFilter =  getIntentFilter();
        context.registerReceiver(screenStateIntentReceiver, intentFilter);
    }

    private void unregisterReceiver() {
        try {
            context.unregisterReceiver(screenStateIntentReceiver);
        } catch (IllegalArgumentException e) {
            ActionLog.v("Exception while un-registering wifi receiver: " + e);
        }
    }

    private IntentFilter getIntentFilter() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        return filter;
    }

    private boolean isScreenCurrentlyOn() {
        PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
        if (powerManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                return powerManager.isInteractive();
            } else {
                return powerManager.isScreenOn();
            }
        }
        return false;
    }


    private class ScreenStateIntentReceiver extends BroadcastReceiver {
        public ScreenStateIntentReceiver() {

        }

        @Override
        public void onReceive(Context c, Intent intent) {
            final String action = intent.getAction();
            if (screenStateCallback != null) {
                if (action.equals(Intent.ACTION_SCREEN_ON)) {
                    screenStateCallback.onScreenStateOn();
                } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                    screenStateCallback.onScreenStateOff();
                }
            }
        }

    }
}

