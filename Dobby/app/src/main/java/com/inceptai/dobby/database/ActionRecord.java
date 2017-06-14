package com.inceptai.dobby.database;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.inceptai.dobby.utils.Utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by vivek on 4/29/17.
 */

@IgnoreExtraProperties
public class ActionRecord {
    public String uid;
    public String phoneInfo;
    public String appVersion;
    public long timestamp;
    public String bandwidthGradeJson = Utils.EMPTY_STRING;
    public String wifiGradeJson = Utils.EMPTY_STRING;
    public String pingGradeJson = Utils.EMPTY_STRING;
    public String httpGradeJson = Utils.EMPTY_STRING;
    public String actionType = Utils.EMPTY_STRING;

    public ActionRecord() {}

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("uid", uid);
        result.put("phoneInfo", phoneInfo);
        result.put("appVersion", appVersion);
        result.put("timestamp", timestamp);
        result.put("bandwidthGradeJson", bandwidthGradeJson);
        result.put("wifiGradeJson", wifiGradeJson);
        result.put("pingGradeJson", pingGradeJson);
        result.put("httpGradeJson", httpGradeJson);
        result.put("actionType", actionType);
        return result;
    }

    public String getUid() {
        return uid;
    }

    public String getPhoneInfo() {
        return phoneInfo;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getBandwidthGradeJson() {
        return bandwidthGradeJson;
    }

    public String getWifiGradeJson() {
        return wifiGradeJson;
    }

    public String getPingGradeJson() {
        return pingGradeJson;
    }

    public String getHttpGradeJson() {
        return httpGradeJson;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public String getActionType() {
        return actionType;
    }

}
