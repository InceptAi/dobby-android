package com.inceptai.dobby.ai;

import android.support.annotation.IntDef;

import com.inceptai.dobby.utils.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import static com.inceptai.dobby.ai.UserResponse.ResponseType.ASK_ABOUT_DOBBY;
import static com.inceptai.dobby.ai.UserResponse.ResponseType.CANCEL;
import static com.inceptai.dobby.ai.UserResponse.ResponseType.CONTACT_HUMAN_EXPERT;
import static com.inceptai.dobby.ai.UserResponse.ResponseType.LIST_ALL_FUNCTIONS;
import static com.inceptai.dobby.ai.UserResponse.ResponseType.NO;
import static com.inceptai.dobby.ai.UserResponse.ResponseType.NO_COMMENTS;
import static com.inceptai.dobby.ai.UserResponse.ResponseType.NO_RESPONSE;
import static com.inceptai.dobby.ai.UserResponse.ResponseType.START_WIFI_REPAIR;
import static com.inceptai.dobby.ai.UserResponse.ResponseType.RUN_ALL_DIAGNOSTICS;
import static com.inceptai.dobby.ai.UserResponse.ResponseType.RUN_BW_TESTS;
import static com.inceptai.dobby.ai.UserResponse.ResponseType.RUN_WIFI_TESTS;
import static com.inceptai.dobby.ai.UserResponse.ResponseType.SHOW_LAST_SUGGESTION_DETAILS;
import static com.inceptai.dobby.ai.UserResponse.ResponseType.TURN_OFF_WIFI_MONITORING;
import static com.inceptai.dobby.ai.UserResponse.ResponseType.TURN_ON_WIFI_MONITORING;
import static com.inceptai.dobby.ai.UserResponse.ResponseType.YES;

/**
 * Represents an action (to be taken or in progress) by the InferenceEngine.
 */

public class UserResponse {
    public static final UserResponse RESPONSE_NONE = new UserResponse(Utils.EMPTY_STRING, ResponseType.NO_RESPONSE);

    @IntDef({YES,
            NO,
            CANCEL,
            RUN_ALL_DIAGNOSTICS,
            RUN_BW_TESTS,
            RUN_WIFI_TESTS,
            LIST_ALL_FUNCTIONS,
            ASK_ABOUT_DOBBY,
            NO_RESPONSE,
            SHOW_LAST_SUGGESTION_DETAILS,
            NO_COMMENTS,
            CONTACT_HUMAN_EXPERT,
            START_WIFI_REPAIR,
            TURN_ON_WIFI_MONITORING,
            TURN_OFF_WIFI_MONITORING
    })


    @Retention(RetentionPolicy.SOURCE)
    public @interface ResponseType {
        int YES = 0;
        int NO = 1;
        int CANCEL = 2;
        int RUN_ALL_DIAGNOSTICS = 3;
        int RUN_BW_TESTS = 4;
        int RUN_WIFI_TESTS = 5;
        int LIST_ALL_FUNCTIONS = 6;
        int ASK_ABOUT_DOBBY = 7;
        int NO_RESPONSE = 8;
        int SHOW_LAST_SUGGESTION_DETAILS = 9;
        int NO_COMMENTS = 10;
        int CONTACT_HUMAN_EXPERT = 11;
        int START_WIFI_REPAIR = 12;
        int TURN_ON_WIFI_MONITORING = 13;
        int TURN_OFF_WIFI_MONITORING = 14;
    }

    /* User response to be shown, null for no response. */
    private String responseString;

    @ResponseType
    private int responseType;

    public String getResponseString() {
        return responseString;
    }

    @ResponseType
    public int getResponse() {
        return responseType;
    }

    public UserResponse(String userResponse, @ResponseType  int responseType) {
        this.responseString = userResponse;
        this.responseType = responseType;
    }

    public static List<String > convertResponseTypesToString(List<Integer> inputResponseList) {
        List<String> stringList = new ArrayList<>();
        for (Integer responseType: inputResponseList) {
            stringList.add(getStringForResponseType(responseType));
        }
        return stringList;
    }

    public static String getStringForResponseType(@ResponseType int inputResponseType) {
        switch(inputResponseType) {
            case YES:
                return "Yes";
            case NO:
                return "No";
            case CANCEL:
                return "Cancel";
            case RUN_ALL_DIAGNOSTICS:
                return "Slow internet";
            case RUN_BW_TESTS:
                return "Run speed test";
            case RUN_WIFI_TESTS:
                return "Check wifi";
            case LIST_ALL_FUNCTIONS:
                return "List functions";
            case ASK_ABOUT_DOBBY:
                return "About Wifi Expert";
            case SHOW_LAST_SUGGESTION_DETAILS:
                return "Details";
            case NO_COMMENTS:
                return "Cancel";
            case CONTACT_HUMAN_EXPERT:
                return "Contact Human Expert";
            case TURN_ON_WIFI_MONITORING:
                return "Turn on wifi monitoring";
            case TURN_OFF_WIFI_MONITORING:
                return "Turn off wifi monitoring";
            case START_WIFI_REPAIR:
                return "Repair wifi";
            case NO_RESPONSE:
            default:
                return Utils.EMPTY_STRING;
        }
    }

}
