package com.inceptai.dobby.ui;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.inceptai.dobby.R;
import com.inceptai.dobby.leaderboard.LeaderBoardInfo;
import com.inceptai.dobby.utils.DobbyLog;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link com.inceptai.dobby.leaderboard.LeaderBoardInfo}.
 */
public class WifiLeaderBoardRecyclerViewAdapter extends RecyclerView.Adapter<WifiLeaderBoardRecyclerViewAdapter.ViewHolder> {

    private List<LeaderBoardInfo> mValues;

    public WifiLeaderBoardRecyclerViewAdapter(List<LeaderBoardInfo> items) {
        mValues = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        DobbyLog.i("onCreateViewHolder");
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.leader_board_single_entry, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        DobbyLog.i("onBindViewHolder: " + position);
        holder.mItem = mValues.get(position);
        holder.mRankView.setText(String.valueOf(position + 1));
        holder.mHandleView.setText(String.valueOf(mValues.get(position).getHandle()));
        holder.mSpeedView.setText(String.format("%.2f Mbps", mValues.get(position).getSpeed()));
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mRankView;
        public final TextView mHandleView;
        public final TextView mSpeedView;
        public LeaderBoardInfo mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mRankView = (TextView) view.findViewById(R.id.lb_rank);
            mHandleView = (TextView) view.findViewById(R.id.lb_handle);
            mSpeedView = (TextView) view.findViewById(R.id.lb_speed);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mSpeedView.getText() + "'";
        }
    }


    public void refresh(final List<LeaderBoardInfo> results) {
        Comparator<LeaderBoardInfo> leaderBoardInfoComparator = new Comparator<LeaderBoardInfo>() {
            @Override
            public int compare(LeaderBoardInfo o1, LeaderBoardInfo o2) {
                if (o2.getSpeed() - o1.getSpeed() > 0) {
                    return 1;
                } else if (o2.getSpeed() - o1.getSpeed() == 0) {
                    return 0;
                } else {
                    return -1;
                }
                //return (int) (o2.getSpeed() - o1.getSpeed());
            }
        };
        Collections.sort(results, leaderBoardInfoComparator);
        mValues = results;
        notifyDataSetChanged();
        DobbyLog.i("Notified data sets changed." + mValues.toString());
    }

}
