package com.inceptai.wifimonitoringservice.utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.support.v7.app.NotificationCompat;

import com.inceptai.wifimonitoringservice.R;
import com.inceptai.wifimonitoringservice.WifiMonitoringService;

import static android.support.v4.app.NotificationCompat.VISIBILITY_PUBLIC;

/**
 * Created by vivek on 7/18/17.
 */

public class DisplayNotification implements Runnable {
    private Context context;
    private String title;
    private String body;
    private int notificationId;
    private PendingIntent pendingIntent;
    private NotificationManager notificationManager;

    public DisplayNotification(Context context, String title, String body, int notificationId,
                               PendingIntent pendingIntent) {
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
        makeNotification();
    }

    public void makeNotification() {
        if (title == null || body == null) {
            ServiceLog.v("Not showing notification since title/body is null");
        }
        //int iconResource = Utils.WIFIEXPERT_FLAVOR.equals(BuildConfig.FLAVOR) ? R.mipmap.wifi_expert_launcher : R.mipmap.wifi_doc_launcher;
        //PendingIntent pendingIntent = Utils.getPendingIntentForNotification(context, null);
        //Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(context)
                .setAutoCancel(true)   //Automatically delete the notification
                .setSmallIcon(R.drawable.ic_wifimonitoring_notif) //Notification icon
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_wifi_service_launcher))
                .setContentIntent(pendingIntent)
                .setContentTitle(title)
                .setContentText(body)
                .setVisibility(VISIBILITY_PUBLIC);
                //.setSound(defaultSoundUri);
        // Vibration
        //notificationBuilder.setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 });
        // LED
        //notificationBuilder.setLights(Color.WHITE, 3000, 3000);
        //notificationBuilder.setOngoing(true);
        Notification notification = notificationBuilder.build();
        if (notificationId == WifiMonitoringService.WIFI_STATUS_NOTIFICATION_ID) {
            notification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        }
        notificationManager.notify(notificationId, notification);
        //return notification;
    }
}

