package com.inceptai.actionlibrary.NetworkLayer;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.inceptai.actionlibrary.ActionThreadPool;
import com.inceptai.actionlibrary.utils.ActionLog;
import com.inceptai.actionlibrary.utils.ActionUtils;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.HttpURLConnection;
import java.util.concurrent.TimeUnit;

import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static com.inceptai.actionlibrary.NetworkLayer.ConnectivityTester.WifiConnectivityMode.CONNECTED_AND_CAPTIVE_PORTAL;
import static com.inceptai.actionlibrary.NetworkLayer.ConnectivityTester.WifiConnectivityMode.CONNECTED_AND_OFFLINE;
import static com.inceptai.actionlibrary.NetworkLayer.ConnectivityTester.WifiConnectivityMode.CONNECTED_AND_ONLINE;
import static com.inceptai.actionlibrary.NetworkLayer.ConnectivityTester.WifiConnectivityMode.CONNECTED_AND_UNKNOWN;
import static com.inceptai.actionlibrary.NetworkLayer.ConnectivityTester.WifiConnectivityMode.MAX_MODES;
import static com.inceptai.actionlibrary.NetworkLayer.ConnectivityTester.WifiConnectivityMode.OFF;
import static com.inceptai.actionlibrary.NetworkLayer.ConnectivityTester.WifiConnectivityMode.ON_AND_DISCONNECTED;
import static com.inceptai.actionlibrary.NetworkLayer.ConnectivityTester.WifiConnectivityMode.UNKNOWN;


/**
 * Created by vivek on 4/12/17.
 */
public class ConnectivityTester {
    private static String URL_FOR_CONNECTIVITY_AND_PORTAL_TEST = "http://clients3.google.com/generate_204";
    private static final int PORTAL_TEST_READ_TIMEOUT_MS = 3000;
    private static final int PORTAL_TEST_CONNECTION_TIMEOUT_MS = 3000;
    private static int GAP_BETWEEN_CONNECTIVITY_CHECKS_MS = 300;
    private static int MAX_RESCHEDULING_CONNECTIVITY_TESTS = 5;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({CONNECTED_AND_ONLINE, CONNECTED_AND_CAPTIVE_PORTAL,
            CONNECTED_AND_OFFLINE, ON_AND_DISCONNECTED,
            OFF, UNKNOWN, CONNECTED_AND_UNKNOWN,
            MAX_MODES})
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

    // Store application context to prevent leaks and crashes from an activity going out of scope.
    protected Context context;
    protected ActionThreadPool threadpool;
    private ConnectivityManager connectivityManager;
    private SettableFuture<Integer> connectivityCheckFuture;
    private ConnectivityAnalyzerWifiNetworkCallback connectivityAnalyzerWifiNetworkCallback;
    private Network wifiNetwork;


