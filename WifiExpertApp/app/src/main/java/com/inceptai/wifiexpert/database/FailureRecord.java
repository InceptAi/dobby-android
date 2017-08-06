package com.inceptai.wifiexpert.database;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.inceptai.dobby.speedtest.BandwidthTestCodes;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by vivek on 4/29/17.
 */

@IgnoreExtraProperties
public class FailureRecord {
    public String uid;
    public String phoneInfo;
    public String appVersion;
    public String errorMessage;
    public long timestamp;
    @BandwidthTestCodes.TestMode
    public int testMode;
    @BandwidthTestCodes.ErrorCodes
    public int errorCode;

    public FailureRecord() {}

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("uid", uid);
        result.put("phoneInfo", phoneInfo);
        result.put("errorMessage", errorMessage);
        result.put("appVersion", appVersion);
        result.put("timestamp", timestamp);
        result.put("testMode", testMode);
        result.put("errorCode", errorCode);
        return result;
    }

    public String getUid() {
        return uid;
    }

    public String getPhoneInfo() {
        return phoneInfo;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public int getTestMode() {
        return testMode;
    }

    public int getErrorCode() {
        return errorCode;
    }
}
