package com.inceptai.wifiexpertsystem.database.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by vivek on 4/29/17.
 */

@IgnoreExtraProperties
public class RepairRecord {
    public String uid;
    public String phoneInfo;
    public String appVersion;
    public String repairStatusMessage;
    public String repairStatusString;
    public String failureReason;
    public long timestamp;

    public RepairRecord() {}

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("uid", uid);
        result.put("phoneInfo", phoneInfo);
        result.put("repairStatus", repairStatusString);
        result.put("repairStatusString", repairStatusMessage);
        result.put("appVersion", appVersion);
        result.put("timestamp", timestamp);
        result.put("failureReason", failureReason);
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

    public String getAppVersion() {
        return appVersion;
    }

    public String getRepairStatusMessage() {
        return repairStatusMessage;
    }

    public String getRepairStatusString() {
        return repairStatusString;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
