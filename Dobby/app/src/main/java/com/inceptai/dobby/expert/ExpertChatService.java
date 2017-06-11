package com.inceptai.dobby.expert;

/**
 * Created by arunesh on 6/1/17.
 */

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.TaskStackBuilder;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static android.support.v4.app.NotificationCompat.VISIBILITY_PUBLIC;

/**
 * A "service" that connects to the expert chat system.
 * This is NOT an Android service.
 */
public class ExpertChatService implements ChildEventListener, ValueEventListener {

    private static final String USER_ROOT = BuildConfig.FLAVOR + "/" + BuildConfig.BUILD_TYPE  + "/" + "users/";
    private static final String CHAT_ROOM_CHILD = BuildConfig.FLAVOR + "_chat_rooms/" + BuildConfig.BUILD_TYPE;
    private static final String FCM_KEY = "fcmToken";
    private static final String ASSIGNED_EXPERT_KEY = "assignedExpert";
    private static final String NOTIFICATIONS_BASE = "/notifications/messages/";
    private static final String CHAT_NOTIFICATION_TITLE = "You have a new chat message.";
    private static final String EXPERT_BASE = "/expert";

    private static final long ETA_OFFLINE = 24L * 60L * 60L;  // 24 hours.
    private static final long ETA_ONLINE = 2L * 60L; // 2 minutes or less.
    private static final long ETA_PRESENT = 20L * 60L; // 20 minutes or less.

    private static ExpertChatService INSTANCE;

    private String userUuid;
    private String chatRoomPath;
    private String userTokenPath;
    private String assignedExpertUsernamePath;
    private String assignedExpertUsername;
    private ChatCallback chatCallback;
    private List<ExpertData> expertList;
    private long currentEtaSeconds;
    private boolean listenerConnected = false; // whether firebase listerners are registered.

    // TODO Use this field.
    private boolean isChatEmpty;

    public interface ChatCallback {
        void onMessageAvailable(ExpertChat expertChat);
        void onNoHistoryAvailable();
        void onEtaUpdated(long newEtaSeconds, boolean isPresent);
        void onEtaAvailable(long newEtaSeconds, boolean isPresent);
    }

    private ExpertChatService(String userUuid) {
        this.userUuid = userUuid;
        this.chatRoomPath =  CHAT_ROOM_CHILD + "/" + userUuid;
        this.userTokenPath = USER_ROOT + "/" + userUuid + "/" + FCM_KEY;
        this.assignedExpertUsernamePath = USER_ROOT + "/" + userUuid + "/" + ASSIGNED_EXPERT_KEY;
        expertList = new ArrayList<>();
        DobbyLog.i("Using chat room ID: " + chatRoomPath);
        isChatEmpty = true;
        currentEtaSeconds = ETA_PRESENT;
    }

    public static ExpertChatService fetchInstance(String userUuid) {
        if (INSTANCE == null) {
            INSTANCE = new ExpertChatService(userUuid);
        }
        return INSTANCE;
    }

    public void addAssignedExpertNameListener() {
        getAssignedExpertReference().addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                assignedExpertUsername = (String) dataSnapshot.getValue();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void setCallback(ChatCallback callback) {
        this.chatCallback = callback;
    }

    public void unregisterChatCallback() {
        this.chatCallback = null;
    }

    public void saveFcmToken(String token) {
        getFcmTokenReference().setValue(token);
    }

    public void showNotification(Context context, Map<String, String> data) {
        String title = data.get("titleText");
        String body = data.get("bodyText");
        String pushId = data.get("messagePushId");
        String source = data.get("source");
        DobbyLog.i("Title: " + title);
        DobbyLog.i(" Body: " + body);
        DobbyLog.i(" Data: " + data);
        Intent intent = new Intent(context, ExpertChatActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(ExpertChatActivity.INTENT_NOTIF_SOURCE, source);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        // Adds the back stack
        stackBuilder.addParentStack(ExpertChatActivity.class);
        // Adds the Intent to the top of the stack
        stackBuilder.addNextIntent(intent);
        // Gets a PendingIntent containing the entire back stack
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(context)
                .setAutoCancel(true)   //Automatically delete the notification
                .setSmallIcon(R.drawable.ic_person_64dp) //Notification icon
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.wifi_doc_launcher))
                .setContentIntent(resultPendingIntent)
                .setContentTitle(title)
                .setContentText(body)
                .setVisibility(VISIBILITY_PUBLIC)
                .setSound(defaultSoundUri);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0, notificationBuilder.build());

