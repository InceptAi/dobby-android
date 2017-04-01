package com.inceptai.dobby.speedtest;

import com.google.common.primitives.Ints;
import com.inceptai.dobby.utils.Utils;

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
            public int compare(ServerInformation.ServerDetails o1, ServerInformation.ServerDetails o2) {
                return (int)(o1.distance - o2.distance);
            }
        });
        //Take the top MAX_CLOSEST_SERVERS
        int closeServerLength = Math.min(MAX_CLOSEST_SERVERS, info.serverList.size());
        closeList = info.serverList.subList(0, closeServerLength);
        return closeList;
    }

    public static ServerInformation.ServerDetails getBestServer(Lui)

}
