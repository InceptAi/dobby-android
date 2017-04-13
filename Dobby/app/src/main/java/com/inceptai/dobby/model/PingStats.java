package com.inceptai.dobby.model;

import com.google.gson.Gson;

/**
 * Created by vivek on 4/8/17.
 */

public class PingStats {
    public String ipAddress;
    public double minLatencyMs;
    public double maxLatencyMs;
    public double avgLatencyMs;
    public double deviationMs;
    public double lossRatePercent;
    public long updatedAt;


    public PingStats(String ipAddress, double minLatencyMs,
                     double maxLatencyMs, double avgLatencyMs,
                     double deviationMs, double lossRatePercent,
                     long updatedAt) {
        this.ipAddress = ipAddress;
        this.minLatencyMs = minLatencyMs;
        this.avgLatencyMs = avgLatencyMs;
        this.maxLatencyMs = maxLatencyMs;
        this.deviationMs = deviationMs;
        this.lossRatePercent = lossRatePercent;
        if (updatedAt == 0) {
            updatedAt = System.currentTimeMillis();
        }
    }

    public PingStats(String ipAddress) {
        this.ipAddress = ipAddress;
        this.minLatencyMs = -1;
        this.maxLatencyMs = -1;
        this.avgLatencyMs = -1;
        this.deviationMs = -1;
        this.lossRatePercent = -1;
        this.updatedAt = -1;
    }

    public String toJson() {
        Gson gson = new Gson();
        String json = gson.toJson(this);
        return json;
    }

    @Override
    public String toString() {
        return toJson();
    }
}
