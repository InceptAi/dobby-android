package com.inceptai.wifimonitoringservice.monitors;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.inceptai.actionlibrary.utils.ActionLog;
import com.inceptai.wifimonitoringservice.utils.ServiceAlarm;

/**
 * Created by vivek on 7/10/17.
 */

public class PeriodicCheckMonitor {
    private static final String PERIODIC_CHECK_INTENT = "com.inceptai.dobby.PERIODIC_CHECK_INTENT";
    private static final long PERIODIC_CHECK_INTERVAL_MS = 5 * 60 * 1000;
    private PendingIntent pendingIntent;
    private Context context;
    private Intent intent;
    private PeriodicCheckCallback periodicCheckCallback;
    private PeriodicCheckReceiver periodicCheckReceiver;
    private boolean periodicCheckReceiverRegistered = false;

    public interface PeriodicCheckCallback {
        void checkFired();
    }

    public PeriodicCheckMonitor(Context context) {
        this.context = context;
        intent = new Intent(PERIODIC_CHECK_INTENT);
        pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        periodicCheckReceiver = new PeriodicCheckReceiver();
    }


    public void enableCheck(long waitBeforeCheck, PeriodicCheckCallback periodicCheckCallback) {
        registerCallback(periodicCheckCallback);
        if (!ServiceAlarm.alarmExists(context, intent)) {
            ServiceAlarm.addAlarm(context,
                    waitBeforeCheck, true, PERIODIC_CHECK_INTERVAL_MS, pendingIntent);
        }
    }

    public void disableCheck() {
        unRegisterCallback();
        ServiceAlarm.unsetAlarm(context, pendingIntent);
    }

    //Private calls
    private void registerCallback(PeriodicCheckCallback periodicCheckCallback) {
        this.periodicCheckCallback = periodicCheckCallback;
        registerReceiver();
    }

    private void unRegisterCallback() {
        this.periodicCheckCallback = null;
        if (periodicCheckReceiverRegistered) {
            unregisterReceiver();
        }
    }

    private void registerReceiver() {
        IntentFilter intentFilter =  getIntentFilter();
        context.registerReceiver(periodicCheckReceiver, intentFilter);
        periodicCheckReceiverRegistered = true;
    }

    private void unregisterReceiver() {
        try {
            context.unregisterReceiver(periodicCheckReceiver);
        } catch (IllegalArgumentException e) {
            ActionLog.v("Exception while un-registering wifi receiver: " + e);
        }
        periodicCheckReceiverRegistered = false;
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
                ActionLog.v("OnReceive Alarm Manager.");
                if (periodicCheckCallback != null) {
                    periodicCheckCallback.checkFired();
                }
            }
        }
    }

}
