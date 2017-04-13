package com.inceptai.dobby.wifi;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.Subscribe;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.eventbus.DobbyEvent;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.utils.Utils;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.TimeUnit;

import static com.inceptai.dobby.DobbyApplication.TAG;


/**
 * Created by vivek on 4/12/17.
 */
public class ConnectivityAnalyzer {
    public static int MIN_STRING_LENGTH_TO_FETCH = 1000; //1Kbyte
    public static String URL_FOR_CONNECTIVITY_TEST = "http://www.google.com";
    private static int MAX_SCHEDULING_TRIES_FOR_CHECKING_WIFI_CONNECTIVITY = 5;
    private static int GAP_BETWEEN_CONNECTIIVITY_CHECKS_MS = 2000;
    private static int MAX_TRIES_AFTER_CONNECTIVITY_CHECK_RETURNS_FALSE = 1;



    @Retention(RetentionPolicy.SOURCE)
    @IntDef({WifiConnectivityMode.CONNECTED_AND_ONLINE, WifiConnectivityMode.CONNECTED_AND_OFFLINE,
            WifiConnectivityMode.ON_AND_DISCONNECTED, WifiConnectivityMode.OFF, WifiConnectivityMode.UNKNOWN})
    public @interface WifiConnectivityMode {
        int CONNECTED_AND_ONLINE = 0;
        int CONNECTED_AND_OFFLINE = 1;
        int ON_AND_DISCONNECTED = 2;
        int OFF = 3;
        int UNKNOWN = 4;
    }

    @WifiConnectivityMode
    private int wifiConnectivityMode;
    // Store application context to prevent leaks and crashes from an activity going out of scope.
    private Context context;
    private DobbyThreadpool threadpool;
    private DobbyEventBus eventBus;
    private ConnectivityManager connectivityManager;
    private ConnectivityAnalyzerNetworkCallback networkCallback;

