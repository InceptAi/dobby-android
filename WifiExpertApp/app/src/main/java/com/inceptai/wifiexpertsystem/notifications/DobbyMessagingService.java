package com.inceptai.wifiexpertsystem.notifications;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.inceptai.wifiexpertsystem.DobbyApplication;
import com.inceptai.wifiexpertsystem.analytics.DobbyAnalytics;
import com.inceptai.wifiexpertsystem.expert.ExpertChatService;
import com.inceptai.wifiexpertsystem.utils.DobbyLog;

import javax.inject.Inject;

/**
 * Created by arunesh on 6/5/17.
 */

public class DobbyMessagingService extends FirebaseMessagingService {

    @Inject
    DobbyAnalytics dobbyAnalytics;
    @Inject
    ExpertChatService expertChatService;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        DobbyApplication application = (DobbyApplication) getApplicationContext();
        application.getProdComponent().inject(this);
        // TODO: Handle FCM messages here.
        // If the application is in the foreground handle both data and notification messages here.
        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated.
        DobbyLog.i("From: " + remoteMessage.getFrom());
        expertChatService.showNotification(getApplicationContext(), remoteMessage.getData());
        dobbyAnalytics.expertChatNotificationShown();
    }
}
