package com.inceptai.dobby.expert;

/**
 * Created by arunesh on 6/1/17.
 */

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v7.app.NotificationCompat;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.inceptai.dobby.BuildConfig;
import com.inceptai.dobby.R;
import com.inceptai.dobby.ui.ExpertChatActivity;
import com.inceptai.dobby.utils.DobbyLog;
import com.inceptai.dobby.utils.Utils;

import java.util.Map;

/**
 * A "service" that connects to the expert chat system.
 * This is NOT an Android service.
 */
public class ExpertChatService implements ChildEventListener, ValueEventListener {

    private static final String USER_ROOT = BuildConfig.FLAVOR + "/" + BuildConfig.BUILD_TYPE  + "/" + "users/";
    private static final String CHAT_ROOM_CHILD = BuildConfig.FLAVOR + "_chat_rooms/" + BuildConfig.BUILD_TYPE;
    private static final String FCM_KEY = "fcmToken";
    private static final String NOTIFICATIONS_BASE = "/notifications/messages/";
    private static final String CHAT_NOTIFICATION_TITLE = "You have a new chat message.";

    private static ExpertChatService INSTANCE;

    private String userUuid;
    private String chatRoomPath;
    private String userTokenPath;
    private ChatCallback chatCallback;

    public interface ChatCallback {
        void onMessageAvailable(ExpertChat expertChat);
        void onNoHistoryAvailable();
    }

    private ExpertChatService(String userUuid) {
        this.userUuid = userUuid;
        this.chatRoomPath =  CHAT_ROOM_CHILD + "/" + userUuid;
        this.userTokenPath = USER_ROOT + "/" + userUuid + "/" + FCM_KEY;
        DobbyLog.i("Using chat room ID: " + chatRoomPath);
        initialize();
    }

    public static ExpertChatService fetchInstance(String userUuid) {
        if (INSTANCE == null) {
            INSTANCE = new ExpertChatService(userUuid);
        }
        return INSTANCE;
    }

    public void setCallback(ChatCallback callback) {
        this.chatCallback = callback;
    }

    public void saveFcmToken(String token) {
        // TODO This is BROKEN.
        getFcmTokenReference().setValue(token);
    }

    public void showNotification(Context context, String title, String body, Map<String, String> data) {
        DobbyLog.i("Title: " + title);
        DobbyLog.i(" Body: " + body);
        DobbyLog.i(" Data: " + data);
        Intent intent = new Intent(context, ExpertChatActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(context)
                .setAutoCancel(true)   //Automatically delete the notification
                .setSmallIcon(R.mipmap.ic_launcher) //Notification icon
                .setContentIntent(pendingIntent)
                .setContentTitle(title)
                .setContentText(body)
                .setSound(defaultSoundUri);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0, notificationBuilder.build());
    }

    public void disconnect() {
        getChatReference().removeEventListener((ChildEventListener) this);
    }
    
    private void initialize() {
        DatabaseReference chatReference = getChatReference();
        chatReference.addChildEventListener(this);
        chatReference.addListenerForSingleValueEvent(this);
    }

    public void pushData(ExpertChat expertChat) {
        getChatReference().push().setValue(expertChat);
        sendExpertNotification(getExpertId(), expertChat);
    }

    public void sendExpertNotification(String toExpert, ExpertChat expertChat) {
        ChatNotification chatNotification = new ChatNotification();
        chatNotification.from = userUuid;
        chatNotification.to = toExpert;
        chatNotification.body = expertChat.getText();
        chatNotification.title = CHAT_NOTIFICATION_TITLE;
        chatNotification.fcmIdPath = getFcmIdPathForExpert(toExpert);
        getNotificationReference().push().setValue(chatNotification);
    }

    // TODO
    private String getFcmIdPathForExpert(String expert) {
        return Utils.EMPTY_STRING;
    }

    // TODO
    private String getFcmIdPathForUser(String userUuid) {
        return Utils.EMPTY_STRING;
    }

    // TODO
    private String getExpertId() {
        return Utils.EMPTY_STRING;
    }

    private DatabaseReference getChatReference() {
        return FirebaseDatabase.getInstance().getReference().child(chatRoomPath);
    }

    private DatabaseReference getNotificationReference() {
        return FirebaseDatabase.getInstance().getReference().child(NOTIFICATIONS_BASE);
    }

    private DatabaseReference getFcmTokenReference() {
        return FirebaseDatabase.getInstance().getReference().child(userTokenPath);
    }

    @Override
    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
        if (chatCallback != null) {
            ExpertChat expertChat = parse(dataSnapshot);
            chatCallback.onMessageAvailable(expertChat);
            DobbyLog.i("Got chat message: " + expertChat .getText());
        }
    }

    @Override
    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

    }

    @Override
    public void onChildRemoved(DataSnapshot dataSnapshot) {

    }

    @Override
    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

    }

    @Override
    public void onCancelled(DatabaseError databaseError) {

    }

    private static ExpertChat parse(DataSnapshot dataSnapshot) {
        ExpertChat expertChat = dataSnapshot.getValue(ExpertChat.class);
        return expertChat;
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        if (dataSnapshot == null && chatCallback != null) {
            chatCallback.onNoHistoryAvailable();
        }
    }
}
