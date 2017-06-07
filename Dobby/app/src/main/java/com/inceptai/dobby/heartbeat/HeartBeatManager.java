package com.inceptai.dobby.heartbeat;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.inceptai.dobby.DobbyAnalytics;
import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.utils.DobbyLog;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by vivek on 6/6/17.
 */

@Singleton
public class HeartBeatManager {
    private static final int ALARM_RECEIVER_UNREGISTERED = 0;
    private static final int ALARM_RECEIVER_REGISTERED = 1;
    static final String USER_ID = "UID";
    static final String DAILY_HEARTBEAT_INTENT = "DAILY_HEARTBEAT_INTENT";
    //private static final long DAILY_HEARTBEAT_INTERVAL_MS = AlarmManager.INTERVAL_DAY;
    static final long DAILY_HEARTBEAT_INTERVAL_MS = 5000;
    private PendingIntent pendingIntent;
    private int alaramReceiverState = ALARM_RECEIVER_UNREGISTERED;
    //private AlarmReceiver alarmReceiver;
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
        alarmIntent.putExtra(USER_ID, dobbyApplication.getUserUuid());
        alarmIntent.setAction(DAILY_HEARTBEAT_INTENT);
        pendingIntent = PendingIntent.getBroadcast(dobbyApplication.getApplicationContext(), 0, alarmIntent, 0);
        //alarmReceiver = new AlarmReceiver();
        //registerAlarmReceiver();
    }


    public void setDailyHeartBeat() {
        setDailyHeartBeat(DAILY_HEARTBEAT_INTERVAL_MS);
    }

    void performHeartBeatTask() {
        //Send analytics event
        DobbyLog.v("Sending analytics event to firebase for user " + dobbyApplication.getUserUuid());
        dobbyAnalytics.wifiExpertDailyHeartBeat(dobbyApplication.getUserUuid(), 0, 0);
    }

    private void setDailyHeartBeat(long intervalMs) {
        setAlarm(dobbyApplication.getApplicationContext(), intervalMs);
    }


    private void setAlarm(Context context, long intervalMs) {
        DobbyLog.v("Setting alarm with interval ms " + intervalMs);
        alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction(DAILY_HEARTBEAT_INTENT);
        pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        //alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(),
        //        intervalMs, pendingIntent);
        alarmMgr.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime(),
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



    void setAlarm(Context context) {
        setAlarm(context, DAILY_HEARTBEAT_INTERVAL_MS);
    }

    void cancelAlarm(Context context) {
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
