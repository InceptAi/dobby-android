package com.inceptai.dobby.speedtest;

import android.util.Log;

import com.inceptai.dobby.utils.Utils;

import java.security.InvalidParameterException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.inceptai.dobby.utils.Utils.runSystemCommand;
import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * Created by vivek on 4/2/17.
 */

public class PingAnalyzer {
    private static int defaultTimeOut = 3;
    private static int defaultNumberOfPings = 3;
    private static int defaultPacketSize = 1200;

    public class PingStats {
        double minLatency;
        double maxLatency;
        double avgLatency;
        double deviation;
        double lossRate;


        public PingStats(double minLatency, double maxLatency, double avgLatency,
                         double deviation, double lossRate) {
            this.minLatency = minLatency;
            this.avgLatency = avgLatency;
            this.maxLatency = maxLatency;
            this.deviation = deviation;
            this.lossRate = lossRate;
        }

        public PingStats() {
            this.minLatency = -1;
            this.maxLatency = -1;
            this.avgLatency = -1;
            this.deviation = -1;
            this.lossRate = -1;
        }
    }
    public String pingIP(String ipAddress) {
        return pingIP(ipAddress, defaultTimeOut, defaultNumberOfPings);
    }

    public String pingIP(String ipAddress, int timeOut, int numberOfPings) {
        return runSystemCommand("ping -t " + timeOut + " -c " + numberOfPings + " " + ipAddress);
    }

    public String pingWirelessRouter() {
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
        //String line = "round-trip min/avg/max/stddev = 1.847/2.557/3.634/0.774 ms";
        PingStats pingStatsToReturn = new PingStats();
        String patternForLatency = "min/avg/max/[a-z]+ = \\d+(\\.\\d+)?/\\d+(\\.\\d+)?/\\d+(\\.\\d+)?/\\d+(\\.\\d+)?";
        String patternForPktLoss = "\\d+(\\.\\d+)?% packet loss";

        String pingOutput = pingIP(ipAddress, timeOut, numberOfPings);


        //Get pkts stats
        try {
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
        } catch (IndexOutOfBoundsException|InvalidParameterException|PatternSyntaxException e) {
            Log.i(TAG, "Exception while parsing ping output: " + e);
        }

        return pingStatsToReturn;
    }

}