package com.inceptai.dobby.ping;

import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.util.Log;

import com.inceptai.dobby.model.PingStats;
import com.inceptai.dobby.utils.Utils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.inceptai.dobby.DobbyApplication.TAG;
import static com.inceptai.dobby.ping.PingAction.PingErrorCode.PING_EXCEPTION_COMMAND_NOT_FOUND;
import static com.inceptai.dobby.ping.PingAction.PingErrorCode.PING_EXCEPTION_INVALID_HOST;
import static com.inceptai.dobby.ping.PingAction.PingErrorCode.PING_EXCEPTION_PARSING_OUTPUT;
import static com.inceptai.dobby.utils.Utils.EMPTY_STRING;
import static com.inceptai.dobby.utils.Utils.runSystemCommand;

/**
 * Created by vivek on 4/2/17.
 */

public class PingAction {
    private static int defaultTimeOut = 3;
    private static int defaultNumberOfPings = 3;
    private static int defaultPacketSize = 1200;
    private ResultsCallback resultsCallback;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PING_EXCEPTION_INVALID_HOST,
            PING_EXCEPTION_COMMAND_NOT_FOUND,
            PING_EXCEPTION_PARSING_OUTPUT})
    public @interface PingErrorCode {
        int PING_EXCEPTION_INVALID_HOST = 1;
        int PING_EXCEPTION_COMMAND_NOT_FOUND = 2;
        int PING_EXCEPTION_PARSING_OUTPUT = 3;
    }

    /**
     * Callback interface for results. More methods to follow.
     */
    public interface ResultsCallback {
        void onFinish(HashMap<String, PingStats> pingStatsHashMap);
        void onError(@PingErrorCode int errorType, String[] addressList, String errorMessage);
    }

    private PingAction(@Nullable ResultsCallback resultsCallback) {
        this.resultsCallback = resultsCallback;
    }

    /**
     * Factory constructor to create an instance
     * @return Instance of PingAction.
     */
    @Nullable
    public static PingAction create(@Nullable ResultsCallback resultsCallback) {
        return new PingAction(resultsCallback);
    }



    public static class PingActionException extends Exception {
        @PingErrorCode
        private int exceptionType = 0;
        private String addressToPing = EMPTY_STRING;
        public PingActionException(@PingErrorCode  int exceptionType, String addressToPing) {
            this.exceptionType = exceptionType;
            this.addressToPing = addressToPing;
        }

        @Override
        public String toString() {
            switch(exceptionType) {
                case PING_EXCEPTION_INVALID_HOST:
                    return "Invalid host to ping | host = " + addressToPing;
                case PING_EXCEPTION_COMMAND_NOT_FOUND:
                    return "Ping command not found on the device | host = " + addressToPing;
                case PING_EXCEPTION_PARSING_OUTPUT:
                    return "Exception while parsing ping output | host = " + addressToPing;
                default:
                    return "Unknown Ping Error | host = " + addressToPing;
            }
        }
    }


    public static String pingIP(String ipAddress) throws PingActionException {
        return pingIP(ipAddress, defaultTimeOut, defaultNumberOfPings);
    }

    public static String pingIP(String ipAddress, int timeOut, int numberOfPings) throws PingActionException {
        String output = EMPTY_STRING;
        try {
            output = runSystemCommand("ping -t " + timeOut + " -c " + numberOfPings + " " + ipAddress);
        } catch (Exception e) {
            throw new PingActionException(PING_EXCEPTION_COMMAND_NOT_FOUND, ipAddress);
        }
        return output;
    }

    public HashMap<String, PingStats> pingAndReturnStatsList(String[] pingAddressList, int timeOut, int numberOfPings) {
        if (pingAddressList == null) {
            return null;
        }

        HashMap<String, PingStats> pingStatsMap = new HashMap<>();
        try {
            for (int addressIndex = 0; addressIndex < pingAddressList.length; addressIndex++) {
                pingStatsMap.put(pingAddressList[addressIndex],
                        pingAndReturnStats(pingAddressList[addressIndex], timeOut, numberOfPings));
            }
            if (resultsCallback != null) {
                resultsCallback.onFinish(pingStatsMap);
            }
        } catch (PingActionException e) {
            Log.i(TAG, "Exception while pinging: " + e);
            if (resultsCallback != null) {
                resultsCallback.onError(e.exceptionType, pingAddressList,
                        "Exception while pinging: " + e);
            }
        }  catch (Exception e) {
            Log.i(TAG, "Exception while pinging: " + e);
            if (resultsCallback != null) {
                //TODO: Return meaningful error code here.
                resultsCallback.onError(PingErrorCode.PING_EXCEPTION_PARSING_OUTPUT,
                        pingAddressList, "Exception while pinging: " + e);
            }
        }
        return pingStatsMap;
    }

    public HashMap<String, PingStats> pingAndReturnStatsList(String[] pingAddressList) {
        return pingAndReturnStatsList(pingAddressList, defaultTimeOut, defaultNumberOfPings);
    }

    public PingStats pingAndReturnStats(String ipAddress, int timeOut, int numberOfPings) throws PingActionException, IndexOutOfBoundsException {
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
        String patternForLatency = "min/avg/max/[a-z]+ = \\d+(\\.\\d+)?/\\d+(\\.\\d+)?/\\d+(\\.\\d+)?/\\d+(\\.\\d+)?";
        String patternForPktLoss = "\\d+(\\.\\d+)?% packet loss";

        //Get pkts stats
        String pingOutput = pingIP(ipAddress, timeOut, numberOfPings);
        Pattern pktsPattern = Pattern.compile(patternForPktLoss);
        Matcher pktsMatcher = pktsPattern.matcher(pingOutput);
        if (pktsMatcher.find()) {
            String[] matchingStrings = pktsMatcher.group(0).split(" ");
            if (matchingStrings.length >= 3) {
                pingStatsToReturn.lossRatePercent = Utils.parseDoubleWithDefault(-1, matchingStrings[0].split("%")[0]);
            }
        }

        Pattern latencyPattern = Pattern.compile(patternForLatency);
        Matcher latencyMatcher = latencyPattern.matcher(pingOutput);
        if (latencyMatcher.find()) {
            String[] matchingStrings = latencyMatcher.group(0).split(" ");
            if (matchingStrings.length >= 3) {
                String[] latencies = matchingStrings[2].split("/");
                if (latencies.length >= 4) {
                    pingStatsToReturn.minLatencyMs = Utils.parseDoubleWithDefault(-1.0, latencies[0]);
                    pingStatsToReturn.avgLatencyMs = Utils.parseDoubleWithDefault(-1.0, latencies[1]);
                    pingStatsToReturn.maxLatencyMs = Utils.parseDoubleWithDefault(-1.0, latencies[2]);
                    pingStatsToReturn.deviationMs = Utils.parseDoubleWithDefault(-1.0, latencies[3]);
                }
            }
        }
        return pingStatsToReturn;
    }

}