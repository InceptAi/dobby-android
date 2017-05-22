package com.inceptai.dobby.ai;

import android.support.annotation.IntDef;

import com.inceptai.dobby.utils.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

import static com.inceptai.dobby.ai.UserResponse.ResponseType.ASK_ABOUT_DOBBY;
import static com.inceptai.dobby.ai.UserResponse.ResponseType.CANCEL;
import static com.inceptai.dobby.ai.UserResponse.ResponseType.LIST_ALL_FUNCTIONS;
import static com.inceptai.dobby.ai.UserResponse.ResponseType.NO;
import static com.inceptai.dobby.ai.UserResponse.ResponseType.NO_RESPONSE;
import static com.inceptai.dobby.ai.UserResponse.ResponseType.RUN_ALL_DIAGNOSTICS;
import static com.inceptai.dobby.ai.UserResponse.ResponseType.RUN_BW_TESTS;
import static com.inceptai.dobby.ai.UserResponse.ResponseType.RUN_WIFI_TESTS;
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
            ResponseType.NO_RESPONSE
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
                return "yes";
            case NO:
                return "no";
            case CANCEL:
                return "cancel";
            case RUN_ALL_DIAGNOSTICS:
                return "slow internet";
            case RUN_BW_TESTS:
                return "run speed test";
            case RUN_WIFI_TESTS:
                return "run wifi test";
            case LIST_ALL_FUNCTIONS:
                return "list all functions";
            case ASK_ABOUT_DOBBY:
                return "what is Dobby ?";
            case NO_RESPONSE:
            default:
                return Utils.EMPTY_STRING;
        }
    }

}
