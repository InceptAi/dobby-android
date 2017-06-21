package com.inceptai.dobby.expert;

import com.inceptai.dobby.ui.ChatEntry;

/**
 * Created by arunesh on 6/8/17.
 */

public class ChatNotification {

    String from;
    String to;
    String title;
    String body;
    String fcmIdPath;
    String id;
    String appFlavor;

    ChatNotification() {}

    public void setFrom(String from) {
        this.from = from;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setFcmIdPath(String fcmIdPath) {
        this.fcmIdPath = fcmIdPath;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getFcmIdPath() {
        return fcmIdPath;
    }

    public String getId() {
        return id;
    }

    public String getAppFlavor() {
        return appFlavor;
    }

    public void setAppFlavor(String appFlavor) {
        this.appFlavor = appFlavor;
    }
}
