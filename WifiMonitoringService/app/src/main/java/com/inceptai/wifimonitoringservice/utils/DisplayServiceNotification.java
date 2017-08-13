package com.inceptai.wifimonitoringservice.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.inceptai.wifimonitoringservice.R;

import static com.inceptai.wifimonitoringservice.WifiMonitoringService.WIFI_STATUS_NOTIFICATION_ID;

/**
 * Created by vivek on 7/18/17.
 */

public class DisplayServiceNotification implements Runnable {
    private String NOTIFICATION_INTENT = "WIFI_SERVICE_NOTIFICATION";
    private Context context;
    private String title;
    private String body;
    private int notificationId;
    private PendingIntent pendingIntent;
    private NotificationManager notificationManager;


    public DisplayServiceNotification(Context context,
                                      String title,
                                      String body,
                                      PendingIntent pendingIntent,
                                      int notificationId) {
        this.context = context;
        this.title = title;
        this.body = body;
        this.notificationId = notificationId;
        this.pendingIntent = pendingIntent;
        notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public void run() {
        makeNotification(context);
    }

    public DisplayServiceNotification(Context context) {
        this.context = context;
        notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private void makeNotification(Context context) {
        Intent intent = new Intent(NOTIFICATION_INTENT);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, WIFI_STATUS_NOTIFICATION_ID, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(context)
                .setContentTitle("Notification Title")
                .setContentText("Sample Notification Content")
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_wifimonitoring_notif);
        Notification n;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            n = builder.build();
        } else {
            n = builder.getNotification();
        }
        n.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        notificationManager.notify(WIFI_STATUS_NOTIFICATION_ID, n);
    }
}