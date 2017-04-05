package com.inceptai.dobby.speedtest;

import android.support.annotation.Nullable;
import android.util.Log;

import com.google.gson.Gson;
import com.inceptai.dobby.utils.Utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.inceptai.dobby.DobbyApplication.TAG;
import static com.inceptai.dobby.utils.Utils.runSystemCommand;

/**
 * Created by vivek on 4/2/17.
 */

public class PingAnalyzer {
    private static int defaultTimeOut = 3;
    private static int defaultNumberOfPings = 3;
    private static int defaultPacketSize = 1200;

    private ResultsCallback resultsCallback;

    /**
     * Callback interface for results. More methods to follow.
     */
    public interface ResultsCallback {
        void onPingResults(PingStats stats);
        void onPingError(String error);
    }


    public PingAnalyzer(@Nullable ResultsCallback resultsCallback) {
        this.resultsCallback = resultsCallback;
    }

    /**
     * Factory constructor to create an instance
     * @return Instance of PingAnalyzer.
     */
    @Nullable
    public static PingAnalyzer create(ResultsCallback resultsCallback) {
        return new PingAnalyzer(resultsCallback);
    }


    public class PingStats {
        String ipAddress;
        double minLatency;
        double maxLatency;
        double avgLatency;
        double deviation;
        double lossRate;


        public PingStats(String ipAddress,double minLatency,
                         double maxLatency, double avgLatency,
                         double deviation, double lossRate) {
            this.ipAddress = ipAddress;
            this.minLatency = minLatency;
            this.avgLatency = avgLatency;
            this.maxLatency = maxLatency;
            this.deviation = deviation;
            this.lossRate = lossRate;
        }

        public PingStats(String ipAddress) {
            this.ipAddress = ipAddress;
            this.minLatency = -1;
            this.maxLatency = -1;
            this.avgLatency = -1;
            this.deviation = -1;
            this.lossRate = -1;
        }

        public String toJson() {
            Gson gson = new Gson();
            String json = gson.toJson(this);
            return json;
        }
    }
    public String pingIP(String ipAddress) throws Exception {
        return pingIP(ipAddress, defaultTimeOut, defaultNumberOfPings);
    }

    public String pingIP(String ipAddress, int timeOut, int numberOfPings) throws Exception {
        return runSystemCommand("ping -t " + timeOut + " -c " + numberOfPings + " " + ipAddress);
    }

    public String pingWirelessRouter() throws Exception {
        return runSystemCommand("ping -t " + defaultTimeOut + " -c " + defaultNumberOfPings + " 192.168.1.1");
    }

    public PingStats pingAndReturnStats(String ipAddress) {
        return pingAndReturnStats(ipAddress, defaultTimeOut, defaultNumberOfPings);
    }

    public PingStats pingAndReturnStats(String ipAddress, int timeOut, int numberOfPings) {
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
        try {
            String pingOutput = pingIP(ipAddress, timeOut, numberOfPings);
            Pattern pktsPattern = Pattern.compile(patternForPktLoss);
            Matcher pktsMatcher = pktsPattern.matcher(pingOutput);
            if (pktsMatcher.find()) {
                String[] matchingStrings = pktsMatcher.group(0).split(" ");
                if (matchingStrings.length >= 3) {
                    pingStatsToReturn.lossRate = Utils.parseDoubleWithDefault(-1, matchingStrings[0].split("%")[0]);
                }
            }

            Pattern latencyPattern = Pattern.compile(patternForLatency);
            Matcher latencyMatcher = latencyPattern.matcher(pingOutput);
            if (latencyMatcher.find()) {
                String[] matchingStrings = latencyMatcher.group(0).split(" ");
                if (matchingStrings.length >= 3) {
                    String[] latencies = matchingStrings[2].split("/");
                    if (latencies.length >= 4) {
                        pingStatsToReturn.minLatency = Utils.parseDoubleWithDefault(-1.0, latencies[0]);
                        pingStatsToReturn.avgLatency = Utils.parseDoubleWithDefault(-1.0, latencies[1]);
                        pingStatsToReturn.maxLatency = Utils.parseDoubleWithDefault(-1.0, latencies[2]);
                        pingStatsToReturn.deviation = Utils.parseDoubleWithDefault(-1.0, latencies[3]);
                    }
                }
            }
        } catch (Exception e) {
            Log.i(TAG, "Exception while parsing ping output: " + e);
            if (resultsCallback != null) {
                resultsCallback.onPingError("Exception while parsing ping output: " + e);
            }
        }
        if (resultsCallback != null) {
            resultsCallback.onPingResults(pingStatsToReturn);
        }
        return pingStatsToReturn;
    }

}