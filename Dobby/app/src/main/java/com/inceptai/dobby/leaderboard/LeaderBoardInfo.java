package com.inceptai.dobby.leaderboard;

/**
 * Created by vivek on 6/7/17.
 */

public class LeaderBoardInfo {
    private int rank;
    private double speed;
    private String userHandle;

    public LeaderBoardInfo(int rank, double speed, String userHandle) {
        this.rank = rank;
        this.speed = speed;
        this.userHandle = userHandle;
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

    public String getUserHandle() {
        return userHandle;
    }

    public void setUserHandle(String userHandle) {
        this.userHandle = userHandle;
    }

}
