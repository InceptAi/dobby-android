package com.inceptai.dobby.ui;

import android.content.Context;
import android.net.wifi.ScanResult;
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
import com.inceptai.dobby.R;
import com.inceptai.dobby.utils.DobbyLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * A fragment representing a list of wifi aps and their signal strengths.
 */
public class WifiFragment extends Fragment implements Handler.Callback {
    public static final String FRAGMENT_TAG = "WifiFragment";
    private static final int MSG_UPDATE_WIFI_SCAN_RESULT = 101;

    private RecyclerView recyclerView;
    private WifiScanRecyclerViewAdapter wifiScanRecyclerViewAdapter;
    private Handler handler;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public WifiFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        handler = new Handler(this);
        View view = inflater.inflate(R.layout.fragment_wifi_scan, container, false);
        TextView tv = (TextView) view.findViewById(R.id.wifi_frag_title_tv);
        tv.setText(R.string.wifi_scan_results_title);
        recyclerView = (RecyclerView) view.findViewById(R.id.wifi_scan_list);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        wifiScanRecyclerViewAdapter = new WifiScanRecyclerViewAdapter(
                new ArrayList<ScanResult>());
        recyclerView.setAdapter(wifiScanRecyclerViewAdapter);

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    public void updateWifiScanResults(List<ScanResult> results) {
        wifiScanRecyclerViewAdapter.refresh(results);
    }

    public void setWifiScanFuture(final ListenableFuture<List<ScanResult>> scanFuture, Executor executor) {
        scanFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    DobbyLog.i("wifi scan future got result:" + scanFuture.get());
                    Message.obtain(handler, MSG_UPDATE_WIFI_SCAN_RESULT, scanFuture.get()).sendToTarget();
                } catch (Exception e) {
                    DobbyLog.i("Exception getting scan result");
                }
            }
        }, executor);
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_UPDATE_WIFI_SCAN_RESULT:
                updateWifiScanResults((List<ScanResult>) msg.obj);
                break;
        }
        return false;
    }
}
