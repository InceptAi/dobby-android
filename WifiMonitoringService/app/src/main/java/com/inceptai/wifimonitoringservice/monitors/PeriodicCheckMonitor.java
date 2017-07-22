package com.inceptai.wifimonitoringservice.monitors;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.inceptai.wifimonitoringservice.utils.ServiceAlarm;
import com.inceptai.wifimonitoringservice.utils.ServiceLog;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by vivek on 7/10/17.
 */

public class PeriodicCheckMonitor {
    private static final String PERIODIC_CHECK_INTENT = "com.inceptai.dobby.PERIODIC_CHECK_INTENT";
    private PendingIntent pendingIntent;
    private Context context;
    private Intent intent;
    private PeriodicCheckCallback periodicCheckCallback;
    private PeriodicCheckReceiver periodicCheckReceiver;
    private AtomicBoolean periodicCheckReceiverRegistered;

    public interface PeriodicCheckCallback {
        void checkFired();
    }

    public PeriodicCheckMonitor(Context context) {
        this.context = context;
        intent = new Intent(PERIODIC_CHECK_INTENT);
        pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        periodicCheckReceiver = new PeriodicCheckReceiver();
        periodicCheckReceiverRegistered = new AtomicBoolean(false);
    }

    public void cleanup() {
        disableCheck();
    }

    public void enableCheck(long waitBeforeCheck, long checkPeriod, PeriodicCheckCallback periodicCheckCallback) {
        disableCheck();
        registerCallback(periodicCheckCallback);
        ServiceAlarm.addAlarm(context, waitBeforeCheck, true, checkPeriod, pendingIntent);
    }

    public void disableCheck() {
        unRegisterCallback();
        if (ServiceAlarm.alarmExists(context, intent)) {
            ServiceAlarm.unsetAlarm(context, pendingIntent);
        }
    }

    //Private calls
    private void registerCallback(PeriodicCheckCallback periodicCheckCallback) {
        this.periodicCheckCallback = periodicCheckCallback;
        if (periodicCheckReceiverRegistered.compareAndSet(false, true)) {
            registerReceiver();
        }
    }

    private void unRegisterCallback() {
        this.periodicCheckCallback = null;
        if (periodicCheckReceiverRegistered.compareAndSet(true, false)) {
            unregisterReceiver();
        }
    }

    private void registerReceiver() {
        IntentFilter intentFilter =  getIntentFilter();
        context.registerReceiver(periodicCheckReceiver, intentFilter);
    }

    private void unregisterReceiver() {
        try {
            context.unregisterReceiver(periodicCheckReceiver);
        } catch (IllegalArgumentException e) {
            ServiceLog.v("Exception while un-registering wifi receiver: " + e);
        }
    }

    private IntentFilter getIntentFilter() {
        return new IntentFilter(PERIODIC_CHECK_INTENT);
    }

    private class PeriodicCheckReceiver extends BroadcastReceiver {

        public PeriodicCheckReceiver() {}

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(PERIODIC_CHECK_INTENT)) {
                ServiceLog.v("OnReceive Alarm Manager.");
                if (periodicCheckCallback != null) {
                    periodicCheckCallback.checkFired();
                }
            }
        }
    }

}
