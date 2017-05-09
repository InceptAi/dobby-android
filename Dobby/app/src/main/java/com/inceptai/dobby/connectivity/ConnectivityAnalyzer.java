package com.inceptai.dobby.connectivity;

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

import com.google.common.base.Preconditions;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.eventbus.DobbyEvent;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.utils.DobbyLog;
import com.inceptai.dobby.utils.Utils;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static com.inceptai.dobby.connectivity.ConnectivityAnalyzer.WifiConnectivityMode.CONNECTED_AND_CAPTIVE_PORTAL;
import static com.inceptai.dobby.connectivity.ConnectivityAnalyzer.WifiConnectivityMode.CONNECTED_AND_OFFLINE;
import static com.inceptai.dobby.connectivity.ConnectivityAnalyzer.WifiConnectivityMode.CONNECTED_AND_ONLINE;
import static com.inceptai.dobby.connectivity.ConnectivityAnalyzer.WifiConnectivityMode.CONNECTED_AND_UNKNOWN;
import static com.inceptai.dobby.connectivity.ConnectivityAnalyzer.WifiConnectivityMode.OFF;
import static com.inceptai.dobby.connectivity.ConnectivityAnalyzer.WifiConnectivityMode.ON_AND_DISCONNECTED;


/**
 * Created by vivek on 4/12/17.
 */
public class ConnectivityAnalyzer {
    private static int MAX_STRING_LENGTH_TO_FETCH = 1000; // 1Kbyte
    private static String URL_FOR_CONNECTIVITY_AND_PORTAL_TEST = "http://clients3.google.com/generate_204";
    protected static int MAX_SCHEDULING_TRIES_FOR_CHECKING_WIFI_CONNECTIVITY = 0; // Only once
    private static int GAP_BETWEEN_CONNECTIIVITY_CHECKS_MS = 2000;
    private static final int PORTAL_TEST_READ_TIMEOUT_MS = 3000;
    private static final int PORTAL_TEST_CONNECTION_TIMEOUT_MS = 3000;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({WifiConnectivityMode.CONNECTED_AND_ONLINE, WifiConnectivityMode.CONNECTED_AND_CAPTIVE_PORTAL,
            WifiConnectivityMode.CONNECTED_AND_OFFLINE, WifiConnectivityMode.ON_AND_DISCONNECTED,
            WifiConnectivityMode.OFF, WifiConnectivityMode.UNKNOWN, WifiConnectivityMode.CONNECTED_AND_UNKNOWN,
            WifiConnectivityMode.MAX_MODES})
    public @interface WifiConnectivityMode {
        int CONNECTED_AND_ONLINE = 0;
        int CONNECTED_AND_CAPTIVE_PORTAL = 1;
        int CONNECTED_AND_OFFLINE = 2;
        int CONNECTED_AND_UNKNOWN = 3;
        int ON_AND_DISCONNECTED = 4;
        int OFF = 5;
        int UNKNOWN = 6;
        int MAX_MODES = 7;
    }

    @WifiConnectivityMode
    protected int wifiConnectivityMode;
    // Store application context to prevent leaks and crashes from an activity going out of scope.
    protected Context context;
    protected DobbyThreadpool threadpool;
    protected DobbyEventBus eventBus;
    protected ConnectivityManager connectivityManager;

    protected ConnectivityAnalyzer(Context context, ConnectivityManager connectivityManager,
                                 DobbyThreadpool threadpool, DobbyEventBus eventBus) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(threadpool);
        Preconditions.checkNotNull(eventBus);
        this.context = context.getApplicationContext();
        this.threadpool = threadpool;
        this.eventBus = eventBus;
        this.connectivityManager = connectivityManager;
        wifiConnectivityMode = WifiConnectivityMode.UNKNOWN;

        // Make sure we're running on Honeycomb or higher to use ActionBar APIs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            this.connectivityManager.registerDefaultNetworkCallback(new ConnectivityAnalyzerNetworkCallback());
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

