package com.inceptai.dobby.ui;


import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.common.base.Strings;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.NetworkLayer;
import com.inceptai.dobby.R;
import com.inceptai.dobby.eventbus.DobbyEvent;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.model.BandwidthStats;
import com.inceptai.dobby.model.PingStats;
import com.inceptai.dobby.speedtest.BandwithTestCodes;
import com.inceptai.dobby.speedtest.NewBandwidthAnalyzer;
import com.inceptai.dobby.speedtest.ServerInformation;
import com.inceptai.dobby.speedtest.SpeedTestConfig;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import static com.inceptai.dobby.DobbyApplication.TAG;
import static com.inceptai.dobby.speedtest.BandwithTestCodes.BandwidthTestMode.DOWNLOAD;
import static com.inceptai.dobby.speedtest.BandwithTestCodes.BandwidthTestMode.DOWNLOAD_AND_UPLOAD;
import static com.inceptai.dobby.speedtest.BandwithTestCodes.BandwidthTestMode.IDLE;
import static com.inceptai.dobby.speedtest.BandwithTestCodes.BandwidthTestMode.UPLOAD;

/**
 * A simple {@link Fragment} subclass.
 */
public class DebugFragment extends Fragment implements View.OnClickListener, NewBandwidthAnalyzer.ResultsCallback {
    public static final String FRAGMENT_TAG = "DebugFragment";
    private static final String TAG_BW_TEST = "bw test";
    private static final String TAG_WIFI_SCAN = "wifi scan";
    private static final String TAG_PING = "ping";
    private static final String TAG_WIFI_STATS = "wifi stats";

    private Button bwTestButton;
    private Button wifiScanButton;
    private Button pingTestButton;
    private Button wifiStatsButton;
    private SwitchCompat uploadSwitchButton;
    private SwitchCompat downloadSwitchButton;
    private TextView consoleTv;
    private long bwDisplayTs;

    @Inject
    NetworkLayer networkLayer;
    @Inject
    DobbyThreadpool threadpool;
    @Inject
    DobbyEventBus eventBus;


    public DebugFragment() {
        // Required empty public constructor
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ((DobbyApplication) getActivity().getApplication()).getProdComponent().inject(this);
        //Assigning listener for event bus
        eventBus.registerListener(this);

        // Inflate the layout for this fragment
        View mainView =  inflater.inflate(R.layout.fragment_debug, container, false);
        bwTestButton = (Button) mainView.findViewById(R.id.bw_test_button);
        bwTestButton.setTag(TAG_BW_TEST);
        bwTestButton.setOnClickListener(this);

        wifiScanButton = (Button) mainView.findViewById(R.id.wifi_scan_button);
        wifiScanButton.setTag(TAG_WIFI_SCAN);
        wifiScanButton.setOnClickListener(this);

        pingTestButton = (Button) mainView.findViewById(R.id.ping_button);
        pingTestButton.setTag(TAG_PING);
        pingTestButton.setOnClickListener(this);

        wifiStatsButton = (Button) mainView.findViewById(R.id.wifi_stats_button);
        wifiStatsButton.setTag(TAG_WIFI_STATS);
        wifiStatsButton.setOnClickListener(this);

        uploadSwitchButton = (SwitchCompat) mainView.findViewById(R.id.upload_switch_button);
        downloadSwitchButton = (SwitchCompat) mainView.findViewById(R.id.download_switch_button);

        consoleTv = (TextView) mainView.findViewById(R.id.console_textview);

        return mainView;
    }

    @Override
    public void onClick(View v) {
        String tag = (String) v.getTag();
        if (TAG_BW_TEST.equals(tag)) {
            @BandwithTestCodes.BandwidthTestMode int testMode = IDLE;
            String config = "";
            if (uploadSwitchButton.isChecked()) {
                config = "Upload ";
                testMode = BandwithTestCodes.BandwidthTestMode.UPLOAD;
            }
            if (downloadSwitchButton.isChecked()) {
                config += "Download ";
                testMode = (testMode == UPLOAD) ? DOWNLOAD_AND_UPLOAD : DOWNLOAD;
            }

            if (!config.isEmpty()) {
                addConsoleText("Starting Bandwidth test with : { " + config + " } and " + testMode);
                startBandwidthTest(testMode);
            } else {
                addConsoleText("No bandwidth test selected !");
            }

        } else if (TAG_PING.equals(tag)) {
            addConsoleText("\nStarting Ping.");
            startPing();
        } else if (TAG_WIFI_SCAN.equals(tag)) {
            addConsoleText("\nStarting Wifi scan...");
            startWifiScan();
        } else if (TAG_WIFI_STATS.equals(tag)) {
            getWifiStats();
        }

    }

