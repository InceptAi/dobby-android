package com.inceptai.dobby.speedtest;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by vivek on 4/5/17.
 */

public class BandwithTestCodes {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TestMode.DOWNLOAD_AND_UPLOAD, TestMode.DOWNLOAD,
            TestMode.UPLOAD, TestMode.CONFIG_FETCH,
            TestMode.SERVER_FETCH, TestMode.IDLE})
    public @interface TestMode {
        int DOWNLOAD_AND_UPLOAD = 0;
        int DOWNLOAD = 1;
        int UPLOAD = 2;
        int CONFIG_FETCH = 3;
        int SERVER_FETCH = 4;
        int IDLE = 5;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ErrorCodes.ERROR_UNKNOWN, ErrorCodes.ERROR_FETCHING_CONFIG,
            ErrorCodes.ERROR_PARSING_CONFIG, ErrorCodes.ERROR_FETCHING_SERVER_INFO,
            ErrorCodes.ERROR_PARSING_SERVER_INFO, ErrorCodes.ERROR_SELECTING_BEST_SERVER,
            ErrorCodes.ERROR_INVALID_HTTP_RESPONSE, ErrorCodes.ERROR_SOCKET_ERROR,
            ErrorCodes.ERROR_SOCKET_TIMEOUT, ErrorCodes.ERROR_CONNECTION_ERROR,
            ErrorCodes.ERROR_TEST_INTERRUPTED})
    public @interface ErrorCodes {
        int ERROR_UNKNOWN = 1;
        int ERROR_FETCHING_CONFIG = 2;
        int ERROR_PARSING_CONFIG = 3;
        int ERROR_FETCHING_SERVER_INFO = 4;
        int ERROR_PARSING_SERVER_INFO = 5;
        int ERROR_SELECTING_BEST_SERVER = 6;
        int ERROR_INVALID_HTTP_RESPONSE = 7;
        int ERROR_SOCKET_ERROR = 8;
        int ERROR_SOCKET_TIMEOUT = 9;
        int ERROR_CONNECTION_ERROR = 10;
        int ERROR_TEST_INTERRUPTED = 11;
    }


    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ExceptionCodes.TEST_STARTED_NO_EXCEPTION,
            ExceptionCodes.TEST_ALREADY_RUNNING,
            ExceptionCodes.GETTING_CONFIG_FAILED,
            ExceptionCodes.GETTING_SERVER_INFORMATION_FAILED,
            ExceptionCodes.GETTING_BEST_SERVER_FAILED,
            ExceptionCodes.NETWORK_OFFLINE,
            ExceptionCodes.ATTEMPTING_TO_START,
            ExceptionCodes.UNKNOWN})
    public @interface ExceptionCodes {
        int TEST_STARTED_NO_EXCEPTION = 0;
        int TEST_ALREADY_RUNNING = 1;
        int GETTING_CONFIG_FAILED = 2;
        int GETTING_SERVER_INFORMATION_FAILED = 3;
        int GETTING_BEST_SERVER_FAILED = 4;
        int NETWORK_OFFLINE = 5;
        int UNKNOWN = 6;
        int ATTEMPTING_TO_START = 7;
    }
}
