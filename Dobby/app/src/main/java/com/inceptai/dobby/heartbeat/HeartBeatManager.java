package com.inceptai.dobby.heartbeat;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.inceptai.dobby.DobbyAnalytics;
import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.utils.DobbyLog;
import com.inceptai.dobby.utils.Utils;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by vivek on 6/6/17.
 */

@Singleton
public class HeartBeatManager {
    static final String DAILY_HEARTBEAT_INTENT = "DAILY_HEARTBEAT_INTENT";
    static final String LAST_HEARTBEAT_TIMESTAMP_MS = "LAST_HEART_BEAT_TS_MS";
    private static final long DAILY_HEARTBEAT_INTERVAL_MS = AlarmManager.INTERVAL_DAY;

    private PendingIntent pendingIntent;

    private AlarmManager alarmMgr;

    @Inject
    DobbyApplication dobbyApplication;
    @Inject
    DobbyAnalytics dobbyAnalytics;

    @Inject
    HeartBeatManager(DobbyApplication dobbyApplication, DobbyAnalytics dobbyAnalytics) {
        this.dobbyApplication = dobbyApplication;
        this.dobbyAnalytics = dobbyAnalytics;
        Intent alarmIntent = new Intent(dobbyApplication.getApplicationContext(), AlarmReceiver.class);
        alarmIntent.setAction(DAILY_HEARTBEAT_INTENT);
        pendingIntent = PendingIntent.getBroadcast(dobbyApplication.getApplicationContext(), 0, alarmIntent, 0);
        //registerAlarmReceiver();
    }


    public void setDailyHeartBeat() {
        setAlarm(dobbyApplication.getApplicationContext(), DAILY_HEARTBEAT_INTERVAL_MS);
    }

    void performHeartBeatTask() {
        //Send analytics event
        DobbyLog.v("Sending analytics event to firebase for user " + dobbyApplication.getUserUuid());
        dobbyAnalytics.wifiExpertDailyHeartBeat(dobbyApplication.getUserUuid(), 0, 0);
        Utils.saveSharedSetting(dobbyApplication.getApplicationContext(), LAST_HEARTBEAT_TIMESTAMP_MS, System.currentTimeMillis());
    }

    void setAlarm(Context context) {
        setAlarm(context, DAILY_HEARTBEAT_INTERVAL_MS);
    }

    private void setAlarm(Context context, long intervalMs) {
        //Check the time since last alarm event
        long intervalTillNextEventMs = 0;
        long lastHeartBeatTimeStampMs = Utils.readSharedSetting(context, LAST_HEARTBEAT_TIMESTAMP_MS, 0);
        if (lastHeartBeatTimeStampMs > 0) {
            long gapSinceLastHeartBeatMs = System.currentTimeMillis() - lastHeartBeatTimeStampMs;
            if (gapSinceLastHeartBeatMs < intervalMs) {
                //We can't fire right now. Fire after some time as follows
                intervalTillNextEventMs = intervalMs - gapSinceLastHeartBeatMs;
            }
        }
        setAlarm(context, intervalMs, intervalTillNextEventMs); //Trigger one event right away
    }

    private void setAlarm(Context context, long intervalMs, long nextEventAfterMs) {
        DobbyLog.v("Setting alarm with interval ms, next firing after  " + intervalMs + ", " + nextEventAfterMs);
        alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        //First cancel any pending alarms
        cancelAlarm(context);
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(DAILY_HEARTBEAT_INTENT);
        pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + nextEventAfterMs,
                intervalMs, pendingIntent);

        /*
        //Turning on boot stuff
        ComponentName receiver = new ComponentName(context, AlarmReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
                */
    }



    private void cancelAlarm(Context context) {
        if (alarmMgr != null) {
            alarmMgr.cancel(pendingIntent);
        }

        // Disable {@code SampleBootReceiver} so that it doesn't automatically restart the
        // alarm when the device is rebooted.
        /*
        ComponentName receiver = new ComponentName(context, AlarmReceiver.class);
        PackageManager pm = context.getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
                */
    }


    /*
    private void registerAlarmReceiver() {
        dobbyApplication.getApplicationContext().registerReceiver(alarmReceiver, new IntentFilter(DAILY_HEARTBEAT_INTENT));
        alaramReceiverState = ALARM_RECEIVER_REGISTERED;
    }

    private void unregisterAlarmReceiver() {
        if (alaramReceiverState != ALARM_RECEIVER_REGISTERED) {
            return;
        }

        try {
            dobbyApplication.getApplicationContext().unregisterReceiver(alarmReceiver);
        } catch (IllegalArgumentException e) {
            DobbyLog.v("Exception while unregistering alarm receiver: " + e);
        }
        alaramReceiverState = ALARM_RECEIVER_UNREGISTERED;
    }
    */

}