    private ConnectivityTester(Context context,
                                 ConnectivityManager connectivityManager,
                                 ActionThreadPool threadpool) {
        Preconditions.checkNotNull(context);
        Preconditions.checkNotNull(threadpool);
        this.context = context.getApplicationContext();
        this.threadpool = threadpool;
        this.connectivityManager = connectivityManager;
        // Make sure we're running on Honeycomb or higher to use ActionBar APIs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            connectivityAnalyzerWifiNetworkCallback = new ConnectivityAnalyzerWifiNetworkCallback();
            NetworkRequest.Builder request = new NetworkRequest.Builder();
            request.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
            this.connectivityManager.registerNetworkCallback(request.build(), connectivityAnalyzerWifiNetworkCallback);
        }
    }

    /**
     * Factory constructor to create an instance
     * @param context Application context.
     * @return Instance of WifiAnalyzer or null on error.
     */
    @Nullable
    public static ConnectivityTester create(Context context, ActionThreadPool actionThreadPool) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            return new ConnectivityTester(context.getApplicationContext(), connectivityManager, actionThreadPool);
        }
        return null;
    }

    ListenableFuture<Integer> connectivityTest(final boolean onlyOnActiveNetwork, final boolean isWifiConnected) {
        if (connectivityCheckFuture != null && !connectivityCheckFuture.isDone()) {
            return connectivityCheckFuture;
        }
        connectivityCheckFuture = SettableFuture.create();
        threadpool.getExecutorService().submit(new Runnable() {
            @Override
            public void run() {
                performConnectivityTest(onlyOnActiveNetwork, isWifiConnected, MAX_RESCHEDULING_CONNECTIVITY_TESTS);
            }
        });
        return connectivityCheckFuture;
    }

    private void performConnectivityTest(boolean onlyOnActiveNetwork,
                                         boolean isWifiConnected,
                                         int rescheduleCount) {
        @WifiConnectivityMode int mode = UNKNOWN;
        ActionLog.v("In ConnectivityTester: performConnectivityTest " + rescheduleCount);
        if (getIsWifiActive()) {
            ActionLog.v("In ConnectivityTester: performConnectivityTest wifi is active " + rescheduleCount);
            mode = testUrl();
        } else if (!onlyOnActiveNetwork && isWifiConnected){
            //Active network is not wifi but we are connected -- split by api levels
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mode = forceTestOverWifiPostLollipop();
            } else {
                mode = forceTestOverWifiPreLollipop();
            }
        }
        if (mode != CONNECTED_AND_ONLINE && rescheduleCount > 0) {
            rescheduleConnectivityTest(onlyOnActiveNetwork, isWifiConnected, rescheduleCount);
            return;
        }
        connectivityCheckFuture.set(mode);
    }

    private void rescheduleConnectivityTest(final boolean onlyOnActiveNetwork,
                                            final boolean isWifiConnected,
                                            final int scheduleCount) {
        threadpool.getExecutorServiceForNetworkLayer().schedule(new Runnable() {
            @Override
            public void run() {
                ActionLog.v("ConnectivityTester: Scheduled " + scheduleCount);
                performConnectivityTest(onlyOnActiveNetwork, isWifiConnected, scheduleCount - 1);
            }
        }, GAP_BETWEEN_CONNECTIVITY_CHECKS_MS, TimeUnit.MILLISECONDS);
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


    @ConnectivityTester.WifiConnectivityMode
    private int testUrl() {
        int mode;
        try {
            int responseCode = ActionUtils.checkUrlWithOptions(
                    URL_FOR_CONNECTIVITY_AND_PORTAL_TEST,
                    PORTAL_TEST_READ_TIMEOUT_MS,
                    PORTAL_TEST_CONNECTION_TIMEOUT_MS,
                    false, //No redirects
                    false);
            if (responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                mode = CONNECTED_AND_ONLINE;
            } else if (responseCode == HttpURLConnection.HTTP_OK) {
                mode = CONNECTED_AND_CAPTIVE_PORTAL;
            } else {
                mode = CONNECTED_AND_OFFLINE;
            }
        } catch (IOException e) {
            ActionLog.v("Exception in connectivity test");
            mode = CONNECTED_AND_OFFLINE;
        }
        return mode;
    }

    @ConnectivityTester.WifiConnectivityMode
    private int forceTestOverWifiPreLollipop() {
        connectivityManager.setNetworkPreference(ConnectivityManager.TYPE_WIFI);
        int mode = testUrl();
        //Remove your preference
        connectivityManager.setNetworkPreference(ConnectivityManager.DEFAULT_NETWORK_PREFERENCE);
        return mode;
    }

    @TargetApi(LOLLIPOP)
    @WifiConnectivityMode
    private int forceTestOverWifiPostLollipop() {
        @WifiConnectivityMode int mode = UNKNOWN;
        if (wifiNetwork == null) {
            return mode;
        }
        try {
            mode = ActionUtils.checkUrlWithOptionsOverNetwork(URL_FOR_CONNECTIVITY_AND_PORTAL_TEST,
                    wifiNetwork, PORTAL_TEST_READ_TIMEOUT_MS, PORTAL_TEST_CONNECTION_TIMEOUT_MS, false, false);
        } catch (IOException e) {
            mode = CONNECTED_AND_OFFLINE;
        }
        return mode;
    }


    private boolean getIsWifiActive() {
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected()) {
            return true;
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private class ConnectivityAnalyzerWifiNetworkCallback extends ConnectivityManager.NetworkCallback {
        //Connection Manager callbacks
        @Override
        public void onAvailable(Network network) {
            wifiNetwork = network;
            ActionLog.v("Inside onAvailable");
        }

        @Override
        public void onLost(Network network) {
            wifiNetwork = null;
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
