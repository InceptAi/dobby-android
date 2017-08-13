package com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer;

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
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ping.PingStats;
import com.inceptai.wifimonitoringservice.actionlibrary.utils.ActionUtils;
import com.inceptai.wifimonitoringservice.utils.ServiceLog;
import com.inceptai.wifimonitoringservice.utils.Utils;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.os.Build.VERSION_CODES.LOLLIPOP;
import static com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ConnectivityTester.WifiConnectivityMode.CONNECTED_AND_CAPTIVE_PORTAL;
import static com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ConnectivityTester.WifiConnectivityMode.CONNECTED_AND_OFFLINE;
import static com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ConnectivityTester.WifiConnectivityMode.CONNECTED_AND_ONLINE;
import static com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ConnectivityTester.WifiConnectivityMode.CONNECTED_AND_UNKNOWN;
import static com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ConnectivityTester.WifiConnectivityMode.OFF;
import static com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ConnectivityTester.WifiConnectivityMode.ON_AND_DISCONNECTED;
import static com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ping.PingStats.PingErrorCode.COMMAND_NOT_FOUND;
import static com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ping.PingStats.PingErrorCode.ERROR_PARSING_OUTPUT;
import static com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ping.PingStats.PingErrorCode.INVALID_HOST;
import static com.inceptai.wifimonitoringservice.utils.Utils.EMPTY_STRING;


/**
 * Created by vivek on 4/12/17.
 */
