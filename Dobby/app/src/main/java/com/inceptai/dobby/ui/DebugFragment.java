package com.inceptai.dobby.ui;


import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SwitchCompat;
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
import com.inceptai.dobby.speedtest.BandwidthObserver;
import com.inceptai.dobby.speedtest.BandwidthTestCodes;
import com.inceptai.dobby.speedtest.NewBandwidthAnalyzer;
import com.inceptai.dobby.speedtest.ServerInformation;
import com.inceptai.dobby.speedtest.SpeedTestConfig;
import com.inceptai.dobby.utils.DobbyLog;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import static com.inceptai.dobby.speedtest.BandwidthTestCodes.TestMode.DOWNLOAD;
import static com.inceptai.dobby.speedtest.BandwidthTestCodes.TestMode.DOWNLOAD_AND_UPLOAD;
import static com.inceptai.dobby.speedtest.BandwidthTestCodes.TestMode.IDLE;
import static com.inceptai.dobby.speedtest.BandwidthTestCodes.TestMode.UPLOAD;

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
    private boolean scheduleFollowupBandwidthTest = false;
    @BandwidthTestCodes.TestMode
    private int followupBandwidthTestMode = BandwidthTestCodes.TestMode.IDLE;

    @Inject
    NetworkLayer networkLayer;
    @Inject
    DobbyThreadpool threadpool;
    @Inject
    DobbyEventBus eventBus;


    public DebugFragment() {
        DobbyLog.v("Constructing DebugFragment " + this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (eventBus != null) {
            eventBus.unregisterListener(this);
        }
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
            @BandwidthTestCodes.TestMode int testMode = IDLE;
            String config = "";
            if (uploadSwitchButton.isChecked()) {
                config = "Upload ";
                testMode = BandwidthTestCodes.TestMode.UPLOAD;
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
            getWifiState();
        }

    }

    public void addConsoleText(final String text) {
        threadpool.submit(new Runnable() {
            @Override
            public void run() {
                DobbyLog.i(text);
            }
        });
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
        DobbyLog.v("Ping started at: " + System.currentTimeMillis());
        final ListenableFuture<HashMap<String, PingStats>> future = networkLayer.startPing();
        if (future == null) {
            DobbyLog.v("Starting ping failed");
            return;
        }
        future.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    DobbyLog.v("Printing results: " + future.get().toString());
                    addConsoleText("Ping Results:" + future.get().toString());
                    DobbyLog.v("Ping ended at: " + System.currentTimeMillis());
                    long estimatedTime = System.currentTimeMillis() - startedAt;
                    DobbyLog.v("Time elapsed: " + estimatedTime + " ms");
                } catch (InterruptedException e) {
                    DobbyLog.w("Exception pinging " + e);
                } catch (ExecutionException e) {
                    DobbyLog.w("Exception pinging " + e);
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
                    DobbyLog.v("Got wifi scan result");
                    addConsoleText("Wifi Scan:" + future.get().toString());
                } catch (InterruptedException e) {
                    DobbyLog.w("InterruptedException parsing " + e);
                } catch (ExecutionException e) {
                    DobbyLog.w("ExecutionException parsing " + e);
                }
            }
        }, threadpool.getUiThreadExecutor());
    }

    private void getWifiState() {
        addConsoleText("\nWifi Channel Stats:" + networkLayer.getChannelStats().toString());
    }

    private void startBandwidthTest(@BandwidthTestCodes.TestMode final int testMode) {
        threadpool.submit(new Runnable() {
            @Override
            public void run() {
                BandwidthObserver observer = networkLayer.startBandwidthTest(testMode);
                observer.registerCallback(DebugFragment.this);
                if (observer.getTestModeRequested() != testMode) {
                    setFollowUpBandwidthTest(testMode);
                    addConsoleText("Scheduled follow up test. Currently another test is running.");
                }
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
            addConsoleText("Server : " + details.name + " : " + details.latencyMs + " ms.");
        }
    }

    @Override
    public void onBestServerSelected(ServerInformation.ServerDetails bestServer) {
        addConsoleText("Best server selected: " + bestServer.name);
    }

    @Override
    public void onTestFinished(@BandwidthTestCodes.TestMode int testMode, BandwidthStats stats) {
        addConsoleText("Bandwidth Test finished: " + stats.getOverallBandwidth() / 1.0E6 + " Mbps.");
        bwDisplayTs = 0;
        if (scheduleFollowupBandwidthTest) {
            followupBandwidthTest();
        }
    }

    @Override
    public void onTestProgress(@BandwidthTestCodes.TestMode int testMode, double instantBandwidth) {
        long currentTs = System.currentTimeMillis();
        String type = testMode == UPLOAD ? "Upload " : "Download";
        if (currentTs - bwDisplayTs > 1000L) {
            bwDisplayTs = currentTs;
            addConsoleText(type + "bw: " + String.format("%2.2f", instantBandwidth / 1.0E6) + " Mbps.");
        }
    }

    @Override
    public void onBandwidthTestError(@BandwidthTestCodes.TestMode int testMode, @BandwidthTestCodes.ErrorCodes int errorCode, @Nullable String errorMessage) {
        String msg = Strings.isNullOrEmpty(errorMessage) ? "" : errorMessage;
        addConsoleText("Bandwidth test error, exceptionCode: " + errorCode + ",  " + msg);
        if (scheduleFollowupBandwidthTest) {
            followupBandwidthTest();
        }
    }

    @Subscribe
    public void listen(DobbyEvent event) {
        addConsoleText("Found event on dobby event bus: " + event.toString());
    }

    private void setFollowUpBandwidthTest(@BandwidthTestCodes.TestMode int testMode) {
        followupBandwidthTestMode = testMode;
        scheduleFollowupBandwidthTest = true;
    }

    private void clearFollowupBandwidthTest() {
        scheduleFollowupBandwidthTest = false;
        followupBandwidthTestMode = BandwidthTestCodes.TestMode.IDLE;
    }

    private void followupBandwidthTest() {
        if (!scheduleFollowupBandwidthTest) return;
        BandwidthObserver observer = networkLayer.startBandwidthTest(followupBandwidthTestMode);
        observer.registerCallback(DebugFragment.this);
        clearFollowupBandwidthTest();
    }
}
