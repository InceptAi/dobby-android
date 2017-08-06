package com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.speedtest;

import com.inceptai.wifimonitoringservice.actionlibrary.utils.ActionLibraryCodes;

import java.util.List;

/**
 * Created by vivek on 8/2/17.
 */

public class BandwidthProgressSnapshot {
    @ActionLibraryCodes.BandwidthTestSnapshotType private int resultType;
    private double bandwidth;
    private long timeStampMs;
    @ActionLibraryCodes.BandwidthTestMode
    private int testMode;
    private SpeedTestConfig speedTestConfig;
    private ServerInformation.ServerDetails bestServerDetails;
    private BandwidthResult finalBandwidthResult;
    private ServerInformation serverInformation;
    private List<ServerInformation.ServerDetails> closestServers;

    public BandwidthProgressSnapshot(double bandwidth, long timeStampMs, @ActionLibraryCodes.BandwidthTestMode int testMode) {
        this.bandwidth = bandwidth;
        this.timeStampMs = timeStampMs;
        this.testMode = testMode;
        this.resultType = ActionLibraryCodes.BandwidthTestSnapshotType.INSTANTANEOUS_BANDWIDTH;
    }

    public BandwidthProgressSnapshot(SpeedTestConfig speedTestConfig) {
        this.speedTestConfig = speedTestConfig;
        this.resultType = ActionLibraryCodes.BandwidthTestSnapshotType.SPEED_TEST_CONFIG;
    }

    public BandwidthProgressSnapshot(ServerInformation.ServerDetails bestServerDetails) {
        this.bestServerDetails = bestServerDetails;
        this.resultType = ActionLibraryCodes.BandwidthTestSnapshotType.BEST_SERVER_DETAILS;
    }

    public BandwidthProgressSnapshot(BandwidthResult finalBandwidthResult) {
        this.finalBandwidthResult = finalBandwidthResult;
        this.resultType = ActionLibraryCodes.BandwidthTestSnapshotType.FINAL_BANDWIDTH;
    }

    public BandwidthProgressSnapshot(ServerInformation serverInformation) {
        this.serverInformation = serverInformation;
        this.resultType = ActionLibraryCodes.BandwidthTestSnapshotType.SERVER_INFORMATION;
    }

    public BandwidthProgressSnapshot(List<ServerInformation.ServerDetails> closestServers) {
        this.closestServers = closestServers;
        this.resultType = ActionLibraryCodes.BandwidthTestSnapshotType.CLOSEST_SERVERS;
    }


}
