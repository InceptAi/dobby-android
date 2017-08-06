package com.inceptai.wifimonitoringservice.actionlibrary.utils;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.inceptai.wifimonitoringservice.actionlibrary.utils.ActionLibraryCodes.ErrorCodes.ERROR_WIFI_CONNECTED_AND_OFFLINE;
import static com.inceptai.wifimonitoringservice.actionlibrary.utils.ActionLibraryCodes.ErrorCodes.ERROR_WIFI_CONNECTED_AND_UNKNOWN;
import static com.inceptai.wifimonitoringservice.actionlibrary.utils.ActionLibraryCodes.ErrorCodes.ERROR_WIFI_IN_CAPTIVE_PORTAL;
import static com.inceptai.wifimonitoringservice.actionlibrary.utils.ActionLibraryCodes.ErrorCodes.ERROR_WIFI_OFF;
import static com.inceptai.wifimonitoringservice.actionlibrary.utils.ActionLibraryCodes.ErrorCodes.ERROR_WIFI_ON_AND_DISCONNECTED;
import static com.inceptai.wifimonitoringservice.actionlibrary.utils.ActionLibraryCodes.ErrorCodes.ERROR_WIFI_UNKNOWN_STATE;

/**
 * Created by vivek on 4/5/17.
 */

public class ActionLibraryCodes {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({BandwidthTestMode.DOWNLOAD_AND_UPLOAD, BandwidthTestMode.DOWNLOAD,
            BandwidthTestMode.UPLOAD, BandwidthTestMode.CONFIG_FETCH,
            BandwidthTestMode.SERVER_FETCH, BandwidthTestMode.STARTING, BandwidthTestMode.IDLE})
    public @interface BandwidthTestMode {
        int DOWNLOAD_AND_UPLOAD = 0;
        int DOWNLOAD = 1;
        int UPLOAD = 2;
        int CONFIG_FETCH = 3;
        int SERVER_FETCH = 4;
        int STARTING = 5;
        int IDLE = 6;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({ErrorCodes.ERROR_UNKNOWN, ErrorCodes.ERROR_FETCHING_CONFIG,
            ErrorCodes.ERROR_PARSING_CONFIG, ErrorCodes.ERROR_FETCHING_SERVER_INFO,
            ErrorCodes.ERROR_PARSING_SERVER_INFO, ErrorCodes.ERROR_SELECTING_BEST_SERVER,
            ErrorCodes.ERROR_INVALID_HTTP_RESPONSE, ErrorCodes.ERROR_SOCKET_ERROR,
            ErrorCodes.ERROR_SOCKET_TIMEOUT, ErrorCodes.ERROR_CONNECTION_ERROR,
            ErrorCodes.ERROR_TEST_INTERRUPTED, ErrorCodes.ERROR_TEST_ALREADY_RUNNING,
            ErrorCodes.ERROR_WIFI_OFFLINE, ErrorCodes.NO_ERROR, ErrorCodes.ERROR_DHCP_INFO_UNAVAILABLE,
            ErrorCodes.ERROR_PERFORMING_PING, ERROR_WIFI_IN_CAPTIVE_PORTAL,
            ERROR_WIFI_CONNECTED_AND_OFFLINE, ERROR_WIFI_CONNECTED_AND_UNKNOWN,
            ERROR_WIFI_ON_AND_DISCONNECTED, ERROR_WIFI_OFF,
            ErrorCodes.ERROR_WIFI_UNKNOWN_STATE, ErrorCodes.ERROR_UNINITIAlIZED})
    public @interface ErrorCodes {
        int ERROR_UNINITIAlIZED = -1;
        int NO_ERROR = 0;
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
        int ERROR_TEST_ALREADY_RUNNING = 12;
        int ERROR_WIFI_OFFLINE = 13;
        int ERROR_DHCP_INFO_UNAVAILABLE = 14;
        int ERROR_PERFORMING_PING = 15;
        int ERROR_WIFI_IN_CAPTIVE_PORTAL = 16;
        int ERROR_WIFI_CONNECTED_AND_OFFLINE = 17;
        int ERROR_WIFI_CONNECTED_AND_UNKNOWN = 18;
        int ERROR_WIFI_ON_AND_DISCONNECTED = 19;
        int ERROR_WIFI_OFF = 20;
        int ERROR_WIFI_UNKNOWN_STATE = 21;
    }

