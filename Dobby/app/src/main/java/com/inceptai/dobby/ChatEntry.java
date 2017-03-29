package com.inceptai.dobby;

/**
 * Created by arunesh on 3/28/17.
 */

public class ChatEntry {

    // These type values are directly returned in RecyclerViewAdapter, so they should start with
    // zero.
    public static final int DOBBY_CHAT= 0;
    public static final int USER_CHAT = 1;
    private static int UNKNOWN = -1;
    private String text;
    private int entryType = UNKNOWN;

    public ChatEntry(String text, int entryType) {
        this.text = text;
        this.entryType = entryType;
    }

    public String getText() {
        return text;
    }

    public int getEntryType() {
        return entryType;
    }
}
