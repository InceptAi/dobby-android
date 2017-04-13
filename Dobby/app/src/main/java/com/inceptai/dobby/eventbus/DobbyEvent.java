package com.inceptai.dobby.eventbus;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.inceptai.dobby.eventbus.DobbyEvent.EventType.NO_EVENT_RECEIVED;
import static com.inceptai.dobby.eventbus.DobbyEvent.EventType.WIFI_STATE_CHANGED;

/**
 * Created by vivek on 4/10/17.
 */

public class DobbyEvent {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({NO_EVENT_RECEIVED, WIFI_STATE_CHANGED, EventType.WIFI_STATE_ENABLED,
            EventType.WIFI_STATE_DISABLED, EventType.WIFI_STATE_ENABLING, EventType.WIFI_STATE_DISABLING,
            EventType.WIFI_STATE_UNKNOWN, EventType.WIFI_SCAN_AVAILABLE, EventType.WIFI_RSSI_CHANGED,
            EventType.NETWORK_STATE_CHANGED, EventType.DHCP_INFO_AVAILABLE, EventType.WIFI_INTERNET_CONNECTIVITY_ONLINE,
            EventType.WIFI_INTERNET_CONNECTIVITY_OFFLINE, EventType.WIFI_CONNECTED, EventType.WIFI_NOT_CONNECTED,
            EventType.PING_STARTED, EventType.PING_INFO_AVAILABLE, EventType.PING_FAILED,
            EventType.BWTEST_INFO_AVAILABLE, EventType.BWTEST_FAILED})
    public @interface EventType {
        //Unknown event type
        int NO_EVENT_RECEIVED = 0;

        //WiFi state events

        int WIFI_STATE_CHANGED = 1;
        int WIFI_STATE_ENABLED = 2;
        int WIFI_STATE_DISABLED = 3;
        int	WIFI_STATE_ENABLING = 4;
        int	WIFI_STATE_DISABLING = 5;
        int WIFI_STATE_UNKNOWN = 6;

        //WiFi scan events
        int WIFI_SCAN_AVAILABLE = 7;
        int WIFI_RSSI_CHANGED = 8;

        //WiFi network changed event
        int NETWORK_STATE_CHANGED = 9;
        int DHCP_INFO_AVAILABLE = 10;
        int WIFI_INTERNET_CONNECTIVITY_ONLINE = 11;
        int WIFI_INTERNET_CONNECTIVITY_OFFLINE = 12;
        int WIFI_NOT_CONNECTED = 13;
        int WIFI_CONNECTED = 14;

        //Ping notifications
        int PING_STARTED = 15;
        int PING_INFO_AVAILABLE = 16;
        int PING_FAILED = 17;

        //Ping notifications
        int BWTEST_INFO_AVAILABLE = 18;
        int BWTEST_FAILED = 19;
    }

    @DobbyEvent.EventType
    private int eventType;
    private long lastEventTimestampMs;
    private int eventCount;

    public DobbyEvent(@EventType int eventType) {
        this.eventType = eventType;
        lastEventTimestampMs = System.currentTimeMillis();
        eventCount = 0;
    }

    public long getLastEventTimestampMs(){
        return lastEventTimestampMs;
    }

    @EventType
    public int getLastEventType(){
        return eventType;
    }

    @Override
    public String toString() {
        switch(eventType) {
            case EventType.NO_EVENT_RECEIVED:
                return "NO_EVENT_RECEIVED";
            case EventType.WIFI_STATE_CHANGED:
                return "WIFI_STATE_CHANGED";
            case EventType.WIFI_STATE_ENABLED:
                return "WIFI_STATE_ENABLED";
            case EventType.WIFI_STATE_DISABLED:
                return "WIFI_STATE_DISABLED";
            case EventType.WIFI_STATE_ENABLING:
                return "WIFI_STATE_ENABLING";
            case EventType.WIFI_STATE_DISABLING:
                return "WIFI_STATE_DISABLING";
            case EventType.WIFI_STATE_UNKNOWN:
                return "WIFI_STATE_UNKNOWN";
            case EventType.WIFI_SCAN_AVAILABLE:
                return "WIFI_SCAN_AVAILABLE";
            case EventType.WIFI_RSSI_CHANGED:
                return "WIFI_RSSI_CHANGED";

            //WiFi network changed event
            case EventType.NETWORK_STATE_CHANGED:
                return "NETWORK_STATE_CHANGED";
            case EventType.DHCP_INFO_AVAILABLE:
                return "DHCP_INFO_AVAILABLE";
            case EventType.WIFI_INTERNET_CONNECTIVITY_ONLINE:
                return "WIFI_INTERNET_CONNECTIVITY_ONLINE";
            case EventType.WIFI_INTERNET_CONNECTIVITY_OFFLINE:
                return "WIFI_INTERNET_CONNECTIVITY_OFFLINE";
            case EventType.WIFI_NOT_CONNECTED:
                return "WIFI_NOT_CONNECTED";
            case EventType.WIFI_CONNECTED:
                return "WIFI_CONNECTED";

            //Ping notifications
            case EventType.PING_STARTED:
                return "PING_STARTED";
            case EventType.PING_INFO_AVAILABLE:
                return "PING_INFO_AVAILABLE";
            case EventType.PING_FAILED:
                return "PING_FAILED";

            //BWTest notifications
            case EventType.BWTEST_INFO_AVAILABLE:
                return "BWTEST_INFO_AVAILABLE";
            case EventType.BWTEST_FAILED:
                return "BWTEST_FAILED";

            default:
                return "UNKNOWN EVENT TYPE";
        }
    }
}