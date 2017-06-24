package com.inceptai.expertchat;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Map;

import static com.inceptai.expertchat.Utils.EMPTY_STRING;
import static com.inceptai.expertchat.Utils.TAG;

/**
 * Created by arunesh on 6/9/17.
 */

public class ExpertChatService implements SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String CHAT_ROOM_SUFFIX = "_chat_rooms";
    public static final String USERCHAT_NOTIFICATION_TITLE = "You have a new message from a Wifi Expert";
    public static final String ASSIGNED_EXPERT_KEY = "assignedExpert";


    private static final String SELF_NOTIFICATION_TITLE = "You have a new chat message.";
    private static final String EXPERT_BASE = "/expert";
    private static final String NOTIFICATIONS_BASE = "/notifications/messages/";
    private static final String FCM_KEY = "fcmToken";
    private static final String NOTIF_PUSHID_KEY = "pushId";
    private static final String USERS_BASE = "users";

    private static final String DEFAULT_FLAVOR = Utils.WIFIDOC_FLAVOR;
    private static final String DEFAULT_BUILD_TYPE = Utils.BUILD_TYPE_DEBUG;

    private static final long ETA_OFFLINE = 24L * 60L * 60L;  // 24 hours.
    private static final long ETA_ONLINE = 2L * 60L; // 2 minutes or less.
    private static final long ETA_PRESENT = 20L * 60L; // 20 minutes or less.

    private static ExpertChatService INSTANCE;

    private ExpertData expertData;
    private Context context;
    private String avatar;
    private String expertFcmToken;
    private FirebaseUser firebaseUser;
    private String flavor;
    private String buildType;
    private String selectedUserId;
    private UserData selectedUserData;
    private boolean pendingFcmTokenSaveOperation = false;
    private long expertEtaSeconds = ETA_PRESENT;
    private OnExpertDataFetched expertDataFetchedCallback;
    private NotificationRecents notificationRecents;
    private int lastNotificationId = 0;
    private String disableNotificationsForUuid = EMPTY_STRING;

    public interface OnExpertDataFetched {
        void onExpertData(ExpertData expertData);
    }

    private ExpertChatService(Context context) {
        this.context = context;
        flavor = Utils.readSharedSetting(context, Utils.SELECTED_FLAVOR, DEFAULT_FLAVOR);
        selectedUserId = Utils.readSharedSetting(context, Utils.SELECTED_USER_UUID, EMPTY_STRING);
        buildType = Utils.readSharedSetting(context, Utils.SELECTED_BUILD_TYPE, DEFAULT_BUILD_TYPE);
        expertFcmToken = Utils.readSharedSetting(context, Utils.PREF_FCM_TOKEN, EMPTY_STRING);
        loadExpertOnlineFlag();
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPref.registerOnSharedPreferenceChangeListener(this);
        notificationRecents = new NotificationRecents();
    }

    public static ExpertChatService fetchInstance(Context context) {
        if (INSTANCE == null) {
            INSTANCE = new ExpertChatService(context);
        }
        return INSTANCE;
    }

    public NotificationRecents getNotificationRecents() {
        return notificationRecents;
    }

    public void persistFcmToken(String expertFcmToken) {
        this.expertFcmToken = expertFcmToken;
        if (avatar != null && !avatar.isEmpty()) {
            String tokenChild = EXPERT_BASE + "/" + avatar + "/" + FCM_KEY;
            FirebaseDatabase.getInstance().getReference().child(tokenChild).setValue(expertFcmToken);
            Log.i(TAG, "Writing token to: " + tokenChild);
        } else {
            pendingFcmTokenSaveOperation = true;
        }
        Utils.saveSharedSetting(context, Utils.PREF_FCM_TOKEN, expertFcmToken);
    }

    public void setExpertDataFetchedCallback(OnExpertDataFetched expertDataFetchedCallback) {
        this.expertDataFetchedCallback = expertDataFetchedCallback;
    }

    public void disableNotifications(String userUuid) {
        disableNotificationsForUuid = userUuid;
    }

    public void enableNotifications() {
        disableNotificationsForUuid = EMPTY_STRING;
    }

    public void clearExpertDataFetchedCallback() {
        this.expertDataFetchedCallback = null;
    }

    public String getPathForUserChat(String userUuid) {
        UserData userData = UserDataBackend.fetchUser(userUuid);
        if (userData != null && userData.hasBuildTypeAndFlavor()) {
            return getChatRoomForFlavor(userData.appFlavor) + "/" + userData.buildType + "/" + userUuid + "/";
        }
        return flavor + CHAT_ROOM_SUFFIX + "/" + buildType + "/" + userUuid + "/";
    }

    public String getPathForUserProfile(String userUuid) {
        return flavor + "/" + buildType + "/" + USERS_BASE + "/" + userUuid + "/";
    }

    public String getChatRoomBase() {
        return flavor + CHAT_ROOM_SUFFIX + "/" + buildType + "/";
    }

    public void setFirebaseUser(FirebaseUser firebaseUser) {
        this.firebaseUser = firebaseUser;
    }

    private void persistExpertData(FirebaseUser firebaseUser) {
        ExpertData expertData = new ExpertData();
        expertData.email = firebaseUser.getEmail();
        expertData.name = firebaseUser.getDisplayName();
        expertData.avatar = avatar;
        expertData.fcmToken = expertFcmToken;
        expertData.etaSeconds = expertEtaSeconds;
        expertData.selectedUuid = selectedUserId;
        if (expertFcmToken != null && !expertFcmToken.isEmpty() && pendingFcmTokenSaveOperation) {
            pendingFcmTokenSaveOperation = false;
        }
        if (avatar != null && !avatar.isEmpty()) {
            FirebaseDatabase.getInstance().getReference().child(EXPERT_BASE + "/" + avatar).setValue(expertData);
        }
    }

    public void fetchAvatar(final FirebaseUser firebaseUser, OnExpertDataFetched callback) {
        this.firebaseUser = firebaseUser;
        this.expertDataFetchedCallback = callback;
        FirebaseDatabase.getInstance().getReference().child(EXPERT_BASE).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot dataSnapshot1 : dataSnapshot.getChildren()) {
                    ExpertData expertData = dataSnapshot1.getValue(ExpertData.class);
                    if (expertData != null && expertData.email != null && expertData.email.equals(firebaseUser.getEmail())) {
                        loadExpertData(expertData);
                        if (expertDataFetchedCallback != null) {
                            expertDataFetchedCallback.onExpertData(expertData);
                        }
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public void sendUserNotification(String toUser, ExpertChat expertChat) {
        if (expertChat.isHashtagMessage()) {
            Log.i(TAG, "Not sending notification for hash tag message: " + expertChat.getText());
            return;
        }
        ChatNotification chatNotification = new ChatNotification();
        chatNotification.from = avatar;
        chatNotification.to = toUser;
        chatNotification.body = expertChat.getText();
        chatNotification.title = USERCHAT_NOTIFICATION_TITLE;
        chatNotification.fcmIdPath = getFcmIdPathForUser(toUser);
        getNotificationReference().push().setValue(chatNotification);
        notificationRecents.notificationSent(chatNotification);
    }

    public void setAssignedExpert() {
        if (avatar != null && !avatar.isEmpty()) {
            FirebaseDatabase.getInstance().getReference().child(getPathForUserProfile(selectedUserId) + ASSIGNED_EXPERT_KEY).setValue(avatar);
        }
    }

    public void setSelectedUserId(String userUuid) {
        this.selectedUserId = userUuid;
        selectedUserData = UserDataBackend.createOrFetchUser(userUuid);
        if (selectedUserData == null) {
            Log.e(TAG, "Bad user ID:" + userUuid);
        }
        persistExpertData(firebaseUser);
    }

    public void showNotification(Context context, Map<String, String> data) {
        String fromUuid = data.get("source");
        String title = data.get("titleText");
        String body = data.get("bodyText");
        Log.i(TAG, "Title: " + title);
        Log.i(TAG, " Body: " + body);
        Log.i(TAG, " Data: " + data);
        Log.i(TAG, " From User UUID: " + fromUuid);
        Log.i(TAG, "Last notification ID: " + lastNotificationId);

        if (disableNotificationsForUuid.equals(fromUuid)) {
            Log.i(TAG, "Skipping notification for uuid: " + disableNotificationsForUuid);
            return;
        }

        final UserData userData = notificationRecents.saveIncomingNotification(data);
        if (userData == null) {
            Log.e(TAG, "Bad user data. Dumping notification.");
            return;
        }
        boolean betterTitle = userData.hasBuildTypeAndFlavor();
        if (betterTitle) {
            title = createNotifiationTitleFor(userData, title);
        }

        final NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        final NotificationCompat.Builder builder = buildNotification(context, data, title);
        int notificationId = fromUuid.hashCode();
        notificationManager.notify(notificationId, builder.build());
        userData.lastNotificationId = notificationId;
        lastNotificationId ++;

        String pushId = data.get(NOTIF_PUSHID_KEY);
        if (pushId != null && !pushId.isEmpty()) {
            getNotificationReference().child(pushId).removeValue();
        }

        // Set up a renotification if needed.
        if (!betterTitle) {
            ListenableFuture<UserData> future = UserDataBackend.computeUserAppFlavorAndBuild(userData);
            if (future != null) {
                future.addListener(new Runnable() {
                    @Override
                    public void run() {
                        reNotify(userData, builder);
                    }
                }, ExpertChatThreadpool.get().getExecutorService());
            }
        }
    }

    private void reNotify(UserData userData, NotificationCompat.Builder builder) {
        String title = createNotifiationTitleFor(userData, EMPTY_STRING);
        if (!title.isEmpty()) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            builder.setContentTitle(title);
            notificationManager.notify(userData.lastNotificationId, builder.build());
        }
    }

    private DatabaseReference getNotificationReference() {
        return FirebaseDatabase.getInstance().getReference().child(NOTIFICATIONS_BASE);
    }

    private String getFcmIdPathForUser(String userUuid) {
        String userRoot = flavor + "/" + buildType + "/" + "users/";
        return userRoot + "/" + userUuid + "/" + FCM_KEY;
    }

    private void checkPendingFcmSaveOperation() {
        if (avatar != null && !avatar.isEmpty() && pendingFcmTokenSaveOperation) {
            persistExpertData(firebaseUser);
            pendingFcmTokenSaveOperation = false;
        }
    }

    private void loadExpertData(ExpertData expertData) {
        Log.i(TAG, "Expert data loaded." + expertData.toString());
        avatar = expertData.avatar != null ? expertData.avatar : EMPTY_STRING;
        if (avatar != null && !avatar.isEmpty()) {
            Utils.saveSharedSetting(context, Utils.PREF_EXPERT_AVATAR, avatar);
        }
        checkPendingFcmSaveOperation();
        this.expertData = expertData;
    }

    public void goOnline() {
        long oldEtaSeconds = expertEtaSeconds;
        expertEtaSeconds = ETA_ONLINE;
        if (oldEtaSeconds != expertEtaSeconds) {
            persistExpertData(firebaseUser);
        }
    }

    public boolean isExpertOffline() {
        return expertEtaSeconds == ETA_OFFLINE;
    }

    public void notOnline() {
        if (expertEtaSeconds == ETA_ONLINE) {
            expertEtaSeconds = ETA_PRESENT;
            persistExpertData(firebaseUser);
        }
    }

    public String getAvatar() {
        return avatar;
    }

    public String getExpertFcmToken() {
        return expertFcmToken;
    }

    public FirebaseUser getFirebaseUser() {
        return firebaseUser;
    }

    public String getFlavor() {
        return flavor;
    }

    public String getBuildType() {
        return buildType;
    }

    public String getSelectedUserId() {
        return selectedUserId;
    }

    public void saveToSettings() {
        if (!flavor.isEmpty()) {
            Utils.saveSharedSetting(context, Utils.SELECTED_FLAVOR, flavor);
        }

        if (!selectedUserId.isEmpty()) {
            Utils.saveSharedSetting(context, Utils.SELECTED_USER_UUID, selectedUserId);
        }

        if (!buildType.isEmpty()) {
            Utils.saveSharedSetting(context, Utils.SELECTED_BUILD_TYPE, buildType);
        }

        if (avatar != null && !avatar.isEmpty()) {
            Utils.saveSharedSetting(context, Utils.PREF_EXPERT_AVATAR, avatar);
        }
    }

    public void cleanup() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPref.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        boolean persistData = false;
        if (key.equals(Utils.PREF_EXPERT_AVATAR)) {
            avatar = sharedPreferences.getString(Utils.PREF_EXPERT_AVATAR, avatar);
            persistData = true;
            Toast.makeText(context, "Persisting user data for " + avatar, Toast.LENGTH_SHORT).show();
            // avatar change
        } else if (key.equals(Utils.SELECTED_BUILD_TYPE)) {
            // build type change
            buildType = sharedPreferences.getString(Utils.SELECTED_BUILD_TYPE, buildType);
        } else if (key.equals(Utils.SELECTED_FLAVOR)) {
            // flavor change.
            flavor = sharedPreferences.getString(Utils.SELECTED_FLAVOR, flavor);
        } else if (key.equals(Utils.PREF_EXPERT_ONLINE)) {
            loadExpertOnlineFlag();
            persistData = true;
        }
        if (persistData) {
            persistExpertData(firebaseUser);
        }
    }

    private void loadExpertOnlineFlag() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean isOnline = sharedPreferences.getBoolean(Utils.PREF_EXPERT_ONLINE, true);
        if (isOnline) {
            expertEtaSeconds = ETA_PRESENT;
        } else {
            expertEtaSeconds = ETA_OFFLINE;
        }
    }

    private static String createNotifiationTitleFor(UserData userData, String oldTitle) {
        if (userData.getAppFlavor().isEmpty() || userData.getBuildType().isEmpty()) {
            return oldTitle;
        }
        return "New Chat: " + userData.getAppFlavor() + " / " + userData.getBuildType();
    }

    private NotificationCompat.Builder buildNotification(Context context, Map<String, String> data, String title) {
        String fromUuid = data.get("source");
        String body = data.get("bodyText");

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(Utils.NOTIFICATION_USER_UUID, fromUuid);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        for (String key : data.keySet()) {
            Log.i(TAG, "Key: " + key + ", value: " + data.get(key));
        }

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(context)
                .setAutoCancel(true)   // Automatically delete the notification
                .setSmallIcon(R.drawable.ic_person_notif) // Notification icon
                .setContentIntent(pendingIntent)
                .setContentTitle(title)
                .setContentText(body)
                .setSound(defaultSoundUri);

        // Vibration
        notificationBuilder.setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 });

        // LED
        notificationBuilder.setLights(Color.RED, 3000, 3000);

        return notificationBuilder;
    }

    private static String getChatRoomForFlavor(String flavor) {
        return flavor + "_chat_rooms";
    }
}
