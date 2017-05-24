package com.inceptai.dobby.expert;

/**
 * Created by arunesh on 5/23/17.
 */

public class ExpertChat {
    public static final int MSG_TYPE_EXPERT_TEXT = 1001;
    public static final int MSG_TYPE_USER_TEXT = 1002;


    String id;
    String text;
    int messageType;

    public ExpertChat(String text, int messageType) {
        this.text = text;
        this.messageType = messageType;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
