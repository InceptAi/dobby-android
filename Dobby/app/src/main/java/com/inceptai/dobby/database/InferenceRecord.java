package com.inceptai.dobby.database;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by vivek on 4/29/17.
 */

@IgnoreExtraProperties
public class InferenceRecord {
    public String uid;
    public String phoneInfo;
    public String titleMessage;
    public long timestamp;
    public String bandwidthGradeJson;
    public String wifiGradeJson;
    public String pingGradeJson;
    public String httpGradeJson;
    public List<String> detailedMessageList;
    public String conditionsUsedForInference;

    public InferenceRecord() {}

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("uid", uid);
        result.put("phoneInfo", phoneInfo);
        result.put("titleMessage", titleMessage);
        result.put("detailedMessageList", detailedMessageList);
        result.put("timestamp", timestamp);
        result.put("bandwidthGradeJson", bandwidthGradeJson);
        result.put("wifiGradeJson", wifiGradeJson);
        result.put("pingGradeJson", pingGradeJson);
        result.put("httpGradeJson", httpGradeJson);
        result.put("conditionsUsedForInference", conditionsUsedForInference);

        return result;
    }

    public String getUid() {
        return uid;
    }

    public String getPhoneInfo() {
        return phoneInfo;
    }

    public String getTitleMessage() {
        return titleMessage;
    }

    public List<String> getDetailedMessageList() {
        return detailedMessageList;
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

    public String getConditionsUsedForInference() {
        return conditionsUsedForInference;
    }

}
