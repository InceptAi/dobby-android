package com.inceptai.dobby.ui;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.R;
import com.inceptai.dobby.database.LeaderBoardInfoFetcher;
import com.inceptai.dobby.leaderboard.LeaderBoardInfo;
import com.inceptai.dobby.utils.DobbyLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import javax.inject.Inject;

/**
 * A fragment representing a list of wifi aps and their signal strengths.
 */
public class WifiLeaderBoardFragment extends Fragment implements Handler.Callback {
//public class WifiLeaderBoardFragment extends Fragment {
    public static final String FRAGMENT_TAG = "WifiLeaderBoardFragment";
    private static final int MSG_UPDATE_LEADERBOARD = 101;
    private static final int MAX_USERS_IN_LEADERBOARD = 10;

    private RecyclerView recyclerView;
    private WifiLeaderBoardRecyclerViewAdapter wifiLeaderBoardRecyclerViewAdapter;
    private Handler handler;
    private List<LeaderBoardInfo> leaderBoardInfos = new ArrayList<>();
    private ListenableFuture<List<LeaderBoardInfo>> leaderBoardFuture;
    private Executor executor;

    @Inject
    LeaderBoardInfoFetcher leaderBoardInfoFetcher;

    @Inject
    DobbyThreadpool dobbyThreadpool;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public WifiLeaderBoardFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((DobbyApplication) getActivity().getApplication()).getProdComponent().inject(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        handler = new Handler(this);
        View view = inflater.inflate(R.layout.leaderboard_fragment, container, false);
        TextView tv = (TextView) view.findViewById(R.id.wifi_lb_frag_title_tv);
        tv.setText(R.string.wifi_leaderboard);
        recyclerView = (RecyclerView) view.findViewById(R.id.wifi_leader_board);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        //Set the leaderboard infos here
        //populateDummyLeaderBoardInfos();
        //wifiLeaderBoardRecyclerViewAdapter = new WifiLeaderBoardRecyclerViewAdapter(leaderBoardInfo);
        wifiLeaderBoardRecyclerViewAdapter = new WifiLeaderBoardRecyclerViewAdapter(new ArrayList<LeaderBoardInfo>());
        recyclerView.setAdapter(wifiLeaderBoardRecyclerViewAdapter);
        //Fetching the leaderboard
        leaderBoardFuture = leaderBoardInfoFetcher.fetchLeaderBoardInfo(MAX_USERS_IN_LEADERBOARD);
        setLeaderBoardFuture(leaderBoardFuture, dobbyThreadpool.getExecutor());
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        //Unregister from firebase value event listener here
    }

    private void populateDummyLeaderBoardInfos() {
        LeaderBoardInfo leaderBoardInfo1 = new LeaderBoardInfo(1, 10.00, "dummy1");
        LeaderBoardInfo leaderBoardInfo2 = new LeaderBoardInfo(2, 5.00, "dummy2");
        LeaderBoardInfo leaderBoardInfo3 = new LeaderBoardInfo(3, 1.00, "dummy3");
        leaderBoardInfos.add(leaderBoardInfo1);
        leaderBoardInfos.add(leaderBoardInfo2);
        leaderBoardInfos.add(leaderBoardInfo3);
    }


    public void updateLeaderBoard(List<LeaderBoardInfo> results) {
        //See how we handle updates here
        //leaderBoardInfos = results;
        wifiLeaderBoardRecyclerViewAdapter.refresh(results);
    }

    public void setLeaderBoardFuture(final ListenableFuture<List<LeaderBoardInfo>> leaderBoardFuture, Executor executor) {
        leaderBoardFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    DobbyLog.i("Leader board future got result:" + leaderBoardFuture.get());
                    Message.obtain(handler, MSG_UPDATE_LEADERBOARD, leaderBoardFuture.get()).sendToTarget();
                } catch (Exception e) {
                    DobbyLog.i("Exception getting leader board info");
                }
            }
        }, executor);
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_UPDATE_LEADERBOARD:
                updateLeaderBoard((List<LeaderBoardInfo>) msg.obj);
                break;
        }
        return false;
    }

}
