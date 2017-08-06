package com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ping;

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


    public PingStats(String ipAddress) {
        this.ipAddress = ipAddress;
        this.minLatencyMs = 0;
        this.maxLatencyMs = 0;
        this.avgLatencyMs = 0;
        this.deviationMs = 0;
        this.lossRatePercent = -1;
        this.updatedAt = 0;
    }

    private String toJson() {
        Gson gson = new Gson();
        String json = gson.toJson(this);
        return json;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return toJson();
    }
}