        if (pushId != null && !pushId.isEmpty()) {
            getNotificationReference().child(pushId).removeValue();
        }
    }

    public void disconnect() {
        getChatReference().removeEventListener((ChildEventListener) this);
        getChatReference().removeEventListener((ValueEventListener) this);
        listenerConnected = false;
    }

    public boolean isListenerConnected() {
        return listenerConnected;
    }

    public void fetchChatMessages() {
        DatabaseReference chatReference = getChatReference();
        chatReference.addChildEventListener(this);
        chatReference.addValueEventListener(this);
        readExpertList();
        addAssignedExpertNameListener();
        listenerConnected = true;
    }

    public void pushData(ExpertChat expertChat) {
        getChatReference().push().setValue(expertChat);
        if (assignedExpertUsername == null || assignedExpertUsername.isEmpty()) {
            sendExpertNotificationToAll(expertChat);
        } else {
            sendExpertNotification(assignedExpertUsername, expertChat);
        }
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

    public void sendExpertNotificationToAll(ExpertChat expertChat) {
        for (ExpertData expertData : expertList) {
            sendExpertNotification(expertData.getAvatar(), expertChat);
        }
    }

    // TODO
    private String getFcmIdPathForExpert(String expert) {
        return EXPERT_BASE + "/" + expert + "/" + FCM_KEY;
    }

    // TODO
    private String getFcmIdPathForUser(String userUuid) {
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

    private DatabaseReference getAssignedExpertReference() {
        return FirebaseDatabase.getInstance().getReference().child(assignedExpertUsernamePath);
    }

    private void readExpertList() {
        FirebaseDatabase.getInstance().getReference().child(EXPERT_BASE).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                expertList.clear();
                DobbyLog.i("ValueEventListener called for expert data change.");
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                    ExpertData data = dataSnapshot1.getValue(ExpertData.class);
                    expertList.add(data);
                }
                updateEta();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void updateEta() {
        long bestEtaSeconds = ETA_OFFLINE;
        boolean expertAssigned = assignedExpertUsername != null && !assignedExpertUsername.isEmpty();
        for (ExpertData expertData : expertList) {
            if (expertAssigned && assignedExpertUsername.equals(expertData.getAvatar())) {
                bestEtaSeconds = expertData.etaSeconds;
            } else if (!expertAssigned) {
                bestEtaSeconds = Math.min(expertData.etaSeconds, bestEtaSeconds);
            }
        }
        if (bestEtaSeconds != currentEtaSeconds) {
            // Update available
            currentEtaSeconds = bestEtaSeconds;
            if (chatCallback != null) {
                chatCallback.onEtaUpdated(currentEtaSeconds, currentEtaSeconds != ETA_OFFLINE);
            }
        } else {
            if (chatCallback != null) {
                chatCallback.onEtaAvailable(currentEtaSeconds, currentEtaSeconds != ETA_OFFLINE);
            }
        }
    }
    @Override
    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
        if (chatCallback != null) {
            isChatEmpty = false;
            if (dataSnapshot.getKey().equals(ASSIGNED_EXPERT_KEY)) {
                assignedExpertUsername = (String) dataSnapshot.getValue();
            } else {
                ExpertChat expertChat = parse(dataSnapshot);
                chatCallback.onMessageAvailable(expertChat);
                DobbyLog.i("Got chat message: " + expertChat.getText());
            }
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
        if (dataSnapshot == null || dataSnapshot.getValue() == null) {
            if (chatCallback != null) {
                chatCallback.onNoHistoryAvailable();
            }
        }
    }
}
