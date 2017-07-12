package com.inceptai.wifimonitoringservice.utils;

/**
 * Created by vivek on 7/9/17.
 */

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.inceptai.wifimonitoringservice.WifiMonitoringService;

public final class ServiceAlarm extends Object {

    /*
     * Notifies Service that start intent comes from ServiceAlarm
     */
    public static final String ALARM_START_TYPE = "ALARM_SERVICE_START";

    public static final long PERIOD = 300000;
    public static final long SERVICE_START_DELAY_FROM_BOOT = 30000;
    private static final long NO_DELAY = 0;

    public static boolean alarmExists(Context context, Intent intent) {
        return (PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_NO_CREATE) != null);
    }

    public static PendingIntent createPendingIntent(Context context,
                                                    int flag) {
        Intent intent = new Intent(context, WifiMonitoringService.class);
        intent.setFlags(Intent.FLAG_FROM_BACKGROUND);
        intent.putExtra(ALARM_START_TYPE, ALARM_START_TYPE);
        PendingIntent pendingintent = PendingIntent.getService(context, 0,
                intent, flag);
        return pendingintent;
    }

    public static void setRepeatingServiceWatchDogAlarmWithNoDelay(Context context) {
        addAlarm(context, 0, true, PERIOD, createPendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT));
    }

    public static void setOneTimServiceWatchDogAlarmWithDelay(Context context) {
        addAlarm(context, SERVICE_START_DELAY_FROM_BOOT, false, 0, createPendingIntent(context, PendingIntent.FLAG_UPDATE_CURRENT));
    }

    public static void addAlarm(Context context,
                                long initialDelay,
                                boolean repeating,
                                long period,
                                PendingIntent pendingIntent) {
        registerAlarm(context, initialDelay, repeating, period, pendingIntent);
    }

    public static void unsetAlarm(Context c) {
        AlarmManager mgr = (AlarmManager) c
                .getSystemService(Context.ALARM_SERVICE);
        mgr.cancel(createPendingIntent(c, 0));
    }

    public static void unsetAlarm(Context c, PendingIntent p) {
        AlarmManager mgr = (AlarmManager) c
                .getSystemService(Context.ALARM_SERVICE);
        mgr.cancel(p);
    }


    private static void registerAlarm(Context context,
                                      long delay,
                                      boolean repeating,
                                      long period,
                                      PendingIntent pendingIntent) {
        AlarmManager mgr = (AlarmManager) context
                .getSystemService(Context.ALARM_SERVICE);
        if (repeating)
            mgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + delay, period, pendingIntent);
        else
            mgr.set(AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime() + delay, pendingIntent);
    }

}

