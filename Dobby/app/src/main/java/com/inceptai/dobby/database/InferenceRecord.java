package com.inceptai.dobby.database;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;
import com.inceptai.dobby.ai.DataInterpreter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by vivek on 4/29/17.
 */

@IgnoreExtraProperties
public class InferenceRecord {
    public String uid;
    public String phoneId;
    public String titleMessage;
    public List<String> detailedMessageList;
    public long timestamp;
    public DataInterpreter.BandwidthGrade bandwidthGrade;
    public DataInterpreter.WifiGrade wifiGrade;
    public DataInterpreter.PingGrade pingGrade;
    public DataInterpreter.HttpGrade httpGrade;
    public HashMap<Integer, Double> conditionsUsedForInference;

    public InferenceRecord() {}

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("uid", uid);
        result.put("phoneId", phoneId);
        result.put("title", titleMessage);
        result.put("detailed", detailedMessageList);
        result.put("ts", timestamp);
        result.put("bw", bandwidthGrade);
        result.put("wifi", wifiGrade);
        result.put("ping", pingGrade);
        result.put("http", httpGrade);
        result.put("pc", conditionsUsedForInference);
        return result;
    }

}
