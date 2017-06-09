package com.inceptai.dobby.database;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.leaderboard.LeaderBoardInfo;
import com.inceptai.dobby.utils.DobbyLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by vivek on 6/9/17.
 */

@Singleton
public class LeaderBoardInfoFetcher {
    private static final String LEADERBOARD_DB_ROOT = "leaderboard";
    private DatabaseReference mDatabase;
    private ExecutorService executorService;
    protected SettableFuture<List<LeaderBoardInfo>> leaderBoardFetchFuture;


    @Inject
    public LeaderBoardInfoFetcher(DobbyThreadpool dobbyThreadpool) {
        mDatabase = FirebaseDatabase.getInstance().getReference(LEADERBOARD_DB_ROOT);
        executorService = dobbyThreadpool.getExecutorService();
    }

    private void setLeaderBoardFetchFuture(List<LeaderBoardInfo> leaderBoardInfoList) {
        if (leaderBoardFetchFuture != null) {
            boolean setResult = leaderBoardFetchFuture.set(leaderBoardInfoList);
            DobbyLog.v("Setting leaderBoard future: return value: " + setResult);
        }
    }

    public ListenableFuture<List<LeaderBoardInfo>> fetchLeaderBoardInfo(final int maxLeadersToFetch) {
        leaderBoardFetchFuture = SettableFuture.create();
        mDatabase.orderByKey().limitToLast(maxLeadersToFetch).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final List<LeaderBoardInfo> leaderBoardInfoList = new ArrayList<>();
                for (DataSnapshot leaderBoardSnapShot : dataSnapshot.getChildren()) {
                    LeaderBoardInfo leaderBoardInfo = leaderBoardSnapShot.getValue(LeaderBoardInfo.class);
                    DobbyLog.v("LeaderBoard info " + leaderBoardInfo.getUserHandle()); //log
                    leaderBoardInfoList.add(leaderBoardInfo);
                    //System.out.println(dataSnapshot.getKey() + " was " + leaderBoardInfo.getSpeed() + " fast");
                }
                setLeaderBoardFetchFuture(leaderBoardInfoList);
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        return leaderBoardFetchFuture;
    }
}
