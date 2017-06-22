package com.inceptai.expertchat;

import com.raizlabs.android.dbflow.annotation.Database;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.util.Map;

/**
 * Created by arunesh on 6/21/17.
 */
@Database(name = NotificationRecents.DB_NAME, version = NotificationRecents.DB_VERSION)
public class NotificationRecents {
    public static final String DB_NAME = "MyDataBase";

    public static final int DB_VERSION = 1;

    public NotificationRecents() {

    }

    public void saveIncomingNotification(Map<String, String> notificationData) {
        String fromUuid = notificationData.get("source");
        String appFlavor = notificationData.get("flavor");
    }

    public void notificationSent(ChatNotification chatNotification) {

    }

    UserData fetchUser(String userUuid) {
        return SQLite.select().from(UserData.class).where(UserData_Table.userUuid.eq(userUuid)).querySingle();
    }
}
