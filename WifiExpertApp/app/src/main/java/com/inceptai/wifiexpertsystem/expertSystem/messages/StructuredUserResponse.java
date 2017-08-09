package com.inceptai.wifiexpertsystem.expertSystem.messages;

import android.support.annotation.IntDef;

import com.inceptai.wifiexpertsystem.utils.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import static com.inceptai.wifiexpertsystem.expertSystem.messages.StructuredUserResponse.ResponseType.ASK_ABOUT_DOBBY;
import static com.inceptai.wifiexpertsystem.expertSystem.messages.StructuredUserResponse.ResponseType.CANCEL;
import static com.inceptai.wifiexpertsystem.expertSystem.messages.StructuredUserResponse.ResponseType.CONTACT_HUMAN_EXPERT;
import static com.inceptai.wifiexpertsystem.expertSystem.messages.StructuredUserResponse.ResponseType.INVALID;
import static com.inceptai.wifiexpertsystem.expertSystem.messages.StructuredUserResponse.ResponseType.LIST_ALL_FUNCTIONS;
import static com.inceptai.wifiexpertsystem.expertSystem.messages.StructuredUserResponse.ResponseType.NO;
import static com.inceptai.wifiexpertsystem.expertSystem.messages.StructuredUserResponse.ResponseType.NO_COMMENTS;
import static com.inceptai.wifiexpertsystem.expertSystem.messages.StructuredUserResponse.ResponseType.NO_RESPONSE;
import static com.inceptai.wifiexpertsystem.expertSystem.messages.StructuredUserResponse.ResponseType.RUN_ALL_DIAGNOSTICS;
import static com.inceptai.wifiexpertsystem.expertSystem.messages.StructuredUserResponse.ResponseType.RUN_BW_TESTS;
import static com.inceptai.wifiexpertsystem.expertSystem.messages.StructuredUserResponse.ResponseType.RUN_WIFI_TESTS;
import static com.inceptai.wifiexpertsystem.expertSystem.messages.StructuredUserResponse.ResponseType.SHOW_LAST_SUGGESTION_DETAILS;
import static com.inceptai.wifiexpertsystem.expertSystem.messages.StructuredUserResponse.ResponseType.YES;

/**
 * Represents an action (to be taken or in progress) by the InferenceEngine.
 */

public class StructuredUserResponse {
    public static final StructuredUserResponse RESPONSE_NONE = new StructuredUserResponse(NO_RESPONSE);
    private static final String YES_STRING = "Yes";
    private static final String NO_STRING = "No";
    private static final String CANCEL_STRING = "Cancel";
    private static final String RUN_ALL_DIAGNOSTICS_STRING = "Slow internet";
    private static final String RUN_BW_TESTS_STRING = "Run speed test";
    private static final String RUN_WIFI_TESTS_STRING = "Check wifi";
    private static final String LIST_ALL_FUNCTIONS_STRING = "List functions";
    private static final String ASK_ABOUT_DOBBY_STRING = "About Wifi Expert";
    private static final String NO_RESPONSE_STRING = Utils.EMPTY_STRING;
    private static final String SHOW_LAST_SUGGESTION_DETAILS_STRING = "Details";
    private static final String NO_COMMENTS_STRING = "No comments";
    private static final String CONTACT_HUMAN_EXPERT_STRING = "Contact Human Expert";



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
            ResponseType.CONTACT_HUMAN_EXPERT,
            INVALID
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
        int INVALID = 12;
    }

    /* User response to be shown, null for no response. */
    private String responseString;

    @ResponseType
    private int responseType;

    public String getResponseString() {
        return responseString;
    }

    @ResponseType
    public int getResponseType() {
        return responseType;
    }

    public StructuredUserResponse(@ResponseType  int responseType) {
        this.responseString = getStringForResponseType(responseType);
        this.responseType = responseType;
    }

    public StructuredUserResponse(String message) {
        this.responseString = message;
        this.responseType = getResponseTypeFromString(message);
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
                return YES_STRING;
            case NO:
                return NO_STRING;
            case CANCEL:
                return CANCEL_STRING;
            case RUN_ALL_DIAGNOSTICS:
                return RUN_ALL_DIAGNOSTICS_STRING;
            case RUN_BW_TESTS:
                return RUN_BW_TESTS_STRING;
            case RUN_WIFI_TESTS:
                return RUN_WIFI_TESTS_STRING;
            case LIST_ALL_FUNCTIONS:
                return LIST_ALL_FUNCTIONS_STRING;
            case ASK_ABOUT_DOBBY:
                return ASK_ABOUT_DOBBY_STRING;
            case SHOW_LAST_SUGGESTION_DETAILS:
                return SHOW_LAST_SUGGESTION_DETAILS_STRING;
            case NO_COMMENTS:
                return NO_COMMENTS_STRING;
            case CONTACT_HUMAN_EXPERT:
                return CONTACT_HUMAN_EXPERT_STRING;
            case NO_RESPONSE:
            default:
                return Utils.EMPTY_STRING;
        }
    }

    @StructuredUserResponse.ResponseType
    public static int getResponseTypeFromString(String responseString) {
        switch(responseString) {
            case YES_STRING:
                return YES;
            case NO_STRING:
                return NO;
            case CANCEL_STRING:
                return CANCEL;
            case RUN_ALL_DIAGNOSTICS_STRING:
                return RUN_ALL_DIAGNOSTICS;
            case RUN_BW_TESTS_STRING:
                return RUN_BW_TESTS;
            case RUN_WIFI_TESTS_STRING:
                return RUN_WIFI_TESTS;
            case LIST_ALL_FUNCTIONS_STRING:
                return LIST_ALL_FUNCTIONS;
            case ASK_ABOUT_DOBBY_STRING:
                return ASK_ABOUT_DOBBY;
            case SHOW_LAST_SUGGESTION_DETAILS_STRING:
                return SHOW_LAST_SUGGESTION_DETAILS;
            case NO_COMMENTS_STRING:
                return NO_COMMENTS;
            case CONTACT_HUMAN_EXPERT_STRING:
                return CONTACT_HUMAN_EXPERT;
            case NO_RESPONSE_STRING:
                return NO_RESPONSE;
            default:
                return INVALID;
        }
    }



}
