package com.inceptai.dobby.ui;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.eventbus.Subscribe;
import com.inceptai.dobby.R;
import com.inceptai.dobby.ai.DataInterpreter;
import com.inceptai.dobby.eventbus.DobbyEvent;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.model.BandwidthStats;
import com.inceptai.dobby.speedtest.BandwidthObserver;
import com.inceptai.dobby.speedtest.BandwithTestCodes;
import com.inceptai.dobby.speedtest.NewBandwidthAnalyzer;
import com.inceptai.dobby.speedtest.ServerInformation;
import com.inceptai.dobby.speedtest.SpeedTestConfig;
import com.inceptai.dobby.utils.Utils;

import java.util.List;

public class WifiDocMainFragment extends Fragment implements View.OnClickListener, NewBandwidthAnalyzer.ResultsCallback, Handler.Callback{
    public static final String TAG = "WifiDocMainFragment";
    private static final int PERMISSION_COARSE_LOCATION_REQUEST_CODE = 101;
    private static final String ARG_PARAM1 = "param1";
    private static final int MSG_UPDATED_CIRCULAR_GAUGE = 1001;
    private static final int MSG_WIFI_GRADE_AVAILABLE = 1002;
    private static final int MSG_PING_GRADE_AVAILABLE = 1003;

    private OnFragmentInteractionListener mListener;

    private FloatingActionButton mainFab;
    private CircularGauge circularGauge;
    private String mParam1;
    private DobbyEventBus eventBus;
    private BandwidthObserver bandwidthObserver;
    private Handler handler;
    private TextView gaugeTv;
    private TextView pingTv;
    private TextView wifiTv;

    public WifiDocMainFragment() {
        // Required empty public constructor
    }

    public static WifiDocMainFragment newInstance(String param1) {
        WifiDocMainFragment fragment = new WifiDocMainFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        handler = new Handler(this);
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_wifi_doc_main, container, false);
        mainFab = (FloatingActionButton) view.findViewById(R.id.main_fab_button);
        mainFab.setOnClickListener(this);
        circularGauge = (CircularGauge) view.findViewById(R.id.bw_gauge);
        gaugeTv = (TextView) view.findViewById(R.id.gauge_tv);
        wifiTv = (TextView) view.findViewById(R.id.wifi_quality_tv);
        requestPermissions();
        return view;
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        eventBus = ((WifiDocActivity) getActivity()).getEventBus();
        eventBus.registerListener(this);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        eventBus.unregisterListener(this);
        if (bandwidthObserver != null) {
            bandwidthObserver.unregisterCallback(this);
        }
        mListener = null;
    }

    @Override
    public void onClick(View v) {
        if (mListener != null) {
            mListener.onMainButtonClick();
        }
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
        void onMainButtonClick();
    }


    @TargetApi(Build.VERSION_CODES.M)
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (getActivity().checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("This app needs the LOCATION permission");
                builder.setMessage(" In order to analyze your wifi network, Android requires us to ask for the LOCATION permission." +
                " We do not compute or use your location in this app.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_COARSE_LOCATION_REQUEST_CODE);
                    }
                });
                builder.show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_COARSE_LOCATION_REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG,"Coarse location permission granted.");
                } else {
                    Utils.buildSimpleDialog(getContext(), "Functionality limited",
                            "Since location access has not been granted, this app will not be able to analyze your wifi network.");
                }
                return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Subscribe
    public void listenToEventBus(DobbyEvent event) {
        if (event.getEventType() == DobbyEvent.EventType.BANDWIDTH_TEST_STARTING) {
            bandwidthObserver = (BandwidthObserver) event.getPayload();
            bandwidthObserver.registerCallback(this);
        } else if (event.getEventType() == DobbyEvent.EventType.WIFI_GRADE_AVAILABLE) {
            Message.obtain(handler, MSG_WIFI_GRADE_AVAILABLE, event.getPayload()).sendToTarget();
        } else if (event.getEventType() == DobbyEvent.EventType.PING_GRADE_AVAILABLE) {
            Message.obtain(handler, MSG_PING_GRADE_AVAILABLE, event.getPayload()).sendToTarget();
        }
    }

    @Override
    public void onConfigFetch(SpeedTestConfig config) {

    }

    @Override
    public void onServerInformationFetch(ServerInformation serverInformation) {

    }

    @Override
    public void onClosestServersSelected(List<ServerInformation.ServerDetails> closestServers) {

    }

    @Override
    public void onBestServerSelected(ServerInformation.ServerDetails bestServer) {

    }

    @Override
    public void onTestFinished(@BandwithTestCodes.TestMode int testMode, BandwidthStats stats) {

    }

    @Override
    public void onTestProgress(@BandwithTestCodes.TestMode int testMode, double instantBandwidth) {
        Message.obtain(handler, MSG_UPDATED_CIRCULAR_GAUGE, (int)(instantBandwidth / 1.0e6), testMode).sendToTarget();
    }

    @Override
    public void onBandwidthTestError(@BandwithTestCodes.TestMode int testMode, @BandwithTestCodes.ErrorCodes int errorCode, @Nullable String errorMessage) {

    }

    @Override
    public boolean handleMessage(Message msg) {
        switch(msg.what) {
            case MSG_UPDATED_CIRCULAR_GAUGE:
                circularGauge.setValue(msg.arg1);
                gaugeTv.setText(String.valueOf(msg.arg1));
                break;
            case MSG_WIFI_GRADE_AVAILABLE:
                showWifiResults((DataInterpreter.WifiGrade) msg.obj);
                break;
            case MSG_PING_GRADE_AVAILABLE:
                showPingResults((DataInterpreter.PingGrade) msg.obj);
                break;
        }
        return false;
    }

    private void showWifiResults(DataInterpreter.WifiGrade wifiGrade) {
        switch (wifiGrade.getPrimaryApSignalMetric()) {
            case DataInterpreter.MetricType.GOOD:
                break;
            case DataInterpreter.MetricType.AVERAGE:
                break;
            case DataInterpreter.MetricType.EXCELLENT:
                break;
            case DataInterpreter.MetricType.POOR:
                break;
            case DataInterpreter.MetricType.UNKNOWN:
                break;

        }
    }

    private void showPingResults(DataInterpreter.PingGrade pingGrade) {
        if (pingGrade == null) return;
        switch (pingGrade.getRouterLatencyMetric()) {
            case DataInterpreter.MetricType.GOOD:
                break;
            case DataInterpreter.MetricType.AVERAGE:
                break;
            case DataInterpreter.MetricType.EXCELLENT:
                break;
            case DataInterpreter.MetricType.POOR:
                break;
            case DataInterpreter.MetricType.UNKNOWN:
                break;

        }
    }
}
