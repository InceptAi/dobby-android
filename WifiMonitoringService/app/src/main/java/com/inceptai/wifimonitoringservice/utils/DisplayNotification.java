package com.inceptai.wifimonitoringservice.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.inceptai.wifimonitoringservice.R;

/**
 * Created by vivek on 7/18/17.
 */

public class DisplayNotification implements Runnable {
    private static final String NOTIFICATION_INTENT = "com.inceptai.wifimonitoring.NOTIFICATION";
    private static final int USER_ACTION_NOTIFICATION_ID = 1;
    private static final int SERVICE_ACTION_NOTIFICATION_ID = 2;
    private static final int WIFI_STATUS_NOTIFICATION_ID = 3;
    Context context;
    NotificationManager notificationManager;

    public DisplayNotification(Context context) {
        this.context = context;
        notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public void run() {
        makeNotification(context);
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