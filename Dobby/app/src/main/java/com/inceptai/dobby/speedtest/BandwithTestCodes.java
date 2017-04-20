package com.inceptai.dobby.speedtest;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by vivek on 4/5/17.
 */

public class BandwithTestCodes {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({BandwidthTestMode.DOWNLOAD_AND_UPLOAD, BandwidthTestMode.DOWNLOAD,
            BandwidthTestMode.UPLOAD, BandwidthTestMode.CONFIG_FETCH,
            BandwidthTestMode.SERVER_FETCH, BandwidthTestMode.IDLE})
    public @interface BandwidthTestMode {
        int DOWNLOAD_AND_UPLOAD = 0;
        int DOWNLOAD = 1;
        int UPLOAD = 2;
        int CONFIG_FETCH = 3;
        int SERVER_FETCH = 4;
        int IDLE = 5;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({BandwidthTestErrorCodes.ERROR_UNKNOWN, BandwidthTestErrorCodes.ERROR_FETCHING_CONFIG,
            BandwidthTestErrorCodes.ERROR_PARSING_CONFIG, BandwidthTestErrorCodes.ERROR_FETCHING_SERVER_INFO,
            BandwidthTestErrorCodes.ERROR_PARSING_SERVER_INFO, BandwidthTestErrorCodes.ERROR_SELECTING_BEST_SERVER,
            BandwidthTestErrorCodes.ERROR_INVALID_HTTP_RESPONSE, BandwidthTestErrorCodes.ERROR_SOCKET_ERROR,
            BandwidthTestErrorCodes.ERROR_SOCKET_TIMEOUT, BandwidthTestErrorCodes.ERROR_CONNECTION_ERROR,
            BandwidthTestErrorCodes.ERROR_TEST_INTERRUPTED})
    public @interface BandwidthTestErrorCodes {
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
    @IntDef({BandwidthTestExceptionErrorCodes.TEST_STARTED_NO_EXCEPTION,
            BandwidthTestExceptionErrorCodes.TEST_ALREADY_RUNNING,
            BandwidthTestExceptionErrorCodes.GETTING_CONFIG_FAILED,
            BandwidthTestExceptionErrorCodes.GETTING_SERVER_INFORMATION_FAILED,
            BandwidthTestExceptionErrorCodes.GETTING_BEST_SERVER_FAILED,
            BandwidthTestExceptionErrorCodes.NETWORK_OFFLINE,
            BandwidthTestExceptionErrorCodes.UNKNOWN})
    public @interface BandwidthTestExceptionErrorCodes {
        int TEST_STARTED_NO_EXCEPTION = 0;
        int TEST_ALREADY_RUNNING = 1;
        int GETTING_CONFIG_FAILED = 2;
        int GETTING_SERVER_INFORMATION_FAILED = 3;
        int GETTING_BEST_SERVER_FAILED = 4;
        int NETWORK_OFFLINE = 5;
        int UNKNOWN = 6;
    }
}
