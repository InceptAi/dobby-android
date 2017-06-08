package com.inceptai.dobby.notifications;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.inceptai.dobby.utils.DobbyLog;

/**
 * Created by arunesh on 6/5/17.
 */

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // TODO: Handle FCM messages here.
        // If the application is in the foreground handle both data and notification messages here.
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated.
        DobbyLog.i("From: " + remoteMessage.getFrom());
        DobbyLog.i("Notification Message Body: " + remoteMessage.getNotification().getBody());
    }
}