package com.inceptai.wifiexpert.eventbus;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.inceptai.wifiexpert.eventbus.DobbyEvent.EventType.NO_EVENT_RECEIVED;
import static com.inceptai.wifiexpert.eventbus.DobbyEvent.EventType.WIFI_STATE_CHANGED;

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
            EventType.BWTEST_INFO_AVAILABLE, EventType.BWTEST_FAILED, EventType.HANGING_ON_DHCP,
            EventType.HANGING_ON_AUTHENTICATING, EventType.HANGING_ON_SCANNING, EventType.FREQUENT_DISCONNECTIONS,
            EventType.BANDWIDTH_TEST_STARTING, EventType.WIFI_SCAN_STARTING,
            EventType.PING_GRADE_AVAILABLE, EventType.GATEWAY_HTTP_GRADE_AVAILABLE, EventType.WIFI_GRADE_AVAILABLE,
            EventType.SUGGESTIONS_AVAILABLE, EventType.WIFI_INTERNET_CONNECTIVITY_CAPTIVE_PORTAL, EventType.BANDWIDTH_GRADE_AVAILABLE,
            EventType.BANDWIDTH_TEST_FAILED_WIFI_OFFLINE, EventType.EXPERT_ACTION_STARTED, EventType.EXPERT_ACTION_COMPLETED, EventType.EXPERT_ASKED_FOR_ACTION})
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

        //Wifi state problems
        int HANGING_ON_DHCP = 20;
        int HANGING_ON_AUTHENTICATING = 21;
        int HANGING_ON_SCANNING = 22;
        int FREQUENT_DISCONNECTIONS = 23;

        // Bandwidth event with payload to listen in.
        // Payload type: BandwidthObserver.
        int BANDWIDTH_TEST_STARTING = 24;
        int WIFI_SCAN_STARTING = 25;
        int PING_GRADE_AVAILABLE = 27;
        int GATEWAY_HTTP_GRADE_AVAILABLE = 28;
        int WIFI_GRADE_AVAILABLE = 29;
        int SUGGESTIONS_AVAILABLE = 30;
        int WIFI_INTERNET_CONNECTIVITY_CAPTIVE_PORTAL = 31;
        int BANDWIDTH_GRADE_AVAILABLE = 32;
        int BANDWIDTH_TEST_FAILED_WIFI_OFFLINE = 33;

        //Dobby AI action events
        int EXPERT_ACTION_STARTED = 101;
        int EXPERT_ACTION_COMPLETED = 102;
        int EXPERT_ASKED_FOR_ACTION = 103;
    }

    @DobbyEvent.EventType
    private int eventType;
    private long lastEventTimestampMs;
    private int eventCount;
    private Object payload;

    public DobbyEvent(@EventType int eventType) {
        this.eventType = eventType;
        lastEventTimestampMs = System.currentTimeMillis();
        eventCount = 0;
    }

    public DobbyEvent(@EventType int eventType, Object payload) {
        this(eventType);
        this.payload = payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public Object getPayload() {
        return payload;
    }

    public long getLastEventTimestampMs(){
        return lastEventTimestampMs;
    }

    @EventType
    public int getEventType(){
        return eventType;
    }

    @Override
    public String toString() {
        switch(eventType) {
            case NO_EVENT_RECEIVED:
                return "NO_EVENT_RECEIVED";
            case WIFI_STATE_CHANGED:
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
            case EventType.WIFI_SCAN_STARTING:
                return "WIFI_SCAN_STARTING";
            case EventType.WIFI_GRADE_AVAILABLE:
                return "WIFI_GRADE_AVAILABLE";

            //WiFi network changed event
            case EventType.NETWORK_STATE_CHANGED:
                return "NETWORK_STATE_CHANGED";
            case EventType.DHCP_INFO_AVAILABLE:
                return "DHCP_INFO_AVAILABLE";
            case EventType.WIFI_INTERNET_CONNECTIVITY_ONLINE:
                return "WIFI_INTERNET_CONNECTIVITY_ONLINE";
            case EventType.WIFI_INTERNET_CONNECTIVITY_OFFLINE:
                return "WIFI_INTERNET_CONNECTIVITY_OFFLINE";
            case EventType.WIFI_INTERNET_CONNECTIVITY_CAPTIVE_PORTAL:
                return "WIFI_INTERNET_CONNECTIVITY_CAPTIVE_PORTAL";
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
            case EventType.PING_GRADE_AVAILABLE:
                return "PING_GRADE_AVAILABLE";
            case EventType.GATEWAY_HTTP_GRADE_AVAILABLE:
                return "GATEWAY_HTTP_GRADE_AVAILABLE";
            case EventType.BANDWIDTH_GRADE_AVAILABLE:
                return "BANDWIDTH_GRADE_AVAILABLE";

            //BWTest notifications
            case EventType.BANDWIDTH_TEST_STARTING:
                return "BANDWIDTH_TEST_STARTING";
            case EventType.BWTEST_INFO_AVAILABLE:
                return "BWTEST_INFO_AVAILABLE";
            case EventType.BWTEST_FAILED:
                return "BWTEST_FAILED";

            //Wifi state problems
            case EventType.HANGING_ON_DHCP:
                return "HANGING_ON_DHCP";
            case EventType.HANGING_ON_AUTHENTICATING:
                return "HANGING_ON_AUTHENTICATING";
            case EventType.HANGING_ON_SCANNING:
                return "HANGING_ON_SCANNING";
            case EventType.FREQUENT_DISCONNECTIONS:
                return "FREQUENT_DISCONNECTIONS";
            case EventType.SUGGESTIONS_AVAILABLE:
                return "SUGGESTIONS_AVAILABLE";
            case EventType.BANDWIDTH_TEST_FAILED_WIFI_OFFLINE:
                return "BANDWIDTH_TEST_FAILED_WIFI_OFFLINE";

            //Expert events
            case EventType.EXPERT_ACTION_STARTED:
                return "EXPERT_ACTION_STARTED";
            case EventType.EXPERT_ACTION_COMPLETED:
                return "EXPERT_ACTION_COMPLETED";


            default:
                return "UNKNOWN EVENT TYPE:" + eventType;
        }
    }
}
