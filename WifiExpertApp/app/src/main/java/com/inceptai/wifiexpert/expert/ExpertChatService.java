package com.inceptai.wifiexpert.expert;

/**
 * Created by arunesh on 6/1/17.
 */

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v7.app.NotificationCompat;

import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.inceptai.wifiexpert.BuildConfig;
import com.inceptai.wifiexpert.R;
import com.inceptai.wifiexpert.analytics.DobbyAnalytics;
import com.inceptai.wifiexpert.eventbus.DobbyEvent;
import com.inceptai.wifiexpert.eventbus.DobbyEventBus;
import com.inceptai.wifiexpert.utils.DobbyLog;
import com.inceptai.wifiexpert.utils.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static android.support.v4.app.NotificationCompat.VISIBILITY_PUBLIC;

/**
 * A "service" that connects to the expert chat system.
 * This is NOT an Android service.
 */
public class ExpertChatService implements
        ChildEventListener,
        ValueEventListener {

    private static final String USER_ROOT = BuildConfig.FLAVOR + "/" + BuildConfig.BUILD_TYPE  + "/" + "users/";
    private static final String CHAT_ROOM_CHILD = BuildConfig.FLAVOR + "_chat_rooms/" + BuildConfig.BUILD_TYPE;
    private static final String CHAT_ROOM_RECENTS =  BuildConfig.FLAVOR + "_chat_recents/" + BuildConfig.BUILD_TYPE;
    private static final String FCM_KEY = "fcmToken";
    private static final String ASSIGNED_EXPERT_KEY = "assignedExpert";
    private static final String NOTIFICATIONS_BASE = "/notifications/messages/";
    private static final String CHAT_NOTIFICATION_TITLE = "You have a new chat message.";
    private static final String EXPERT_BASE = "/expert";
    private static final long TWO_DAYS_IN_MS = 2 * 24 * 60 * 60 * 1000L;
    private static final String PREF_EXPERT_CHAT_TIMESTAMP_MS = "ExpertChatTs";
    private static final String PREF_CHAT_NUMBER = "ChatNumber";
    private static final String PREF_LAST_ANALYZED_CHAT_NUMBER = "AnalyzedChatNumber";
    private static final String EXPERT_LAST_MESSAGE_TS = "ExpertLastMessageTs";
    private static final String USER_FIRST_CHAT_ENTRY_TS = "UserFirstEntryTs";
    private static final String PREF_FIRST_CHAT = "first_expert_chat";


    public static final String INTENT_NOTIF_SOURCE = "NotifSource";
    public static final long ETA_OFFLINE = 24L * 60L * 60L;  // 24 hours.
    public static final long ETA_PRESENT = 20L * 60L; // 20 minutes or less.
    public static final long ETA_12HOURS = 12L * 60L * 60L;

    private String userUuid;
    private Context context;
    private String chatRoomPath;
    private String userTokenPath;
    private String assignedExpertUsernamePath;
    private String assignedExpertUsername;
    private String recentsUpdatePath;
    private ChatCallback chatCallback;
    private List<ExpertData> expertList;
    private long currentEtaSeconds;
    private boolean listenerConnected = false; // whether firebase listeners are registered.
    private boolean notificationsEnabled = true;
    private long chatNumber = 0;
    private long lastChatNumberDisplayedToUser = 0;


    //Analytics fields
    private long userFirstEnteredIntoChatAtMs;
    private long expertLastMessagedUserAtMs;
    private long userLastActionAtMs;



    // TODO Use this field.
    private boolean isChatEmpty;

    DobbyEventBus eventBus;
    DobbyAnalytics dobbyAnalytics;

    public interface ChatCallback {
        void onMessageAvailable(ExpertChat expertChat);
        void onNoHistoryAvailable();
        void onEtaUpdated(long newEtaSeconds, boolean isPresent);
        void onEtaAvailable(long newEtaSeconds, boolean isPresent);
    }

    public ExpertChatService(String userId,
                             Context context,
                             DobbyAnalytics dobbyAnalytics) {
        this.userUuid = userId;
        this.context = context.getApplicationContext();
        this.chatRoomPath =  CHAT_ROOM_CHILD + "/" + userUuid;
        this.userTokenPath = USER_ROOT + "/" + userUuid + "/" + FCM_KEY;
        this.recentsUpdatePath = CHAT_ROOM_RECENTS + "/" + userUuid;
        this.assignedExpertUsernamePath = USER_ROOT + "/" + userUuid + "/" + ASSIGNED_EXPERT_KEY;
        expertList = new ArrayList<>();
        DobbyLog.i("Using chat room ID: " + chatRoomPath);
        isChatEmpty = true;
        currentEtaSeconds = ETA_PRESENT;
        this.dobbyAnalytics = dobbyAnalytics;
        chatNumber = getLastChatNumber();
        lastChatNumberDisplayedToUser = getLastDisplayedChatNumber();
        expertLastMessagedUserAtMs = getLastExpertMessageTs();
        userFirstEnteredIntoChatAtMs = getUserFirstEnteredAtTs();
    }

    public void disableNotifications() {
        notificationsEnabled = false;
    }

    public void enableNotifications() {
        notificationsEnabled = true;
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

    public void checkIn() {
        long lastTs = Utils.readSharedSetting(context, PREF_EXPERT_CHAT_TIMESTAMP_MS, -1);
        long currentTs = System.currentTimeMillis();

        if (lastTs < 0 || (currentTs - lastTs) > TWO_DAYS_IN_MS) {
            // do a checkin to update as a "recent chat"
            Date date = new Date();
            getRecentsReference().setValue(date.getTime());
            Utils.saveSharedSetting(context, PREF_EXPERT_CHAT_TIMESTAMP_MS, currentTs);
        }
        dobbyAnalytics.userEnteredChat();
    }

    public long getCurrentEtaSeconds() {
        return currentEtaSeconds;
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

    public boolean isDisplayableMessage(ExpertChat expertChat) {
        return ExpertChatUtil.isDisplayableMessageType(expertChat);
    }

    public void showNotification(Context context, Map<String, String> data) {
        String title = data.get("titleText");
        String body = data.get("bodyText");
        String pushId = data.get("messagePushId");
        String source = data.get("source");
        DobbyLog.i("Title: " + title);
        DobbyLog.i(" Body: " + body);
        DobbyLog.i(" Data: " + data);
        if (!notificationsEnabled) {
            DobbyLog.v("Not showing notification.");
            return;
        }

        PendingIntent resultPendingIntent = Utils.getPendingIntentForNotification(context, source);
        if (resultPendingIntent == null) {
            FirebaseCrash.report(new RuntimeException("Null pending Intent for build flavor" + BuildConfig.FLAVOR));
            return;
        }

        int iconResource = R.mipmap.wifi_expert_launcher;

        Uri defaultSoundUri= RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(context)
                .setAutoCancel(true)   //Automatically delete the notification
                .setSmallIcon(R.drawable.ic_person_64dp) //Notification icon
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), iconResource))
                .setContentIntent(resultPendingIntent)
                .setContentTitle(title)
                .setContentText(body)
                .setVisibility(VISIBILITY_PUBLIC)
                .setSound(defaultSoundUri);


        // Vibration
        notificationBuilder.setVibrate(new long[] { 1000, 1000, 1000, 1000, 1000 });

        // LED
        notificationBuilder.setLights(Color.RED, 3000, 3000);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(0, notificationBuilder.build());

        if (pushId != null && !pushId.isEmpty()) {
            getNotificationReference().child(pushId).removeValue();
        }
        dobbyAnalytics.expertChatNotificationShown();
    }

    public void registerToEventBusListener() {
        eventBus.registerListener(this);
    }

    public void disconnect() {
        DobbyLog.v("ECS: In disconnect");
        getChatReference().removeEventListener((ChildEventListener) this);
        getChatReference().removeEventListener((ValueEventListener) this);
        listenerConnected = false;
        eventBus.unregisterListener(this);
        saveLastChatNumber();
        saveLastDisplayedChatNumber();
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

    public void pushMetaChatMessage(int metaMessageType) {
        ExpertChat expertChat = new ExpertChat();
        expertChat.setMessageType(metaMessageType);
        getChatReference().push().setValue(expertChat);
    }

    public void sendExpertNotification(String toExpert, ExpertChat expertChat) {
        ChatNotification chatNotification = new ChatNotification();
        chatNotification.from = userUuid;
        chatNotification.to = toExpert;
        chatNotification.body = expertChat.getText();
        chatNotification.title = CHAT_NOTIFICATION_TITLE;
        chatNotification.appFlavor = BuildConfig.FLAVOR;
        chatNotification.fcmIdPath = getFcmIdPathForExpert(toExpert);
        getNotificationReference().push().setValue(chatNotification);
    }

    public void sendExpertNotificationToAll(ExpertChat expertChat) {
        for (ExpertData expertData : expertList) {
            sendExpertNotification(expertData.getAvatar(), expertChat);
        }
    }

    public void sendUserLeftMetaMessage() {
        pushMetaChatMessage(ExpertChat.MSG_TYPE_META_USER_LEFT);
    }

    public void sendUserEnteredMetaMessage() {
        pushMetaChatMessage(ExpertChat.MSG_TYPE_META_USER_ENTERED);
    }

    public void pushBotChatMessage(String text) {
        ExpertChat expertChat = new
                ExpertChat(text, ExpertChat.MSG_TYPE_BOT_TEXT, true, getAndUpdateChatNumber());
        DobbyLog.v("ECS: Created bot chat with # " + chatNumber + " text " + text);
        getChatReference().push().setValue(expertChat);
    }

    public void pushExpertChatMessage(String text) {
        ExpertChat expertChat = new
                ExpertChat(text, ExpertChat.MSG_TYPE_EXPERT_TEXT, false, getAndUpdateChatNumber());
        DobbyLog.v("ECS: Created expert chat with # " + chatNumber + " text " + text);
        getChatReference().push().setValue(expertChat);
    }

    public void triggerContactWithHumanExpert(String message) {
        ExpertChat expertChat = new ExpertChat(message, ExpertChat.MSG_TYPE_META_SEND_MESSAGE_TO_EXPERT_FOR_HELP, true, getAndUpdateChatNumber());
        DobbyLog.v("ECS: Created misc chat with # " + chatNumber + " text " + message);
        pushMessageToExpert(expertChat);
        dobbyAnalytics.userAskedForExpert();
    }

    public void pushUserChatMessage(String text, boolean isActionText) {
        ExpertChat expertChat;
        if (isActionText) {
            expertChat = new ExpertChat(text, ExpertChat.MSG_TYPE_USER_TEXT, true, getAndUpdateChatNumber());
            DobbyLog.v("ECS: Created action chat with # " + chatNumber + " text " + text);
        } else {
            expertChat = new ExpertChat(text, ExpertChat.MSG_TYPE_USER_TEXT, false, getAndUpdateChatNumber());
            DobbyLog.v("ECS: Created user chat with # " + chatNumber + " text " + text);
        }
        pushMessageToExpert(expertChat);
    }


    public void sendActionCompletedMessage() {
        pushMetaChatMessage(ExpertChat.MSG_TYPE_META_ACTION_COMPLETED);
    }

    public void sendActionStartedMessage() {
        pushMetaChatMessage(ExpertChat.MSG_TYPE_META_ACTION_STARTED);
    }

    public boolean isFirstChatAfterInstall() {
        return (chatNumber == 0);
    }

    //Private stuff
    private void saveExpertChatStarted() {
        Utils.saveSharedSetting(context,
                PREF_FIRST_CHAT, Utils.FALSE_STRING);
    }


    private long getLastChatNumber() {
        return Utils.readSharedSetting(context, PREF_CHAT_NUMBER, 0);
    }

    private void saveLastChatNumber() {
        DobbyLog.v("ECS: In save last chat # at " + chatNumber);
        Utils.saveSharedSetting(context, PREF_CHAT_NUMBER, chatNumber);
    }

    private long getLastDisplayedChatNumber() {
        return Utils.readSharedSetting(context, PREF_LAST_ANALYZED_CHAT_NUMBER, 0);
    }

    private void saveLastDisplayedChatNumber() {
        DobbyLog.v("ECS: In save last analyzed # at " + lastChatNumberDisplayedToUser);
        Utils.saveSharedSetting(context, PREF_LAST_ANALYZED_CHAT_NUMBER, lastChatNumberDisplayedToUser);
    }

    private long getLastExpertMessageTs() {
        return Utils.readSharedSetting(context, EXPERT_LAST_MESSAGE_TS, 0);
    }

    private void saveLastExpertMessageTs() {
        Utils.saveSharedSetting(context, EXPERT_LAST_MESSAGE_TS, expertLastMessagedUserAtMs);
    }

    private long getUserFirstEnteredAtTs() {
        return Utils.readSharedSetting(context, USER_FIRST_CHAT_ENTRY_TS, 0);
    }

    private void saveUserFirstEnteredAtTs() {
        Utils.saveSharedSetting(context, USER_FIRST_CHAT_ENTRY_TS, userFirstEnteredIntoChatAtMs);
    }

    private void pushMessageToExpert(ExpertChat expertChat) {
        getChatReference().push().setValue(expertChat);
        if (assignedExpertUsername == null || assignedExpertUsername.isEmpty()) {
            sendExpertNotificationToAll(expertChat);
        } else {
            sendExpertNotification(assignedExpertUsername, expertChat);
        }
    }

    public boolean isExpertChatMessageFresh(ExpertChat expertChat) {
        return (ExpertChatUtil.isMessageFresh(expertChat, lastChatNumberDisplayedToUser));
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

    private DatabaseReference getRecentsReference() {
        return FirebaseDatabase.getInstance().getReference().child(recentsUpdatePath);
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
        processExpertChat(dataSnapshot);
    }

    @Override
    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
        processExpertChat(dataSnapshot);
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


    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        if (dataSnapshot == null || dataSnapshot.getValue() == null) {
            if (chatCallback != null) {
                chatCallback.onNoHistoryAvailable();
            }
        }
    }

    public void saveState() {
        saveLastChatNumber();
        saveLastDisplayedChatNumber();
        saveUserFirstEnteredAtTs();
        saveLastExpertMessageTs();
    }

    public void onNotificationConsumed() {
        dobbyAnalytics.expertChatNotificationConsumed();
    }

    private static ExpertChat parse(DataSnapshot dataSnapshot) {
        ExpertChat expertChat = dataSnapshot.getValue(ExpertChat.class);
        expertChat.id = dataSnapshot.getKey();
        return expertChat;
    }

    private void processExpertChat(DataSnapshot dataSnapshot) {
        if (chatCallback != null) {
            isChatEmpty = false;
            if (dataSnapshot.getKey().equals(ASSIGNED_EXPERT_KEY)) {
                assignedExpertUsername = (String) dataSnapshot.getValue();
            } else {
                ExpertChat expertChat = parse(dataSnapshot);
                //Deleting special messages
                if (expertChat.getText().contains(ExpertChat.SPECIAL_MESSAGE_PREFIX)) {
                    dataSnapshot.getRef().removeValue();
                    parseExpertTextAndTakeActionIfNeeded(expertChat);
                } else if (ExpertChatUtil.isDisplayableMessageType(expertChat)) {
                    if (expertChat.getChatNumber() == 0) { //Chat number not set
                        expertChat.setChatNumber(getAndUpdateChatNumber());
                        DobbyLog.v("ECS: Created expert chat with # " + chatNumber + " text " + expertChat.getText());
                        dataSnapshot.getRef().setValue(expertChat);
                    } else {
                        if (isExpertChatMessageFresh(expertChat)) {
                            expertChat.setMessageFresh(true);
                            updateAnalytics(expertChat);
                            lastChatNumberDisplayedToUser = expertChat.getChatNumber();
                            DobbyLog.v("ECS: Updating lastChatNumberDisplayedToUser: " + lastChatNumberDisplayedToUser +
                                    " current chat# " + chatNumber + " for text " + expertChat.getText());
                        }
                        chatCallback.onMessageAvailable(expertChat);
                    }
                }
                DobbyLog.i("Got chat message: " + expertChat.getText());
            }
        }
    }


    private void parseExpertTextAndTakeActionIfNeeded(ExpertChat expertChat) {
        if (expertChat.getMessageType() == ExpertChat.MSG_TYPE_EXPERT_TEXT) {
            String expertMessage = expertChat.getText();
            if (expertMessage.startsWith(ExpertChat.SPECIAL_MESSAGE_PREFIX)) {
                eventBus.postEvent(DobbyEvent.EventType.EXPERT_ASKED_FOR_ACTION, expertMessage);
            }
        }
    }


    private void logExpertResponseTime() {
        long currentTimeInMillis = System.currentTimeMillis();
        if (currentTimeInMillis - userFirstEnteredIntoChatAtMs < 60 * 1000) {
            dobbyAnalytics.expertEnteredChatWithin1MinOfFirstUserMessage();
        } else if (currentTimeInMillis - userFirstEnteredIntoChatAtMs < 5 * 60 * 1000) {
            dobbyAnalytics.expertEnteredChatWithin5MinOfFirstUserMessage();
        } else if (currentTimeInMillis - userFirstEnteredIntoChatAtMs < 30 * 60 * 1000) {
            dobbyAnalytics.expertEnteredChatWithin30MinOfFirstUserMessage();
        } else if (currentTimeInMillis - userFirstEnteredIntoChatAtMs < 12 * 60 * 60 * 1000) {
            dobbyAnalytics.expertEnteredChatWithin12HourOfFirstUserMessage();
        }
    }



    private void updateAnalytics(ExpertChat expertChat) {
        boolean chatInHumanMode = ExpertChatUtil.isChatInHumanMode(expertLastMessagedUserAtMs);
        if (expertChat.getMessageType() == ExpertChat.MSG_TYPE_EXPERT_TEXT) {
            if (expertLastMessagedUserAtMs == 0) { //first time
                saveLastExpertMessageTs();
                logExpertResponseTime();
            }
            expertLastMessagedUserAtMs = System.currentTimeMillis();
            dobbyAnalytics.expertSentMessageToUser();
        } else if (expertChat.getMessageType() == ExpertChat.MSG_TYPE_USER_TEXT) {

            if (userFirstEnteredIntoChatAtMs == 0) { //First time
                userFirstEnteredIntoChatAtMs = System.currentTimeMillis();
                saveUserFirstEnteredAtTs();
            }

            dobbyAnalytics.userSentMessage();
            if (chatInHumanMode) {
                dobbyAnalytics.userSentMessageToExpert();
            } else {
                dobbyAnalytics.userSentMessageToBot();
            }

            if (expertChat.isAutoGenerated()) {
                userLastActionAtMs = System.currentTimeMillis();
                dobbyAnalytics.userInteractedViaAction();
            } else {
                dobbyAnalytics.userInteractedViaText();
                if (!chatInHumanMode) {
                    dobbyAnalytics.userInteractedWithBot();
                } else {
                    dobbyAnalytics.userInteractedWithExpert();
                }
            }
        } else if (expertChat.getMessageType() == ExpertChat.MSG_TYPE_BOT_TEXT) {
            dobbyAnalytics.botSentMessageToUser();
        }
    }

    private long getAndUpdateChatNumber() {
        if (chatNumber == 0) {
            dobbyAnalytics.userEnteredChatFirstTimeEver();
        }
        return ++chatNumber;
    }

}
