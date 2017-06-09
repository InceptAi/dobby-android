package com.inceptai.dobby.leaderboard;

/**
 * Created by vivek on 6/7/17.
 */

public class LeaderBoardInfo {
    private int rank;
    private double speed;
    private String handle;

    public LeaderBoardInfo(int rank, double speed, String handle) {
        this.rank = rank;
        this.speed = speed;
        this.handle = handle;
    }

    public LeaderBoardInfo() {}

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

}
