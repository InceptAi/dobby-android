package com.inceptai.dobby.speedtest;

import android.support.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import com.inceptai.dobby.utils.DobbyLog;
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
    public static final int MAX_LATENCY_TRY = 3;
    public static final int MAX_STRING_LENGTH = 100;

    private SpeedTestConfig config;
    private ServerInformation info;
    //Results callback
    private ResultsCallback resultsCallback;

    /**
     * Callback interface for results. More methods to follow.
     */
    public interface ResultsCallback {
        void onBestServerSelected(ServerInformation.ServerDetails bestServer);

        void onClosestServersSelected(List<ServerInformation.ServerDetails> closestServers);

        void onBestServerSelectionError(String error);
    }

    public BestServerSelector(SpeedTestConfig config, ServerInformation info,
                              @Nullable ResultsCallback resultsCallback) {
        Preconditions.checkNotNull(config);
        Preconditions.checkNotNull(info);
        this.config = config;
        this.info = info;
        this.resultsCallback = resultsCallback;
    }

    /**
     * Closest set of MAX_CLOSEST_SERVERS
     *
     * @param config
     * @param info
     * @return
     */
    public List<ServerInformation.ServerDetails> getClosestServers(SpeedTestConfig config, ServerInformation info) {
        final int MAX_CLOSEST_SERVERS = 10; //taken from speedtest-cli
        List<ServerInformation.ServerDetails> closeList = new ArrayList<ServerInformation.ServerDetails>();
        //Iterate over all servers and compute distance, ignore the ones in ignore ids
        for (ServerInformation.ServerDetails detailInfo : info.serverList) {
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
                return (int) (d1.distance - d2.distance);
            }
        });
        //Take the top MAX_CLOSEST_SERVERS
        int closeServerLength = Math.min(MAX_CLOSEST_SERVERS, info.serverList.size());
        closeList = info.serverList.subList(0, closeServerLength);
        if (this.resultsCallback != null) {
            this.resultsCallback.onClosestServersSelected(closeList);
        }
        return closeList;
    }

    public ServerInformation.ServerDetails getBestServerFromClosestServers(List<ServerInformation.ServerDetails> closeServerList) {
        double bestLatencyMs = 60000;
        ServerInformation.ServerDetails bestServer = null;
        for (ServerInformation.ServerDetails detailInfo : closeServerList) {
            String latencyUrl = detailInfo.url + "/latencyMs.txt";
            //Get url/latencyMs.txt 3 time and average
            double[] latencyMeasurementsMs = {60000, 60000, 60000};
            for (int i = 0; i < MAX_LATENCY_TRY; i++) {
                String dataFromUrl = null;
                try {
                    long startTime = System.currentTimeMillis();
                    dataFromUrl = Utils.getDataFromUrl(latencyUrl, MAX_STRING_LENGTH);
                    if (dataFromUrl.equals("size=500")) {
                        latencyMeasurementsMs[i] = (System.currentTimeMillis() - startTime);
                    }
                } catch (IOException e) {
                    String errorString = "Exception while performing latencyMs test: " + e;
                    if (this.resultsCallback != null) {
                        this.resultsCallback.onBestServerSelectionError(errorString);
                    }
                    DobbyLog.v(errorString);
                }
            }
            try {
                detailInfo.latencyMs = Utils.computeAverage(latencyMeasurementsMs);
                if (detailInfo.latencyMs < bestLatencyMs) {
                    bestLatencyMs = detailInfo.latencyMs;
                    bestServer = detailInfo;
                }
            } catch (InvalidParameterException e) {
                String errorString = "Got exception while computing average: " + e;
                if (this.resultsCallback != null) {
                    this.resultsCallback.onBestServerSelectionError(errorString);
                }
                DobbyLog.v(errorString);
            }
        }
        if (this.resultsCallback != null) {
            this.resultsCallback.onBestServerSelected(bestServer);
        }
        return bestServer;
    }

    public ServerInformation.ServerDetails getBestServer() {
        return getBestServerFromClosestServers(getClosestServers(this.config, this.info));
    }
}
