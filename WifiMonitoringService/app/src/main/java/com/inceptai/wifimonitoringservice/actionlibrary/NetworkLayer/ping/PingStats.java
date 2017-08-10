package com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ping;

import android.support.annotation.IntDef;

import com.google.gson.Gson;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by vivek on 4/8/17.
 */

public class PingStats {
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({PingErrorCode.NO_ERROR,
            PingErrorCode.INVALID_HOST,
            PingErrorCode.COMMAND_NOT_FOUND,
            PingErrorCode.ERROR_PARSING_OUTPUT,
            PingErrorCode.UNINITIALIZED,
            PingErrorCode.NOT_CONNECTED,
            PingErrorCode.UNKNOWN})
    public @interface PingErrorCode {
        int NO_ERROR = 0;
        int INVALID_HOST = 1;
        int COMMAND_NOT_FOUND = 2;
        int ERROR_PARSING_OUTPUT = 3;
        int UNINITIALIZED = 4;
        int NOT_CONNECTED = 5;
        int UNKNOWN = 6;
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({IPAddressType.UNKNOWN,
            IPAddressType.PRIMARY_IP,
            IPAddressType.GATEWAY,
            IPAddressType.PRIMARY_DNS,
            IPAddressType.EXTERNAL_SERVER,
            IPAddressType.EXTERNAL_DNS,
            IPAddressType.UNINITIALIZED})
    public @interface IPAddressType {
        int UNKNOWN = 0;
        int PRIMARY_IP = 1;
        int GATEWAY = 2;
        int PRIMARY_DNS = 3;
        int EXTERNAL_SERVER = 4;
        int EXTERNAL_DNS = 5;
        int UNINITIALIZED = 6;
    }

    public String ipAddress;
    public double minLatencyMs;
    public double maxLatencyMs;
    public double avgLatencyMs;
    public double deviationMs;
    public double lossRatePercent;
    public long updatedAt;
    @PingErrorCode public int errorCode;
    @PingStats.IPAddressType
    public int ipAddressType;


    public PingStats(String ipAddress) {
        this.ipAddress = ipAddress;
        lossRatePercent = -1;
        errorCode = PingErrorCode.UNINITIALIZED;
        ipAddressType = IPAddressType.UNINITIALIZED;
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
