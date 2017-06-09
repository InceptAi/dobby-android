package com.inceptai.dobby.ui;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.inceptai.dobby.R;
import com.inceptai.dobby.leaderboard.LeaderBoardInfo;
import com.inceptai.dobby.utils.DobbyLog;

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
        holder.mRankView.setText(String.valueOf(mValues.get(position).getRank()));
        holder.mHandleView.setText(String.valueOf(mValues.get(position).getUserHandle()));
        holder.mSpeedView.setText(String.valueOf(mValues.get(position).getSpeed()));
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


    public void refresh(List<LeaderBoardInfo> results) {
        /*
        Comparator<ScanResult> scanResultComparator = new Comparator<ScanResult>() {
            @Override
            public int compare(ScanResult o1, ScanResult o2) {
                return o1.BSSID.compareTo(o2.BSSID);
            }
        };
        Collections.sort(results, scanResultComparator);
        */
        mValues = results;
        notifyDataSetChanged();
        DobbyLog.i("Notified data sets changed." + mValues.toString());
    }

}