    public static String connectivityModeToString(@WifiConnectivityMode int mode) {
        switch (mode) {
            case CONNECTED_AND_ONLINE:
                return "CONNECTED_AND_ONLINE";
            case CONNECTED_AND_CAPTIVE_PORTAL:
                return "CONNECTED_AND_CAPTIVE_PORTAL";
            case CONNECTED_AND_OFFLINE:
                return "CONNECTED_AND_OFFLINE";
            case CONNECTED_AND_UNKNOWN:
                return "CONNECTED_AND_UNKNOWN";
            case ON_AND_DISCONNECTED:
                return "ON_AND_DISCONNECTED";
            case OFF:
                return "OFF";
            default:
                return "Unknown";
        }
    }

    private boolean testUrlForCaptivePortal(String urlString) throws IOException {
        HttpURLConnection urlConnection = null;
        URL url = new URL(URL_FOR_CONNECTIVITY_AND_PORTAL_TEST);
        urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setInstanceFollowRedirects(false);
        urlConnection.setConnectTimeout(PORTAL_TEST_CONNECTION_TIMEOUT_MS);
        urlConnection.setReadTimeout(PORTAL_TEST_READ_TIMEOUT_MS);
        urlConnection.setUseCaches(false);
        urlConnection.getInputStream();
        // We got a valid response, but not from the real google
        return urlConnection.getResponseCode() != 204;
    }

    private boolean isWalledGardenConnection() {
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(URL_FOR_CONNECTIVITY_AND_PORTAL_TEST);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setInstanceFollowRedirects(false);
            urlConnection.setConnectTimeout(PORTAL_TEST_CONNECTION_TIMEOUT_MS);
            urlConnection.setReadTimeout(PORTAL_TEST_READ_TIMEOUT_MS);
            urlConnection.setUseCaches(false);
            urlConnection.getInputStream();
            // We got a valid response, but not from the real google
            return urlConnection.getResponseCode() != 204;
        } catch (IOException e) {
            DobbyLog.v("Walled garden check - probably not a portal: exception " + e);
            return false;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
    }


    @WifiConnectivityMode
    public int performConnectivityAndPortalTest(NetworkInfo activeNetwork) {
        @WifiConnectivityMode int mode = WifiConnectivityMode.UNKNOWN;
        DobbyLog.v("CA: In performConnectivityAndPortalTest");
        if (activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI && activeNetwork.isConnected()) {
            try {
                DobbyLog.v("CA: Starting portal test");
                Utils.getDataFromUrlWithOptions(
                        URL_FOR_CONNECTIVITY_AND_PORTAL_TEST,
                        MAX_STRING_LENGTH_TO_FETCH,
                        PORTAL_TEST_READ_TIMEOUT_MS,
                        PORTAL_TEST_CONNECTION_TIMEOUT_MS,
                        false, //No redirects
                        false); //No cache
                //This returned 200 which means we are in captive portal
                DobbyLog.v("CA: Setting wifi connectivity mode to CONNECTED_AND_CAPTIVE_PORTAL from wifiConnectivityMode: " + connectivityModeToString(getWifiConnectivityMode()));
                mode = WifiConnectivityMode.CONNECTED_AND_CAPTIVE_PORTAL;
                DobbyLog.v("CA: Finishing portal test");
            } catch (Utils.HTTPReturnCodeException e) {
                DobbyLog.v("CA: In perform portal test. Wanted 200, Got return code " + e.httpReturnCode);
                if (e.httpReturnCode == HttpURLConnection.HTTP_NO_CONTENT) {
                    //This is working, we can return as online
                    DobbyLog.v("CA: Setting wifi connectivity mode to CONNECTED_AND_ONLINE from wifiConnectivityMode: " + connectivityModeToString(getWifiConnectivityMode()));
                    mode = WifiConnectivityMode.CONNECTED_AND_ONLINE;
                } else if (e.httpReturnCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                    //This is a captive portal
                    DobbyLog.v("CA: Setting wifi connectivity mode to CONNECTED_AND_CAPTIVE_PORTAL from wifiConnectivityMode: " + connectivityModeToString(getWifiConnectivityMode()));
                    mode = WifiConnectivityMode.CONNECTED_AND_CAPTIVE_PORTAL;
                } else {
                    DobbyLog.v("CA: Got Third return code " + e.httpReturnCode);
                }
            } catch (IOException e) {
                DobbyLog.v("Unable to fetch: " + URL_FOR_CONNECTIVITY_AND_PORTAL_TEST + " Except: " + e);
                if (!isWifiInCaptivePortal()) {
                    DobbyLog.v("CA: Setting wifi connectivity mode to CONNECTED_AND_OFFLINE from wifiConnectivityMode: " + connectivityModeToString(getWifiConnectivityMode()));
                    mode = WifiConnectivityMode.CONNECTED_AND_OFFLINE;
                }
            } catch (Exception e) {
                DobbyLog.v("Exception : " + e);
            }
        }
        DobbyLog.v("CA: Returning mode " + connectivityModeToString(mode));
        return mode;
    }

