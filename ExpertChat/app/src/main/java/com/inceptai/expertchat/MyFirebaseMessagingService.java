package com.inceptai.expertchat;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFMService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // Handle data payload of FCM messages.
        Log.e(TAG, "FCM Message Id: " + remoteMessage.getMessageId());
        Log.e(TAG, "FCM Data Message: " + remoteMessage.getData());
        ExpertChatService service = ExpertChatService.fetchInstance(getApplicationContext());
        service.showNotification(getApplicationContext(), remoteMessage.getData());
    }
}
