package com.inceptai.dobby.database;

import android.support.annotation.IntDef;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by vivek on 4/29/17.
 */

@IgnoreExtraProperties
public class FeedbackRecord {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({AppGrade.HELPFUL, AppGrade.MAYBE,
            AppGrade.NOT_HELPFUL, AppGrade.UNKNOWN})
    @interface AppGrade {
        int HELPFUL = 0;
        int MAYBE = 1;
        int NOT_HELPFUL = 2;
        int UNKNOWN = 3;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PromotionScore.YES, PromotionScore.MAYBE,
            PromotionScore.NO, PromotionScore.UNKNOWN})
    @interface PromotionScore {
        int YES = 10;
        int MAYBE = 4;
        int NO = 0;
        int UNKNOWN = -1;
    }


    public String uid;
    public long timestamp;
    @AppGrade
    public int appGrade;
    @PromotionScore
    public int promotionScore;
    public String userFeedback;

    public FeedbackRecord() {}

    public FeedbackRecord(String uid) {
        this.uid = uid;
        timestamp = System.currentTimeMillis();
        appGrade = AppGrade.UNKNOWN;
        promotionScore = PromotionScore.UNKNOWN;
        userFeedback = "DUMMY FEEDBACK";
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("uid", uid);
        result.put("timestamp", timestamp);
        result.put("appGrade", appGrade);
        result.put("promotionScore", promotionScore);
        result.put("userFeedback", userFeedback);
        return result;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public int getAppGrade() {
        return appGrade;
    }

    public void setAppGrade(int appGrade) {
        this.appGrade = appGrade;
    }

    public int getPromotionScore() {
        return promotionScore;
    }

    public void setPromotionScore(int promotionScore) {
        this.promotionScore = promotionScore;
    }

    public String getUserFeedback() {
        return userFeedback;
    }

    public void setUserFeedback(String userFeedback) {
        this.userFeedback = userFeedback;
    }

}
