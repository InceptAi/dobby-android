package com.inceptai.wifimonitoringservice;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.Action;
import com.inceptai.wifimonitoringservice.utils.ServiceAlarm;
import com.inceptai.wifimonitoringservice.utils.ServiceLog;
import com.inceptai.wifimonitoringservice.utils.Utils;

import static android.os.Process.THREAD_PRIORITY_BACKGROUND;

/**
 * Created by vivek on 7/9/17.
 */

public class WifiMonitoringService extends Service {
    public static final String EXTRA_INTENT_NAME = "EXTRA_INTENT_NAME";
    public static final String NOTIFICATION_INFO_INTENT_VALUE = "com.inceptai.WIFI_MONITORING_INFO";
    public static final String EXTRA_NOTIFICATION_TITLE = "EXTRA_NOTIFICATION_TITLE";
    public static final String EXTRA_NOTIFICATION_BODY = "EXTRA_NOTIFICATION_BODY";
    public static final String EXTRA_NOTIFICATION_ID = "EXTRA_NOTIFICATION_ID";
    public static final int WIFI_STATUS_NOTIFICATION_ID = 1000;
    public static final int WIFI_ISSUE_NOTIFICATION_ID = 1001;
    public static final int WIFI_ACTION_NOTIFICATION_ID = 1002;
    public static final String EXTRA_PENDING_INTENT_NAME = "EXTRA_PENDING_INTENT_NAME";

    private Looper mServiceLooper;
    private ServiceHandler mServiceHandler;
    private WifiServiceCore wifiServiceCore;
    // Binder given to clients
    private final IBinder wifiServiceBinder = new WifiServiceBinder();

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            // Normally we would do some work here, like download a file.
            // For our sample, we just sleep for 5 seconds.
            PendingIntent pendingIntent = (PendingIntent)msg.obj;
            if (pendingIntent != null) {
                wifiServiceCore.setPendingIntentForLaunchingNotification(pendingIntent);
            }
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
        if (wifiServiceCore == null) {
            wifiServiceCore = WifiServiceCore.create(this, new ServiceThreadPool());
        }
        Utils.enableBootReceiver(this, WifiMonitoringService.class);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String notificationInfoIntent = null;
        PendingIntent pendingIntent = null;
        if (intent != null) {
            if (intent.hasExtra(ServiceAlarm.ALARM_START_TYPE)) {
                ServiceLog.v("Service started with alarm");
            } else {
                ServiceLog.v("Service started with start intent");
            }
            notificationInfoIntent = intent.getStringExtra(EXTRA_INTENT_NAME);
            pendingIntent = intent.getParcelableExtra(EXTRA_PENDING_INTENT_NAME);
        }
        // For each start request, send a message to start a job and deliver the
        // start ID so we know which request we're stopping when we finish the job
        Message msg = mServiceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = pendingIntent;
        mServiceHandler.sendMessage(msg);
        // If we get killed, after returning from here, restart
        ServiceAlarm.setRepeatingServiceWatchDogAlarmWithNoDelay(this);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return wifiServiceBinder;
    }

    @Override
    public void onDestroy() {
        //Toast.makeText(this, "service done", Toast.LENGTH_SHORT).show();
        //Clear pending alarms
        wifiServiceCore.cleanup();
        wifiServiceCore = null;
        ServiceAlarm.unsetAlarm(this);
        Utils.disableBootReceiver(this, WifiMonitoringService.class);
    }

    public ListenableFuture<ActionResult> repairWifiNetwork(long timeOutMs) {
        if (wifiServiceCore !=  null) {
            return wifiServiceCore.forceRepairWifiNetwork(timeOutMs);
        }
        return null;
    }

    public ListenableFuture<ActionResult> repairWifiNetwork() {
        if (wifiServiceCore !=  null) {
            return wifiServiceCore.forceRepairWifiNetwork(0);
        }
        return null;
    }

    public void cancelRepairOfWifiNetwork() {
        if (wifiServiceCore !=  null) {
            ServiceLog.v("WifiServiceMonitoring: cancelRepair");
            wifiServiceCore.cancelRepairOfWifiNetwork();
        }
    }

    public void sendStatusUpdateNotification() {
        if (wifiServiceCore !=  null) {
            wifiServiceCore.sendStatusUpdateNotification();
        }
    }

    public Action takeAction(ActionRequest actionRequest) {
        return wifiServiceCore.takeAction(actionRequest);
    }

    public void cancelNotifications() {
        wifiServiceCore.cancelNotifications();
    }

    public void pauseNotifications() {
        wifiServiceCore.setNotificationsEnabled(false);
    }

    public void resumeNotifications() {
        wifiServiceCore.setNotificationsEnabled(true);
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class WifiServiceBinder extends Binder {
        public WifiMonitoringService getService() {
            // Return this instance of LocalService so clients can call public methods
            return WifiMonitoringService.this;
        }
    }
}
