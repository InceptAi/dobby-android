package com.inceptai.wifiexpert.expertSystem;

import android.support.annotation.IntDef;

import com.inceptai.wifiexpert.utils.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import static com.inceptai.wifiexpert.expertSystem.StructuredUserResponse.ResponseType.ASK_ABOUT_DOBBY;
import static com.inceptai.wifiexpert.expertSystem.StructuredUserResponse.ResponseType.CANCEL;
import static com.inceptai.wifiexpert.expertSystem.StructuredUserResponse.ResponseType.CONTACT_HUMAN_EXPERT;
import static com.inceptai.wifiexpert.expertSystem.StructuredUserResponse.ResponseType.LIST_ALL_FUNCTIONS;
import static com.inceptai.wifiexpert.expertSystem.StructuredUserResponse.ResponseType.NO;
import static com.inceptai.wifiexpert.expertSystem.StructuredUserResponse.ResponseType.NO_COMMENTS;
import static com.inceptai.wifiexpert.expertSystem.StructuredUserResponse.ResponseType.NO_RESPONSE;
import static com.inceptai.wifiexpert.expertSystem.StructuredUserResponse.ResponseType.RUN_ALL_DIAGNOSTICS;
import static com.inceptai.wifiexpert.expertSystem.StructuredUserResponse.ResponseType.RUN_BW_TESTS;
import static com.inceptai.wifiexpert.expertSystem.StructuredUserResponse.ResponseType.RUN_WIFI_TESTS;
import static com.inceptai.wifiexpert.expertSystem.StructuredUserResponse.ResponseType.SHOW_LAST_SUGGESTION_DETAILS;
import static com.inceptai.wifiexpert.expertSystem.StructuredUserResponse.ResponseType.YES;

/**
 * Represents an action (to be taken or in progress) by the InferenceEngine.
 */

public class StructuredUserResponse {
    public static final StructuredUserResponse RESPONSE_NONE = new StructuredUserResponse(Utils.EMPTY_STRING, NO_RESPONSE);

    @IntDef({ResponseType.YES,
            ResponseType.NO,
            ResponseType.CANCEL,
            ResponseType.RUN_ALL_DIAGNOSTICS,
            ResponseType.RUN_BW_TESTS,
            ResponseType.RUN_WIFI_TESTS,
            ResponseType.LIST_ALL_FUNCTIONS,
            ResponseType.ASK_ABOUT_DOBBY,
            ResponseType.NO_RESPONSE,
            ResponseType.SHOW_LAST_SUGGESTION_DETAILS,
            ResponseType.NO_COMMENTS,
            ResponseType.CONTACT_HUMAN_EXPERT
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

    public StructuredUserResponse(String userResponse, @ResponseType  int responseType) {
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
            case NO_RESPONSE:
            default:
                return Utils.EMPTY_STRING;
        }
    }

}
