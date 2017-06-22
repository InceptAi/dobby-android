package com.inceptai.expertchat;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

/**
 * Created by arunesh on 6/21/17.
 */

@Table(database = NotificationRecents.class)
public class UserData extends BaseModel {

    public static final int USER_NOTIF_RESPONSE_PENDING = 5001;
    public static final int ONGOING_CHAT_EXPERT_RESPONSE_SENT = 5002;
    public static final int ONGOING_CHAT_USER_NOTIF_PENDING = 5002;

    @Column
    @PrimaryKey
    String userUuid;

    @Column
    long freshnessTimestampMs;

    @Column
    int interactionType;

    @Column
    String appFlavor;

    public String getUserUuid() {
        return userUuid;
    }

    public void setUserUuid(String userUuid) {
        this.userUuid = userUuid;
    }

    public long getFreshnessTimestampMs() {
        return freshnessTimestampMs;
    }

    public void setFreshnessTimestampMs(long freshnessTimestampMs) {
        this.freshnessTimestampMs = freshnessTimestampMs;
    }

    public int getInteractionType() {
        return interactionType;
    }

    public void setInteractionType(int interactionType) {
        this.interactionType = interactionType;
    }

    public String getAppFlavor() {
        return appFlavor;
    }

    public void setAppFlavor(String appFlavor) {
        this.appFlavor = appFlavor;
    }
}
