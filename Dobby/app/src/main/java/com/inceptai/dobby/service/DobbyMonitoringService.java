package com.inceptai.dobby.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.widget.Toast;

import com.inceptai.actionlibrary.utils.ActionLog;
import com.inceptai.dobby.service.utils.ServiceAlarm;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

/**
 * Created by vivek on 7/9/17.
 */

public class DobbyMonitoringService extends Service {
    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private WifiServiceCore wifiServiceCore;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            // Normally we would do some work here, like download a file.
            // For our sample, we just sleep for 5 seconds.
            wifiServiceCore.startMonitoring();
        }
    }

    @Override
    public void onCreate() {
        // Start up the thread running the service.  Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block.  We also make it
        // background priority so CPU-intensive work will not disrupt our UI.
        HandlerThread thread = new HandlerThread("ServiceStartArguments", THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
        //Start service watch dog
        wifiServiceCore = WifiServiceCore.create(this, new ServiceThreadPool());
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
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        mServiceHandler.sendMessage(msg);
        // If we get killed, after returning from here, restart
        ServiceAlarm.setRepeatingServiceWatchDogAlarmWithNoDelay(this);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
        wifiServiceCore.cleanup();
    }
}
