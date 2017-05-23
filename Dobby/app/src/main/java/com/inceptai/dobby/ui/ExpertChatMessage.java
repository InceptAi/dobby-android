package com.inceptai.dobby.ui;

/**
 * Created by arunesh on 5/23/17.
 */

public class ExpertChatMessage {

    String id;
    String text;
    String name;

    public ExpertChatMessage(String text, String name) {
        this.text = text;
        this.name = name;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