    synchronized private void updateWifiConnectivityMode(final int scheduleCount) {
        @WifiConnectivityMode int currentWifiMode = WifiConnectivityMode.UNKNOWN;

        if (isWifiOnline()) {
            DobbyLog.v("CA In updateWifiConnectivityMode with scheduleCount " +
                    scheduleCount + " but returning since already Online");
            return;
        }

        DobbyLog.v("Update wifi connectivity mode, scheduleCount =" + scheduleCount);

        final NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();

        // Reschedule network check if requested to do so and we have a null NetworkInfo.
        if (scheduleCount > 0 && activeNetwork == null) {
            rescheduleConnectivityTest(scheduleCount - 1);
            return;
        }

        DobbyLog.v("active network is not null, activeNetwork:" + activeNetwork.toString());
        try {
            currentWifiMode = performConnectivityAndPortalTest(activeNetwork);
        } catch (IllegalStateException e) {
            DobbyLog.v("Exception while checking connectivity: " + e);
        }

        //Set the wifiConnectivityMode
        if (currentWifiMode != WifiConnectivityMode.UNKNOWN) {
            wifiConnectivityMode = currentWifiMode;
        }
        if (isWifiOnline()) {
            eventBus.postEvent(new DobbyEvent(DobbyEvent.EventType.WIFI_INTERNET_CONNECTIVITY_ONLINE));
        } else if (isWifiInCaptivePortal()) {
            eventBus.postEvent(new DobbyEvent(DobbyEvent.EventType.WIFI_INTERNET_CONNECTIVITY_CAPTIVE_PORTAL));
        } else if (isWifiDisconnected()){

        } else {
            eventBus.postEvent(new DobbyEvent(DobbyEvent.EventType.WIFI_INTERNET_CONNECTIVITY_OFFLINE));
        }

        //TODO: Move this to the end.
        if (scheduleCount > 0 && !isWifiOnline()) {
            rescheduleConnectivityTest(0);  // Try once more before quitting.
        }
    }

    public void rescheduleConnectivityTest(final int scheduleCount) {
        threadpool.getExecutorServiceForNetworkLayer().schedule(new Runnable() {
            @Override
            public void run() {
                DobbyLog.v("Scheduled");
                updateWifiConnectivityMode(scheduleCount);
            }
        }, GAP_BETWEEN_CONNECTIIVITY_CHECKS_MS, TimeUnit.MILLISECONDS);
    }

    @ConnectivityAnalyzer.WifiConnectivityMode
    synchronized public int getWifiConnectivityMode() {
        return wifiConnectivityMode;
    }

    public void setWifiConnectivityMode(@WifiConnectivityMode int mode) {
        wifiConnectivityMode = mode;
    }

    synchronized public boolean isWifiOnline() {
        return (wifiConnectivityMode == CONNECTED_AND_ONLINE);
    }