    public void addConsoleText(final String text) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            String existingText = consoleTv.getText().toString();
            consoleTv.setText(existingText + "\n" + text);
        } else {
            threadpool.getUiThreadExecutor().execute(new Runnable() {
                @Override
                public void run() {
                    addConsoleText(text);
                }
            });
        }
    }


    private void startPing() {
        final long startedAt = System.currentTimeMillis();
        Log.v(TAG, "Ping started at: " + System.currentTimeMillis());
        final ListenableFuture<HashMap<String, PingStats>> future = networkLayer.startPing();
        if (future == null) {
            Log.v(TAG, "Starting ping failed");
            return;
        }
        future.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.v(TAG, "Printing results: " + future.get().toString());
                    addConsoleText("Ping Results:" + future.get().toString());
                    Log.v(TAG, "Ping ended at: " + System.currentTimeMillis());
                    long estimatedTime = System.currentTimeMillis() - startedAt;
                    Log.v(TAG, "Time elapsed: " + estimatedTime + " ms");
                } catch (InterruptedException e) {
                    Log.w(TAG, "Exception pinging " + e);
                } catch (ExecutionException e) {
                    Log.w(TAG, "Exception pinging " + e);
                }
            }
        }, threadpool.getUiThreadExecutor());
    }


    private void startWifiScan() {
        final ListenableFuture<List<ScanResult>> future = networkLayer.wifiScan();
        future.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    addConsoleText("Wifi Scan:" + future.get().toString());
                } catch (InterruptedException e) {
                    Log.w(TAG, "Exception parsing " + e);
                } catch (ExecutionException e) {
                    Log.w(TAG, "Exception parsing " + e);
                }
            }
        }, threadpool.getUiThreadExecutor());
    }

    private void getWifiStats() {
        addConsoleText("\nWifi Stats:" + networkLayer.getWifiStats().toString());
    }

    private void startBandwidthTest(@BandwithTestCodes.BandwidthTestMode final int testMode) {
        threadpool.submit(new Runnable() {
            @Override
            public void run() {
                networkLayer.startBandwidthTest(DebugFragment.this, testMode);
            }
        });
    }

    @Override
    public void onConfigFetch(SpeedTestConfig config) {
        addConsoleText("Config fetch done.");
    }

    @Override
    public void onServerInformationFetch(ServerInformation serverInformation) {
        addConsoleText("Server information fetched: " + serverInformation.serverList.size() + " servers.");
    }

    @Override
    public void onClosestServersSelected(List<ServerInformation.ServerDetails> closestServers) {
        addConsoleText("Closest server selected.");
        for (ServerInformation.ServerDetails details : closestServers) {
            addConsoleText("Server : " + details.name + " : " + details.latency + " ms.");
        }
    }

    @Override
    public void onBestServerSelected(ServerInformation.ServerDetails bestServer) {
        addConsoleText("Best server selected: " + bestServer.name);
    }

    @Override
    public void onTestFinished(@BandwithTestCodes.BandwidthTestMode int testMode, BandwidthStats stats) {
        addConsoleText("Bandwidth Test finished: " + stats.getPercentile90() / 1.0E6 + " Mbps.");
        bwDisplayTs = 0;
    }

    @Override
    public void onTestProgress(@BandwithTestCodes.BandwidthTestMode int testMode, double instantBandwidth) {
        long currentTs = System.currentTimeMillis();
        String type = testMode == UPLOAD ? "Upload " : "Download";
        if (currentTs - bwDisplayTs > 1000L) {
            bwDisplayTs = currentTs;
            addConsoleText(type + "bw: " + String.format("%2.2f", instantBandwidth / 1.0E6) + " Mbps.");
        }
    }

    @Override
    public void onBandwidthTestError(@BandwithTestCodes.BandwidthTestMode int testMode, @BandwithTestCodes.BandwidthTestErrorCodes int errorCode, @Nullable String errorMessage) {
        String msg = Strings.isNullOrEmpty(errorMessage) ? "" : errorMessage;
        addConsoleText("Bandwidth test error, errorCode: " + errorCode + ",  " + msg);
    }


    @Subscribe
    public void listen(DobbyEvent event) {
        addConsoleText("Found event on dobby event bus: " + event.toString());
    }
}
