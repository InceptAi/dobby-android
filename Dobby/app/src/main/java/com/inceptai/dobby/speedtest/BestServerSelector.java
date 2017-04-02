package com.inceptai.dobby.speedtest;

import com.google.common.primitives.Ints;
import com.inceptai.dobby.utils.Utils;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by vivek on 4/1/17.
 */

public class BestServerSelector {

    /**
     * Closest set of MAX_CLOSEST_SERVERS
     * @param config
     * @param info
     * @return
     */
    public static List<ServerInformation.ServerDetails> getClosestServers(SpeedTestConfig config, ServerInformation info) {
        final int MAX_CLOSEST_SERVERS = 10; //taken from speedtest-cli
        List<ServerInformation.ServerDetails> closeList = new ArrayList<ServerInformation.ServerDetails>();
        //Iterate over all servers and compute distance, ignore the ones in ignore ids
        for (ServerInformation.ServerDetails detailInfo: info.serverList) {
            if (Ints.contains(config.serverConfig.ignoreIds, detailInfo.serverId)) {
                continue;
            }
            detailInfo.distance = Utils.computeDistance(detailInfo.lat, detailInfo.lon,
                    config.clientConfig.lat, config.clientConfig.lon);
        }
        //Sort the servers
        Collections.sort(info.serverList, new Comparator<ServerInformation.ServerDetails>() {
            @Override
            public int compare(ServerInformation.ServerDetails d1, ServerInformation.ServerDetails d2) {
                return (int)(d1.distance - d2.distance);
            }
        });
        //Take the top MAX_CLOSEST_SERVERS
        int closeServerLength = Math.min(MAX_CLOSEST_SERVERS, info.serverList.size());
        closeList = info.serverList.subList(0, closeServerLength);
        return closeList;
    }

    public static ServerInformation.ServerDetails getBestServerId(List<ServerInformation.ServerDetails> closeServerList) {
        final int MAX_LATENCY_TRY = 3;
        final int MAX_STRING_LENGTH = 100;
        double bestLatency = 60000;
        ServerInformation.ServerDetails bestServer = null;
        for (ServerInformation.ServerDetails detailInfo: closeServerList) {
            String latencyUrl = detailInfo.url.toString() + "/latency.txt";
            //Get url/latency.txt 3 time and average
            double[] latency = {60000, 60000, 60000};
            for (int i=0; i < MAX_LATENCY_TRY; i++) {
                String dataFromUrl = null;
                try {
                    long startTime = System.currentTimeMillis();
                    dataFromUrl = Utils.getDataFromUrl(latencyUrl, MAX_STRING_LENGTH);
                    if (dataFromUrl.equals("size=500")) {
                        latency[i] =  (System.currentTimeMillis() - startTime);
                    }
                } catch (IOException e) {
                    System.out.println("Exception while performing latency test: " + e);
                }
            }
            try {
                detailInfo.latency = Utils.computeAverage(latency);
                if (detailInfo.latency < bestLatency) {
                    bestLatency = detailInfo.latency;
                    bestServer = detailInfo;
                }
            } catch (InvalidParameterException e) {
                System.out.println("Got exception while computing average: " + e);
            }
        }
        return bestServer;
    }

}