    private ConnectivityAnalyzer(Context context, ConnectivityManager connectivityManager,
                                 DobbyThreadpool threadpool, DobbyEventBus eventBus) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(threadpool);
        Preconditions.checkNotNull(eventBus);
        this.context = context.getApplicationContext();
        this.threadpool = threadpool;
        this.eventBus = eventBus;
        this.connectivityManager = connectivityManager;
        eventBus.registerListener(this);
        wifiConnectivityMode = WifiConnectivityMode.UNKNOWN;
        networkCallback = new ConnectivityAnalyzerNetworkCallback();
        // Make sure we're running on Honeycomb or higher to use ActionBar APIs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            this.connectivityManager.registerDefaultNetworkCallback(this.networkCallback);
        }
    }

    /**
     * Factory constructor to create an instance
     * @param context Application context.
     * @return Instance of WifiAnalyzer or null on error.
     */
    @Nullable
    public static ConnectivityAnalyzer create(Context context, DobbyThreadpool threadpool, DobbyEventBus eventBus) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            return new ConnectivityAnalyzer(context.getApplicationContext(), connectivityManager, threadpool, eventBus);
        }
        return null;
    }

    public boolean performConnectivityTest(NetworkInfo activeNetwork) {
        boolean connectedAndOnline = false;
        if (activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI && activeNetwork.isConnected()) {
            //Fetch google.com to validate
            String dataFetched = Utils.EMPTY_STRING;
            try {
                dataFetched = Utils.getDataFromUrl(URL_FOR_CONNECTIVITY_TEST, MIN_STRING_LENGTH_TO_FETCH);
            } catch (IOException e) {
                Log.v(TAG, "Unable to fetch: " + URL_FOR_CONNECTIVITY_TEST + " Except: " + e);
                return false;
            } catch (Exception e) {
                Log.v(TAG, "Exception : " + e);
            }
            Log.v(TAG, "Total bytes recv: " + dataFetched.length());
            connectedAndOnline = (dataFetched.length() > 0);
        }
        return connectedAndOnline;
    }

    private void updateWifiConnectivityMode(final int scheduleCount) {
        if (isWifiOnline()) {
            return;
        }
        Log.v(TAG, "Update wifi connectivity mode, scheduleCount =" + scheduleCount);
        final NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();

        // Reschedule network check if requested to do so and we have a null NetworkInfo.
        if (scheduleCount > 0 && activeNetwork == null) {
            rescheduleConnectivityTest(scheduleCount - 1);
            return;
        }
        Log.v(TAG, "active network is not null, activeNetwork:" + activeNetwork.toString());
        boolean isWifiConnectionOnline = false;
        try {
            isWifiConnectionOnline = performConnectivityTest(activeNetwork);
        } catch (IllegalStateException e) {
            Log.v(TAG, "Exception while checking connectivity: " + e);
        }
        if (scheduleCount > 0 && !isWifiConnectionOnline) {
            rescheduleConnectivityTest(MAX_TRIES_AFTER_CONNECTIVITY_CHECK_RETURNS_FALSE);  // Try once more before quitting.
            return;
        }
        if (isWifiConnectionOnline) {
            wifiConnectivityMode = WifiConnectivityMode.CONNECTED_AND_ONLINE;
            eventBus.postEvent(new DobbyEvent(DobbyEvent.EventType.WIFI_INTERNET_CONNECTIVITY_ONLINE));
        } else {
            wifiConnectivityMode = WifiConnectivityMode.CONNECTED_AND_OFFLINE;
            eventBus.postEvent(new DobbyEvent(DobbyEvent.EventType.WIFI_INTERNET_CONNECTIVITY_OFFLINE));
        }
    }

    private void rescheduleConnectivityTest(final int scheduleCount) {
        threadpool.getScheduledExecutorService().schedule(new Runnable() {
            @Override
            public void run() {
                Log.v(TAG, "Scheduled");
                updateWifiConnectivityMode(scheduleCount);
            }
        }, GAP_BETWEEN_CONNECTIIVITY_CHECKS_MS, TimeUnit.MILLISECONDS);
    }

    @ConnectivityAnalyzer.WifiConnectivityMode
    synchronized public int getWifiConnectivityMode() {
        return wifiConnectivityMode;
    }

    synchronized public boolean isWifiOnline() {
        return (wifiConnectivityMode == WifiConnectivityMode.CONNECTED_AND_ONLINE);
    }

    @Subscribe
    synchronized public void listen(DobbyEvent event) {
        int eventType = event.getLastEventType();
        Log.v(TAG, "Got event: " + event);
        switch(eventType) {
            case DobbyEvent.EventType.WIFI_STATE_ENABLED:
                wifiConnectivityMode = WifiConnectivityMode.ON_AND_DISCONNECTED;
                break;
            case DobbyEvent.EventType.WIFI_STATE_DISABLED:
            case DobbyEvent.EventType.WIFI_STATE_DISABLING:
            case DobbyEvent.EventType.WIFI_STATE_ENABLING:
                wifiConnectivityMode = WifiConnectivityMode.OFF;
                break;
            case DobbyEvent.EventType.WIFI_STATE_UNKNOWN:
                wifiConnectivityMode = WifiConnectivityMode.UNKNOWN;
                break;
            case DobbyEvent.EventType.WIFI_NOT_CONNECTED:
                wifiConnectivityMode = WifiConnectivityMode.ON_AND_DISCONNECTED;
                break;
            case DobbyEvent.EventType.DHCP_INFO_AVAILABLE:
            case DobbyEvent.EventType.WIFI_CONNECTED:
                updateWifiConnectivityMode(MAX_SCHEDULING_TRIES_FOR_CHECKING_WIFI_CONNECTIVITY /* try to schdule again twice */);
                break;
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private class ConnectivityAnalyzerNetworkCallback extends ConnectivityManager.NetworkCallback {
        //Connection Manager callbacks
        @Override
        public void onAvailable(Network network) {
            Log.v(TAG, "Inside onAvailable");
            if (!isWifiOnline()) {
                threadpool.submit(new Runnable() {
                    @Override
                    public void run() {
                        updateWifiConnectivityMode(0 /* Do not schedule again*/);
                    }
                });
            }
        }

        @Override
        public void onLost(Network network) {
        }

        @Override
        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
        }

        @Override
        public void onLosing(Network network, int maxMsToLive) {
        }

        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
        }

    }

}
