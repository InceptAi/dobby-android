package com.inceptai.expertchat;

/**
 * Created by arunesh on 6/9/17.
 */

public class ExpertData {

    String avatar;
    String email;
    String name;
    String fcmToken;
    long etaSeconds;

    public ExpertData() {}

    public String getAvatar() {
        return avatar;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public long getEtaSeconds() {
        return etaSeconds;
    }

    @Override
    public String toString() {
        return "ExpertData: " +
                "Avatar: " + avatar != null ? avatar : "Avatar unknown " +
                "Email: " + email != null ? email : "Email unknown " +
                "Name: " + name != null ? email : "Name unknown " +
                "FCM Token: " + fcmToken != null ? fcmToken : "FCM Token unknown ";
    }
}
