package com.inceptai.wifiexpert.expertSystem;

/**
 * Created by vivek on 8/4/17.
 */

public class ExpertMessage {

    private boolean sourceHuman;
    private String messageString;
    private int actionType;


    public static ExpertMessage fromAction(String hashAction) {
    // Convert into int actionType using a parser.
        // return new ExpertMessage(ActionParser.parse(hashAction));
    }

    public boolean isSourceHuman() {
        return sourceHuman;
    }
}