public class ConnectivityTester {
    private static String URL_FOR_CONNECTIVITY_AND_PORTAL_TEST = "http://clients3.google.com/generate_204";
    private static final int PORTAL_TEST_READ_TIMEOUT_MS = 3000;
    private static final int PORTAL_TEST_CONNECTION_TIMEOUT_MS = 3000;
    private static int GAP_BETWEEN_CONNECTIVITY_CHECKS_MS = 200;
    private static int MAX_RESCHEDULING_CONNECTIVITY_TESTS = 10;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({CONNECTED_AND_ONLINE, CONNECTED_AND_CAPTIVE_PORTAL,
            CONNECTED_AND_OFFLINE, ON_AND_DISCONNECTED,
            OFF, WifiConnectivityMode.UNKNOWN, CONNECTED_AND_UNKNOWN,
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

    public static String connectivityModeDescription(@WifiConnectivityMode int mode) {
        switch (mode) {
            case CONNECTED_AND_ONLINE:
                return "Connected to WiFi";
            case CONNECTED_AND_CAPTIVE_PORTAL:
                return "Connected to WiFi";
            case CONNECTED_AND_OFFLINE:
                return "Connected to WiFi";
            case CONNECTED_AND_UNKNOWN:
                return "Connected to WiFi";
            case ON_AND_DISCONNECTED:
                return "Disconnected from WiFi";
            case OFF:
                return "WiFi Off";
            default:
                return EMPTY_STRING;
        }
    }

    public static boolean isConnected(@WifiConnectivityMode int mode) {
        return mode == CONNECTED_AND_ONLINE ||
                mode == CONNECTED_AND_OFFLINE ||
                mode == CONNECTED_AND_CAPTIVE_PORTAL ||
                mode == CONNECTED_AND_UNKNOWN;
    }

    public static boolean isOnline(@WifiConnectivityMode int mode) {
        return mode == CONNECTED_AND_ONLINE;
    }

    public static boolean isCaptive(@WifiConnectivityMode int mode) {
        return mode == CONNECTED_AND_CAPTIVE_PORTAL;
    }

    public static boolean isOffline(@WifiConnectivityMode int mode) {
        return mode == CONNECTED_AND_OFFLINE;
    }


    public static boolean isDisconnected(@WifiConnectivityMode int mode) {
        return mode == ON_AND_DISCONNECTED;
    }

    public static boolean isOff(@WifiConnectivityMode int mode) {
        return mode == OFF;
    }

    public static boolean isUnknown(@WifiConnectivityMode int mode) {
        return mode == WifiConnectivityMode.UNKNOWN;
    }

    public static boolean isConnectedButUnknown(@WifiConnectivityMode int mode) {
        return mode == CONNECTED_AND_UNKNOWN;
    }

    public static boolean isEnabled(@WifiConnectivityMode int mode) {
        return mode != OFF && mode != WifiConnectivityMode.UNKNOWN;
    }

    // Store application context to prevent leaks and crashes from an activity going out of scope.
    protected Context context;
    private Executor executor;
    private ScheduledExecutorService scheduledExecutorService;
    private ConnectivityManager connectivityManager;
    private SettableFuture<Integer> connectivityCheckFuture;
    private SettableFuture<HashMap<String, PingStats>> pingTestFuture;
    private HashMap<String, PingStats> pingStatsHashMap;
    private ConnectivityAnalyzerWifiNetworkCallback connectivityAnalyzerWifiNetworkCallback;
    private Network wifiNetwork;


    private ConnectivityTester(Context context,
                               ConnectivityManager connectivityManager,
                               Executor executor,
                               ScheduledExecutorService scheduledExecutorService) {
        Preconditions.checkNotNull(context);
        this.context = context.getApplicationContext();
        this.executor = executor;
        this.scheduledExecutorService = scheduledExecutorService;
        this.connectivityManager = connectivityManager;
        // Make sure we're running on Honeycomb or higher to use ActionBar APIs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            registerNetworkCallback();
        }
    }

    /**
     * Factory constructor to create an instance
     * @param context Application context.
     * @return Instance of WifiAnalyzer or null on error.
     */
    @Nullable
    public static ConnectivityTester create(Context context, Executor executor, ScheduledExecutorService scheduledExecutorService) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            return new ConnectivityTester(context.getApplicationContext(), connectivityManager, executor, scheduledExecutorService);
        }
        return null;
    }

    ListenableFuture<Integer> connectivityTest(final boolean onlyOnActiveNetwork,
                                               final boolean isWifiConnected,
                                               final int numTests,
                                               final long gapBetweenConnectivityChecksMs) {
        if (connectivityCheckFuture != null && !connectivityCheckFuture.isDone()) {
            return connectivityCheckFuture;
        }
        connectivityCheckFuture = SettableFuture.create();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                performConnectivityTest(onlyOnActiveNetwork,
                        isWifiConnected,
                        numTests,
                        gapBetweenConnectivityChecksMs);
            }
        });
        return connectivityCheckFuture;
    }

    ListenableFuture<HashMap<String, PingStats>> pingTest(final List<String> ipAddressList,
                                                          final int numPings,
                                                          final long pingTimeOutMs,
                                                          final boolean isWifiConnected) {
        if (pingTestFuture != null && !pingTestFuture.isDone()) {
            return pingTestFuture;
        }
        pingTestFuture = SettableFuture.create();
        pingStatsHashMap = new HashMap<>();
        for (String ipAddress: ipAddressList) {
            pingStatsHashMap.put(ipAddress, new PingStats(ipAddress));
        }
        performPingTestAsync(ipAddressList, numPings, pingTimeOutMs, isWifiConnected);
        return pingTestFuture;
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


    public void cleanup() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            unRegisterNetworkCallback();
        }
    }

    public HashMap<String, PingStats> getLastPingResults() {
        return pingStatsHashMap;
    }

    //Private
    private void performConnectivityTest(boolean onlyOnActiveNetwork,
                                         boolean isWifiConnected,
                                         int rescheduleCount,
                                         long gapBetweenConnectivityChecks) {
        @WifiConnectivityMode int mode = WifiConnectivityMode.UNKNOWN;
        if (isWifiConnected) {
            mode = WifiConnectivityMode.CONNECTED_AND_UNKNOWN;
        }
        ServiceLog.v("In ConnectivityTester: performConnectivityTest " + rescheduleCount);
        if (getIsWifiActive()) {
            ServiceLog.v("In ConnectivityTester: performConnectivityTest wifi is active " + rescheduleCount);
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
            rescheduleConnectivityTest(onlyOnActiveNetwork, isWifiConnected, rescheduleCount, gapBetweenConnectivityChecks);
            return;
        }
        connectivityCheckFuture.set(mode);
    }

    private void rescheduleConnectivityTest(final boolean onlyOnActiveNetwork,
                                            final boolean isWifiConnected,
                                            final int scheduleCount,
                                            final long gapBetweenConnectivityChecksMs) {
        scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                ServiceLog.v("ConnectivityTester: Scheduled " + scheduleCount);
                performConnectivityTest(onlyOnActiveNetwork, isWifiConnected, scheduleCount - 1, gapBetweenConnectivityChecksMs);
            }
        }, gapBetweenConnectivityChecksMs, TimeUnit.MILLISECONDS);
    }


    @WifiConnectivityMode
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
            } else if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                mode = CONNECTED_AND_CAPTIVE_PORTAL;
            } else {
                mode = CONNECTED_AND_OFFLINE;
            }
        } catch (IOException e) {
            ServiceLog.v("Exception in connectivity test");
            mode = CONNECTED_AND_OFFLINE;
        }
        return mode;
    }

    @WifiConnectivityMode
    private int forceTestOverWifiPreLollipop() {
        @WifiConnectivityMode int mode = WifiConnectivityMode.UNKNOWN;
        try {
            connectivityManager.setNetworkPreference(ConnectivityManager.TYPE_WIFI);
            mode = testUrl();
            //Remove your preference
            connectivityManager.setNetworkPreference(ConnectivityManager.DEFAULT_NETWORK_PREFERENCE);
        } catch (SecurityException e) {
            ServiceLog.e("Security exception in forceTestOverWifiPreLollipop");
        }
        return mode;
    }

    @TargetApi(LOLLIPOP)
    @WifiConnectivityMode
    private int forceTestOverWifiPostLollipop() {
        @WifiConnectivityMode int mode = WifiConnectivityMode.UNKNOWN;
        if (wifiNetwork == null) {
            return mode;
        }
        try {
            int responseCode = ActionUtils.checkUrlWithOptionsOverNetwork(URL_FOR_CONNECTIVITY_AND_PORTAL_TEST,
                    wifiNetwork, PORTAL_TEST_READ_TIMEOUT_MS, PORTAL_TEST_CONNECTION_TIMEOUT_MS, false, false);
            if (responseCode == HttpURLConnection.HTTP_NO_CONTENT) {
                mode = CONNECTED_AND_ONLINE;
            } else if (responseCode == HttpURLConnection.HTTP_OK) {
                mode = CONNECTED_AND_CAPTIVE_PORTAL;
            } else {
                mode = CONNECTED_AND_OFFLINE;
            }
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
            ServiceLog.v("Inside onAvailable");
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

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void registerNetworkCallback() {
        connectivityAnalyzerWifiNetworkCallback = new ConnectivityAnalyzerWifiNetworkCallback();
        NetworkRequest.Builder request = new NetworkRequest.Builder();
        request.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
        connectivityManager.registerNetworkCallback(request.build(), connectivityAnalyzerWifiNetworkCallback);

    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void unRegisterNetworkCallback() {
        connectivityManager.unregisterNetworkCallback(connectivityAnalyzerWifiNetworkCallback);
    }

    //Perform ping test here
    private void performPingTestAsync(List<String> ipAddressList,
                                      final int numPings,
                                      final long pingTimeOutMs,
                                      final boolean isWifiConnected) {
        if (!isWifiConnected) {
            //Can't ping if not connected -- set ping error
            for (PingStats pingStatsInMap : pingStatsHashMap.values()) {
                pingStatsInMap.errorCode = PingStats.PingErrorCode.NOT_CONNECTED;
            }
            pingTestFuture.set(pingStatsHashMap);
        }

        for (final String ipAddress : ipAddressList) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    ServiceLog.v("Ping: Putting ping on thread " + ipAddress);
                    PingStats pingStats = pingAndReturnStatsSync(ipAddress, numPings, pingTimeOutMs, scheduledExecutorService);
                    checkPingTaskCompletion(ipAddress, pingStats);
                    ServiceLog.v("Ping: Done with pinging " + ipAddress);
                }
            });
        }
    }

    private void checkPingTaskCompletion(String ipAddress, PingStats pingStats) {
        pingStatsHashMap.put(ipAddress, pingStats);
        boolean allPingsFinished = true;
        for (PingStats pingStatsInMap : pingStatsHashMap.values()) {
            if (pingStatsInMap.errorCode == PingStats.PingErrorCode.UNINITIALIZED) {
                allPingsFinished = false;
                break;
            }
        }
        if (allPingsFinished) {
            pingTestFuture.set(pingStatsHashMap);
        }
    }


    private PingStats pingAndReturnStatsSync(String ipAddress,
                                             int numberOfPings,
                                             long killTimeOutMs,
                                             ScheduledExecutorService scheduledExecutorService) {
        /*
            PING 192.168.1.1 (192.168.1.1): 56 data bytes
            64 bytes from 192.168.1.1: icmp_seq=0 ttl=64 time=2.191 ms
            64 bytes from 192.168.1.1: icmp_seq=1 ttl=64 time=3.634 ms
            64 bytes from 192.168.1.1: icmp_seq=2 ttl=64 time=1.847 ms

            --- 192.168.1.1 ping statistics ---
            3 packets transmitted, 3 packets received, 0.0% packet loss
            round-trip min/avg/max/stddev = 1.847/2.557/3.634/0.774 ms
        */
        PingStats pingStatsToReturn = new PingStats(ipAddress);
        try {
            String patternForLatency = "min/avg/max/[a-z]+ = \\d+(\\.\\d+)?/\\d+(\\.\\d+)?/\\d+(\\.\\d+)?/\\d+(\\.\\d+)?";
            String patternForPktLoss = "\\d+(\\.\\d+)?% packet loss";

            //Get pkts stats
            String pingOutput = pingIP(ipAddress, numberOfPings, killTimeOutMs, scheduledExecutorService);
            Pattern pktsPattern = Pattern.compile(patternForPktLoss);
            Matcher pktsMatcher = pktsPattern.matcher(pingOutput);
            if (pktsMatcher.find()) {
                String[] matchingStrings = pktsMatcher.group(0).split(" ");
                if (matchingStrings.length >= 3) {
                    pingStatsToReturn.lossRatePercent = Utils.parseDoubleWithDefault(-1.0, matchingStrings[0].split("%")[0]);
                }
            }

            Pattern latencyPattern = Pattern.compile(patternForLatency);
            Matcher latencyMatcher = latencyPattern.matcher(pingOutput);
            if (latencyMatcher.find()) {
                String[] matchingStrings = latencyMatcher.group(0).split(" ");
                if (matchingStrings.length >= 3) {
                    String[] latencies = matchingStrings[2].split("/");
                    if (latencies.length >= 4) {
                        pingStatsToReturn.minLatencyMs = Utils.parseDoubleWithDefault(0, latencies[0]);
                        pingStatsToReturn.avgLatencyMs = Utils.parseDoubleWithDefault(0, latencies[1]);
                        pingStatsToReturn.maxLatencyMs = Utils.parseDoubleWithDefault(0, latencies[2]);
                        pingStatsToReturn.deviationMs = Utils.parseDoubleWithDefault(0, latencies[3]);
                    }
                }
            }
            pingStatsToReturn.updatedAt = System.currentTimeMillis();
            pingStatsToReturn.errorCode = PingStats.PingErrorCode.NO_ERROR;
        } catch (PingActionException pingException) {
            pingStatsToReturn.errorCode = pingException.exceptionErrorCode;
        } catch (IndexOutOfBoundsException indexOutOfBoundsException) {
            pingStatsToReturn.errorCode = ERROR_PARSING_OUTPUT;
        }
        return pingStatsToReturn;
    }


    public static String pingIP(String ipAddress, int numberOfPings, long timeoutMs,
                                ScheduledExecutorService scheduledExecutorService)
            throws PingActionException {
        ServiceLog.v("Starting ping for IP: " + ipAddress);
        String output = EMPTY_STRING;
        String command = "ping -s 1200 -c " + numberOfPings + " " + ipAddress;
        try {
            output = Utils.runSystemCommand(command, scheduledExecutorService, timeoutMs);
        } catch (Exception e) {
            throw new PingActionException(COMMAND_NOT_FOUND, ipAddress);
        }
        ServiceLog.v("Ending ping for IP: " + ipAddress);
        return output;
    }

    private static class PingActionException extends Exception {
        @PingStats.PingErrorCode private int exceptionErrorCode = 0;
        private String addressToPing = EMPTY_STRING;
        public PingActionException(@PingStats.PingErrorCode int exceptionErrorCode, String addressToPing) {
            this.exceptionErrorCode = exceptionErrorCode;
            this.addressToPing = addressToPing;
        }

        @Override
        public String toString() {
            switch(exceptionErrorCode) {
                case INVALID_HOST:
                    return "Invalid host to ping | host = " + addressToPing;
                case COMMAND_NOT_FOUND:
                    return "Ping command not found on the device | host = " + addressToPing;
                case ERROR_PARSING_OUTPUT:
                    return "Exception while parsing ping output | host = " + addressToPing;
                default:
                    return "Unknown Ping Error | host = " + addressToPing;
            }
        }
    }

}
