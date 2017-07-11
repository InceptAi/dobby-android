package com.inceptai.dobby.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.inceptai.actionlibrary.utils.ActionLog;
import com.inceptai.dobby.service.utils.ServiceAlarm;

/**
 * Created by vivek on 7/9/17.
 */

public class DobbyMonitoringService extends Service {
    private WifiServiceCore wifiServiceCore;

    @Override
    public void onCreate() {
        //Start service watch dog
        super.onCreate();
        wifiServiceCore = WifiServiceCore.create(this, new ServiceThreadPool());
        ServiceAlarm.setRepeatingServiceWatchDogAlarmWithNoDelay(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.hasExtra(ServiceAlarm.ALARM_START_TYPE)) {
                ActionLog.v("Service started with alarm");
            } else {
                ActionLog.v("Service started with start intent");
            }
        }
        wifiServiceCore.startMonitoring();
        // If we get killed, after returning from here, restart
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        //Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
        wifiServiceCore.cleanup();
    }
}
