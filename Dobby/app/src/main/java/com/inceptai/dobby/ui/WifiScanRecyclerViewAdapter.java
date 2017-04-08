package com.inceptai.dobby.ui;

import android.net.wifi.ScanResult;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.inceptai.dobby.R;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * {@link RecyclerView.Adapter} that can display a {@link ScanResult}.
 */
public class WifiScanRecyclerViewAdapter extends RecyclerView.Adapter<WifiScanRecyclerViewAdapter.ViewHolder> {

    private List<ScanResult> mValues;

    public WifiScanRecyclerViewAdapter(List<ScanResult> items) {
        mValues = items;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.i(TAG, "onCreateViewHolder");
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.fragment_single_ap, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        Log.i(TAG, "onBindViewHolder: " + position);
        holder.mItem = mValues.get(position);
        holder.mIdView.setText(mValues.get(position).BSSID);
        holder.mContentView.setText(String.valueOf(mValues.get(position).level));
    }

    @Override
    public int getItemCount() {
        return mValues.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public final View mView;
        public final TextView mIdView;
        public final TextView mContentView;
        public ScanResult mItem;

        public ViewHolder(View view) {
            super(view);
            mView = view;
            mIdView = (TextView) view.findViewById(R.id.ap_mac);
            mContentView = (TextView) view.findViewById(R.id.ap_rssi);
        }

        @Override
        public String toString() {
            return super.toString() + " '" + mContentView.getText() + "'";
        }
    }

    public void refresh(List<ScanResult> results) {
        Comparator<ScanResult> scanResultComparator = new Comparator<ScanResult>() {
            @Override
            public int compare(ScanResult o1, ScanResult o2) {
                return o1.BSSID.compareTo(o2.BSSID);
            }
        };
        Collections.sort(results, scanResultComparator);
        mValues = results;
        notifyDataSetChanged();
        Log.i(TAG, "Notified data sets changed." + mValues.toString());
    }
}
