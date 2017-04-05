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
        public static final int DOWNLOAD_AND_UPLOAD = 0;
        public static final int DOWNLOAD = 1;
        public static final int UPLOAD = 2;
        public static final int CONFIG_FETCH = 3;
        public static final int SERVER_FETCH = 4;
        public static final int IDLE = 5;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({BandwidthTestErrorCodes.ERROR_UNKNOWN, BandwidthTestErrorCodes.ERROR_FETCHING_CONFIG,
            BandwidthTestErrorCodes.ERROR_PARSING_CONFIG, BandwidthTestErrorCodes.ERROR_FETCHING_SERVER_INFO,
            BandwidthTestErrorCodes.ERROR_PARSING_SERVER_INFO, BandwidthTestErrorCodes.ERROR_SELECTING_BEST_SERVER,
            BandwidthTestErrorCodes.ERROR_INVALID_HTTP_RESPONSE, BandwidthTestErrorCodes.ERROR_SOCKET_ERROR,
            BandwidthTestErrorCodes.ERROR_SOCKET_TIMEOUT, BandwidthTestErrorCodes.ERROR_CONNECTION_ERROR,
            BandwidthTestErrorCodes.ERROR_TEST_INTERRUPTED})
    public @interface BandwidthTestErrorCodes {
        public static final int ERROR_UNKNOWN = 1;
        public static final int ERROR_FETCHING_CONFIG = 2;
        public static final int ERROR_PARSING_CONFIG = 3;
        public static final int ERROR_FETCHING_SERVER_INFO = 4;
        public static final int ERROR_PARSING_SERVER_INFO = 5;
        public static final int ERROR_SELECTING_BEST_SERVER = 6;
        public static final int ERROR_INVALID_HTTP_RESPONSE = 7;
        public static final int ERROR_SOCKET_ERROR = 8;
        public static final int ERROR_SOCKET_TIMEOUT = 9;
        public static final int ERROR_CONNECTION_ERROR = 10;
        public static final int ERROR_TEST_INTERRUPTED = 11;
    }
}