    public static String bandwidthTestErrorCodesToStrings(@ErrorCodes int errorCode) {
        switch (errorCode) {
            case ErrorCodes.NO_ERROR:
                return "NO_ERROR";
            case ErrorCodes.ERROR_FETCHING_CONFIG:
                return "ERROR_FETCHING_CONFIG";
            case ErrorCodes.ERROR_PARSING_CONFIG:
                return "ERROR_PARSING_CONFIG";
            case ErrorCodes.ERROR_FETCHING_SERVER_INFO:
                return "ERROR_FETCHING_SERVER_INFO";
            case ErrorCodes.ERROR_PARSING_SERVER_INFO:
                return "ERROR_PARSING_SERVER_INFO";
            case ErrorCodes.ERROR_SELECTING_BEST_SERVER:
                return "ERROR_SELECTING_BEST_SERVER";
            case ErrorCodes.ERROR_INVALID_HTTP_RESPONSE:
                return "ERROR_INVALID_HTTP_RESPONSE";
            case ErrorCodes.ERROR_SOCKET_ERROR:
                return "ERROR_SOCKET_ERROR";
            case ErrorCodes.ERROR_SOCKET_TIMEOUT:
                return "ERROR_SOCKET_TIMEOUT";
            case ErrorCodes.ERROR_CONNECTION_ERROR:
                return "ERROR_CONNECTION_ERROR";
            case ErrorCodes.ERROR_TEST_INTERRUPTED:
                return "ERROR_TEST_INTERRUPTED";
            case ErrorCodes.ERROR_TEST_ALREADY_RUNNING:
                return "ERROR_TEST_ALREADY_RUNNING";
            case ErrorCodes.ERROR_WIFI_OFFLINE:
                return "ERROR_WIFI_OFFLINE";
            case ErrorCodes.ERROR_UNINITIAlIZED:
                return "ERROR_UNINITIAlIZED";
            case ErrorCodes.ERROR_PERFORMING_PING:
                return "ERROR_PERFORMING_PING";
            case ERROR_WIFI_IN_CAPTIVE_PORTAL:
                return "ERROR_WIFI_IN_CAPTIVE_PORTAL";
            case ERROR_WIFI_CONNECTED_AND_OFFLINE:
                return "ERROR_WIFI_CONNECTED_AND_OFFLINE";
            case ERROR_WIFI_CONNECTED_AND_UNKNOWN:
                return "ERROR_WIFI_CONNECTED_AND_UNKNOWN";
            case ERROR_WIFI_ON_AND_DISCONNECTED:
                return "ERROR_WIFI_ON_AND_DISCONNECTED";
            case ERROR_WIFI_OFF:
                return "ERROR_WIFI_OFF";
            case ERROR_WIFI_UNKNOWN_STATE:
                return "ERROR_WIFI_UNKNOWN_STATE";
            case ErrorCodes.ERROR_UNKNOWN:
            default:
                return "ERROR_UNKNOWN";
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({BandwidthTestSnapshotType.INSTANTANEOUS_BANDWIDTH, BandwidthTestSnapshotType.FINAL_BANDWIDTH,
            BandwidthTestSnapshotType.SPEED_TEST_CONFIG, BandwidthTestSnapshotType.BEST_SERVER_DETAILS,
            BandwidthTestSnapshotType.SERVER_INFORMATION, BandwidthTestSnapshotType.CLOSEST_SERVERS})
    public @interface BandwidthTestSnapshotType {
        int INSTANTANEOUS_BANDWIDTH = 0;
        int FINAL_BANDWIDTH = 1;
        int SPEED_TEST_CONFIG = 2;
        int BEST_SERVER_DETAILS = 3;
        int SERVER_INFORMATION = 4;
        int CLOSEST_SERVERS = 5;
    }


}
