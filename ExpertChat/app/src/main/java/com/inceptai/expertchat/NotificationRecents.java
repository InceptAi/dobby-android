package com.inceptai.expertchat;

import android.util.Log;

import com.raizlabs.android.dbflow.annotation.Database;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.util.List;
import java.util.Map;

import static com.inceptai.expertchat.Utils.EMPTY_STRING;
import static com.inceptai.expertchat.Utils.TAG;

/**
 * Created by arunesh on 6/21/17.
 */
@Database(name = NotificationRecents.DB_NAME, version = NotificationRecents.DB_VERSION)
public class NotificationRecents {
    public static final String DB_NAME = "MyDataBase";

    public static final int DB_VERSION = 1;

    public NotificationRecents() {

    }

    /**
     *
     * @param notificationData
     * @return UserData object, or null on bad data.
     */
    public UserData saveIncomingNotification(Map<String, String> notificationData) {
        String fromUuid = notificationData.get("source");
        String appFlavor = notificationData.get("flavor");

        if (fromUuid == null || fromUuid.isEmpty()) {
            Log.i(TAG, "Empty UUID, unable to save incoming notification.");
            return null;
        }
        UserData userData = UserDataBackend.fetchUser(fromUuid);
        if (userData == null) {
            userData = new UserData();
            userData.userUuid = fromUuid;
        }

        if (appFlavor != null && !appFlavor.isEmpty()) {
            userData.appFlavor = appFlavor;
        }

        userData.setFreshnessTimestampMs(System.currentTimeMillis());
        switch (userData.interactionType) {
            case UserData.INTERACTION_UNKNOWN:
                userData.interactionType = UserData.USER_NOTIF_RESPONSE_PENDING;
                break;
            case UserData.ONGOING_CHAT_EXPERT_RESPONSE_SENT:
                userData.interactionType = UserData.ONGOING_CHAT_USER_NOTIF_PENDING;
                break;
            case UserData.ONGOING_CHAT_USER_NOTIF_PENDING:
                break;
            case UserData.USER_NOTIF_RESPONSE_PENDING:
                break;
        }
        userData.save();
        return userData;
    }

    public void notificationSent(ChatNotification chatNotification) {
        String userUuid = chatNotification.to;
        if (userUuid == null || userUuid.isEmpty()) {
            Log.e(TAG, "Unable to record notification for null or empty user ID.");
            return;
        }

        UserData userData = UserDataBackend.fetchUser(userUuid);
        if (userData == null) {
            userData = new UserData();
            userData.userUuid = userUuid;
        }

        userData.appFlavor = Utils.getFlavorFromFcmIdPath(chatNotification.fcmIdPath);
        userData.buildType = Utils.getBuildTypeFromFcmIdPath(chatNotification.fcmIdPath);

        userData.setFreshnessTimestampMs(System.currentTimeMillis());
        switch (userData.interactionType) {
            case UserData.INTERACTION_UNKNOWN:
                userData.interactionType = UserData.ONGOING_CHAT_EXPERT_RESPONSE_SENT;
                break;
            case UserData.ONGOING_CHAT_EXPERT_RESPONSE_SENT:
                break;
            case UserData.ONGOING_CHAT_USER_NOTIF_PENDING:
                userData.interactionType = UserData.ONGOING_CHAT_EXPERT_RESPONSE_SENT;
                break;
            case UserData.USER_NOTIF_RESPONSE_PENDING:
                userData.interactionType = UserData.ONGOING_CHAT_EXPERT_RESPONSE_SENT;
                break;
        }
        userData.save();
    }

}
