package com.inceptai.dobby.leaderboard;

import com.inceptai.dobby.utils.Utils;

/**
 * Created by vivek on 6/7/17.
 */

public class LeaderBoardInfo {
    private int rank;
    private double speed;
    private String handle;
    private String country;
    private String device;

    public LeaderBoardInfo(int rank, double speed, String handle) {
        this(rank, speed, handle, Utils.EMPTY_STRING, Utils.EMPTY_STRING);
    }

    public LeaderBoardInfo(int rank, double speed, String handle, String country, String device) {
        this.rank = rank;
        this.speed = speed;
        this.handle = handle;
        this.country = country;
        this.device = device;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public String getHandle() {
        return handle;
    }

    public void setHandle(String handle) {
        this.handle = handle;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }
}
