package com.inceptai.wifiexpertsystem.database.model;

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
    @IntDef({HelpfulScore.HELPFUL, HelpfulScore.MAYBE,
            HelpfulScore.NOT_HELPFUL, HelpfulScore.UNKNOWN})
    public @interface HelpfulScore {
        int HELPFUL = 0;
        int MAYBE = 1;
        int NOT_HELPFUL = 2;
        int UNKNOWN = 3;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PromotionScore.YES, PromotionScore.MAYBE,
            PromotionScore.NO, PromotionScore.UNKNOWN})
    public @interface PromotionScore {
        int YES = 10;
        int MAYBE = 4;
        int NO = 0;
        int UNKNOWN = -1;
    }


    public String uid;
    public long timestamp;
    @HelpfulScore
    public int helpfulScore;
    @PromotionScore
    public int promotionScore;
    public String userFeedback;
    public String emailAddress;

    public FeedbackRecord() {}

    public FeedbackRecord(String uid) {
        this.uid = uid;
        timestamp = System.currentTimeMillis();
        helpfulScore = HelpfulScore.UNKNOWN;
        promotionScore = PromotionScore.UNKNOWN;
        userFeedback = "DUMMY FEEDBACK";
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("uid", uid);
        result.put("timestamp", timestamp);
        result.put("helpfulScore", helpfulScore);
        result.put("promotionScore", promotionScore);
        result.put("userFeedback", userFeedback);
        result.put("emailAddress", emailAddress);
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

    public int getHelpfulScore() {
        return helpfulScore;
    }

    public void setHelpfulScore(int helpfulScore) {
        this.helpfulScore = helpfulScore;
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

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }
}