    synchronized public boolean isWifiInCaptivePortal() {
        return (wifiConnectivityMode == WifiConnectivityMode.CONNECTED_AND_CAPTIVE_PORTAL);
    }

    synchronized public boolean isWifiDisconnected() {
        return (wifiConnectivityMode == WifiConnectivityMode.OFF ||
                wifiConnectivityMode == WifiConnectivityMode.ON_AND_DISCONNECTED) ;
    }


    public void processDobbyBusEvents(DobbyEvent event) {
        int eventType = event.getEventType();
        DobbyLog.v("ConnectivityAnalyzer Got event: " + event);
        boolean scheduleWifiOnlineTest = false;
        switch(eventType) {
            case DobbyEvent.EventType.WIFI_NOT_CONNECTED:
                if (wifiConnectivityMode != WifiConnectivityMode.OFF) {
                    DobbyLog.v("CA: Setting wifi connectivity mode to ON_AND_DISCONNECTED from wifiConnectivityMode: " + connectivityModeToString(getWifiConnectivityMode()));
                    wifiConnectivityMode = WifiConnectivityMode.ON_AND_DISCONNECTED;
                }
                break;

            case DobbyEvent.EventType.WIFI_STATE_DISABLED:
            case DobbyEvent.EventType.WIFI_STATE_DISABLING:
                DobbyLog.v("CA: Setting wifi connectivity mode to OFF from wifiConnectivityMode: " + connectivityModeToString(getWifiConnectivityMode()));
                wifiConnectivityMode = WifiConnectivityMode.OFF;
                break;

            case DobbyEvent.EventType.WIFI_STATE_UNKNOWN:
                DobbyLog.v("CA: Got event WIFI_STATE_UNKNOWN, ignoring");
                //wifiConnectivityMode = WifiConnectivityMode.UNKNOWN;
                break;

            case DobbyEvent.EventType.WIFI_CONNECTED:
                //Should change mode to CONNECTED AND OFFLINE -- will transition to CONNECTED AND ONLINE OR CAPTIVE AFTER TEST
                wifiConnectivityMode = WifiConnectivityMode.CONNECTED_AND_UNKNOWN;
                scheduleWifiOnlineTest=true;
                break;

            case DobbyEvent.EventType.WIFI_STATE_ENABLING:
            case DobbyEvent.EventType.WIFI_STATE_ENABLED:
                DobbyLog.v("CA: Setting wifi connectivity mode to ON_AND_DISCONNECTED from wifiConnectivityMode: " + connectivityModeToString(getWifiConnectivityMode()));
                wifiConnectivityMode = WifiConnectivityMode.ON_AND_DISCONNECTED;
                scheduleWifiOnlineTest=true;
                break;

            case DobbyEvent.EventType.WIFI_RSSI_CHANGED:
            case DobbyEvent.EventType.DHCP_INFO_AVAILABLE:
            case DobbyEvent.EventType.PING_INFO_AVAILABLE:
                scheduleWifiOnlineTest = true;
                break;

        }
        //Safe to call since it doesn't do anyting if we are already online
        if (scheduleWifiOnlineTest && !isWifiOnline()) {
            threadpool.getExecutorServiceForNetworkLayer().submit(new Runnable() {
                @Override
                public void run() {
                    updateWifiConnectivityMode(0 /* Do not schedule again*/);
                }
            });
        }
        DobbyLog.v("WifiConnectivity Mode is " + wifiConnectivityMode);
    }


    @TargetApi(Build.VERSION_CODES.N)
    private class ConnectivityAnalyzerNetworkCallback extends ConnectivityManager.NetworkCallback {
        //Connection Manager callbacks
        @Override
        public void onAvailable(Network network) {
            DobbyLog.v("Inside onAvailable");
            if (!isWifiOnline()) {
                threadpool.getExecutorServiceForNetworkLayer().submit(new Runnable() {
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
