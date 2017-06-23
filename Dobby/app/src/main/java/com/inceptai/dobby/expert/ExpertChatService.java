package com.inceptai.dobby.expert;

/**
 * Created by arunesh on 6/1/17.
 */

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.NotificationCompat;

import com.google.common.eventbus.Subscribe;
import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.inceptai.dobby.BuildConfig;
import com.inceptai.dobby.DobbyAnalytics;
import com.inceptai.dobby.MainActivity;
import com.inceptai.dobby.R;
import com.inceptai.dobby.ai.DobbyAi;
import com.inceptai.dobby.dagger.ProdComponent;
import com.inceptai.dobby.eventbus.DobbyEvent;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.ui.ExpertChatActivity;
import com.inceptai.dobby.utils.DobbyLog;
import com.inceptai.dobby.utils.Utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import static android.support.v4.app.NotificationCompat.VISIBILITY_PUBLIC;

/**
 * A "service" that connects to the expert chat system.
 * This is NOT an Android service.
 */
public class ExpertChatService implements
        ChildEventListener,
        ValueEventListener {

    private static final String USER_ROOT = BuildConfig.FLAVOR + "/" + BuildConfig.BUILD_TYPE  + "/" + "users/";
    private static final String WIFI_EXPERT_BUILD_FLAVOR = "dobby";
    private static final String WIFI_TESTER_BUILD_FLAVOR = "wifidoc";
    private static final String CHAT_ROOM_CHILD = BuildConfig.FLAVOR + "_chat_rooms/" + BuildConfig.BUILD_TYPE;
    private static final String CHAT_ROOM_RECENTS =  BuildConfig.FLAVOR + "_chat_recents/" + BuildConfig.BUILD_TYPE;
    private static final String FCM_KEY = "fcmToken";
    private static final String ASSIGNED_EXPERT_KEY = "assignedExpert";
    private static final String NOTIFICATIONS_BASE = "/notifications/messages/";
    private static final String CHAT_NOTIFICATION_TITLE = "You have a new chat message.";
    private static final String EXPERT_BASE = "/expert";
    private static final long TWO_DAYS_IN_MS = 2 * 24 * 60 * 60 * 1000L;

    public static final String INTENT_NOTIF_SOURCE = "NotifSource";
    public static final long ETA_OFFLINE = 24L * 60L * 60L;  // 24 hours.
    public static final long ETA_ONLINE = 2L * 60L; // 2 minutes or less.
    public static final long ETA_PRESENT = 20L * 60L; // 20 minutes or less.
    public static final long ETA_12HOURS = 12L * 60L * 60L;

    private static final int WIFI_SCAN_ACTION = 2001;
    private static final int PING_ACTION = 2002;
    private static final int HTTP_ACTION = 2003;
    private static final int ALL_TESTS = 2100;

    private static final int ASK_FOR_FEEDBACK_ACTION = 3001;
    private static final int SWITCH_TO_BOT_MODE = 3002;
    private static final int RESOLVED_USER_QUERY = 3003;
    private static final int UNRESOLVED_USER_QUERY = 3004;
    private static final int NEED_MORE_DATA = 3005;
    private static final int USER_LEFT_EARLY = 3006;
    private static final int GOOD_INFERENCING = 3007;
    private static final int BAD_INFERENCING = 3008;
    private static final int BETTER_INFERENCING_NEEDED = 3009;


    private static ExpertChatService INSTANCE;

    private String userUuid;
    private String chatRoomPath;
    private String userTokenPath;
    private String assignedExpertUsernamePath;
    private String assignedExpertUsername;
    private String recentsUpdatePath;
    private ChatCallback chatCallback;
    private List<ExpertData> expertList;
    private long currentEtaSeconds;
    private boolean listenerConnected = false; // whether firebase listerners are registered.
    private boolean notificationsEnabled = true;

    // TODO Use this field.
    private boolean isChatEmpty;

    @Inject
    DobbyAi dobbyAi;

    @Inject
    DobbyEventBus eventBus;

    @Inject
    DobbyAnalytics dobbyAnalytics;

    public interface ChatCallback {
        void onMessageAvailable(ExpertChat expertChat);
        void onNoHistoryAvailable();
        void onEtaUpdated(long newEtaSeconds, boolean isPresent);
        void onEtaAvailable(long newEtaSeconds, boolean isPresent);
    }

    private ExpertChatService(String userUuid, ProdComponent prodComponent) {
        prodComponent.inject(this);
        this.userUuid = userUuid;
        this.chatRoomPath =  CHAT_ROOM_CHILD + "/" + userUuid;
        this.userTokenPath = USER_ROOT + "/" + userUuid + "/" + FCM_KEY;
        this.recentsUpdatePath = CHAT_ROOM_RECENTS + "/" + userUuid;
        this.assignedExpertUsernamePath = USER_ROOT + "/" + userUuid + "/" + ASSIGNED_EXPERT_KEY;
        expertList = new ArrayList<>();
        DobbyLog.i("Using chat room ID: " + chatRoomPath);
        isChatEmpty = true;
        currentEtaSeconds = ETA_PRESENT;
        //Being called in connect from the activity onStart.
        //eventBus.registerListener(this);
    }

    public static ExpertChatService fetchInstance(String userUuid, ProdComponent prodComponent) {
        if (INSTANCE == null) {
            INSTANCE = new ExpertChatService(userUuid, prodComponent);
        }
        return INSTANCE;
    }

    public static ExpertChatService get() {
        return INSTANCE;
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

    public void checkIn(Context context) {
        long lastTs = Utils.readSharedSetting(context, Utils.PREF_EXPERT_CHAT_TIMESTAMP_MS, -1);
        long currentTs = System.currentTimeMillis();

        if (lastTs < 0 || (currentTs - lastTs) > TWO_DAYS_IN_MS) {
            // do a checkin to update as a "recent chat"
            Date date = new Date();
            getRecentsReference().setValue(date.getTime());
            Utils.saveSharedSetting(context, Utils.PREF_EXPERT_CHAT_TIMESTAMP_MS, currentTs);
        }
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

        PendingIntent resultPendingIntent = getPendingIntentForNotification(context, source);
        if (resultPendingIntent == null) {
            FirebaseCrash.report(new RuntimeException("Null pending Intent for build flavor" + BuildConfig.FLAVOR));
            return;
        }

        int iconResource = WIFI_EXPERT_BUILD_FLAVOR.equals(BuildConfig.FLAVOR) ? R.mipmap.wifi_expert_launcher : R.mipmap.wifi_doc_launcher;

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
    }

    public void connect() {
        eventBus.registerListener(this);
    }

    public void disconnect() {
        getChatReference().removeEventListener((ChildEventListener) this);
        getChatReference().removeEventListener((ValueEventListener) this);
        listenerConnected = false;
        eventBus.unregisterListener(this);
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

    public void pushUserChatMessage(ExpertChat expertChat, boolean shouldContactExpert) {
        getChatReference().push().setValue(expertChat);
        if (shouldContactExpert) {
            if (assignedExpertUsername == null || assignedExpertUsername.isEmpty()) {
                sendExpertNotificationToAll(expertChat);
            } else {
                sendExpertNotification(assignedExpertUsername, expertChat);
            }
        }
    }

    public void pushMetaChatMessage(int metaMessageType) {
        ExpertChat expertChat = new ExpertChat();
        expertChat.setMessageType(metaMessageType);
        getChatReference().push().setValue(expertChat);
    }


    public void pushBotChatMessage(ExpertChat expertChat) {
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

    private PendingIntent getPendingIntentForNotification(Context context, String source) {
        boolean isWifiTester = false;

        if (BuildConfig.FLAVOR.equals(WIFI_TESTER_BUILD_FLAVOR)) {
            isWifiTester = true;
        } else if (!(BuildConfig.FLAVOR.equals(WIFI_EXPERT_BUILD_FLAVOR))) {
            DobbyLog.e("Unknown build flavor: " + BuildConfig.FLAVOR);
            return null;
        }
        Intent intent = null;

        if (isWifiTester) {
            intent = new Intent(context, ExpertChatActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        } else {
            intent = new Intent(context, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }

        intent.putExtra(INTENT_NOTIF_SOURCE, source);

        PendingIntent pendingIntent = null;
        if (isWifiTester) {
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);

            // Adds the back stack
            stackBuilder.addParentStack(ExpertChatActivity.class);

            // Adds the Intent to the top of the stack
            stackBuilder.addNextIntent(intent);
            // Gets a PendingIntent containing the entire back stack
            pendingIntent =
                    stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        } else {
            pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        }

        return pendingIntent;
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
                    chatCallback.onMessageAvailable(expertChat);
                }
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
        expertChat.id = dataSnapshot.getKey();
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

    private void sendActionCompletedMessage() {
        pushMetaChatMessage(ExpertChat.MSG_TYPE_META_ACTION_COMPLETED);
    }

    private void sendActionStartedMessage() {
        pushMetaChatMessage(ExpertChat.MSG_TYPE_META_ACTION_STARTED);
    }

    private void triggerDiagnosticAction(final int actionToTrigger) {
        switch (actionToTrigger) {
            case WIFI_SCAN_ACTION:
                dobbyAi.performAndRecordWifiAction();
                return;
            case PING_ACTION:
                dobbyAi.performAndRecordPingAction();
                return;
            case HTTP_ACTION:
                //no-op for now
                return;
            case ALL_TESTS:
                //no-op for now
                return;
            case ASK_FOR_FEEDBACK_ACTION:
                dobbyAi.triggerFeedbackRequest();
                break;
            case SWITCH_TO_BOT_MODE:
                dobbyAi.triggerSwitchToBotMode();
                break;
            case USER_LEFT_EARLY:
                dobbyAnalytics.setExpertSaysUserDroppedOff();
                break;
            case RESOLVED_USER_QUERY:
                dobbyAnalytics.setExpertSaysIssueResolved();
                break;
            case UNRESOLVED_USER_QUERY:
                dobbyAnalytics.setExpertSaysIssueUnResolved();
                break;
            case NEED_MORE_DATA:
                dobbyAnalytics.setExpertSaysMoreDataNeeded();
                break;
            case GOOD_INFERENCING:
                dobbyAnalytics.setExpertSaysGoodInferencing();
                break;
            case BAD_INFERENCING:
                dobbyAnalytics.setExpertSaysBadInferencing();
                break;
            case BETTER_INFERENCING_NEEDED:
                dobbyAnalytics.setExpertSaysInferencingCanBeBetter();
                break;
        }
    }

    private boolean parseExpertTextAndTakeActionIfNeeded(ExpertChat expertChat) {
        if (expertChat.getMessageType() == ExpertChat.MSG_TYPE_EXPERT_TEXT) {
            String expertMessage = expertChat.getText();
            if (expertMessage.startsWith("#")) {
                if (expertMessage.toLowerCase().contains("wifi")) {
                    triggerDiagnosticAction(WIFI_SCAN_ACTION);
                } else if (expertMessage.toLowerCase().contains("ping")) {
                    triggerDiagnosticAction(PING_ACTION);
                } else if (expertMessage.toLowerCase().contains("feedback")) {
                    triggerDiagnosticAction(ASK_FOR_FEEDBACK_ACTION);
                } else if (expertMessage.toLowerCase().contains("bot")) {
                    triggerDiagnosticAction(SWITCH_TO_BOT_MODE);
                } else if (expertMessage.toLowerCase().contains("left") ||
                        expertMessage.toLowerCase().contains("early") ||
                        expertMessage.toLowerCase().contains("dropped")) {
                    triggerDiagnosticAction(USER_LEFT_EARLY);
                } else if (expertMessage.toLowerCase().contains("good")) {
                    triggerDiagnosticAction(GOOD_INFERENCING);
                } else if (expertMessage.toLowerCase().contains("bad")) {
                    triggerDiagnosticAction(BAD_INFERENCING);
                }  else if (expertMessage.toLowerCase().contains("unresolved") || expertMessage.toLowerCase().contains("unsolved")) {
                    triggerDiagnosticAction(UNRESOLVED_USER_QUERY);
                } else if (expertMessage.toLowerCase().contains("solved") || expertMessage.toLowerCase().contains("resolved")) {
                    triggerDiagnosticAction(RESOLVED_USER_QUERY);
                } else if (expertMessage.toLowerCase().contains("more") || expertMessage.toLowerCase().contains("data")) {
                    triggerDiagnosticAction(NEED_MORE_DATA);
                } else if (expertMessage.toLowerCase().contains("better")) {
                    triggerDiagnosticAction(BETTER_INFERENCING_NEEDED);
                }
                return true;
            }
        }
        return false;
    }

    //EventBus events
    @Subscribe
    public void listenToEventBus(DobbyEvent event) {
        if (event.getEventType() == DobbyEvent.EventType.EXPERT_ACTION_STARTED) {
            sendActionStartedMessage();
        } else if (event.getEventType() == DobbyEvent.EventType.EXPERT_ACTION_COMPLETED) {
            sendActionCompletedMessage();
        }
    }

}
