package com.inceptai.dobby.ui;

import android.Manifest;
import android.animation.Animator;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.eventbus.Subscribe;
import com.inceptai.dobby.DobbyAnalytics;
import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.R;
import com.inceptai.dobby.RemoteConfig;
import com.inceptai.dobby.ai.DataInterpreter;
import com.inceptai.dobby.ai.SuggestionCreator;
import com.inceptai.dobby.eventbus.DobbyEvent;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.heartbeat.HeartBeatManager;
import com.inceptai.dobby.model.BandwidthStats;
import com.inceptai.dobby.speedtest.BandwidthObserver;
import com.inceptai.dobby.speedtest.BandwidthTestCodes;
import com.inceptai.dobby.speedtest.NewBandwidthAnalyzer;
import com.inceptai.dobby.speedtest.ServerInformation;
import com.inceptai.dobby.speedtest.SpeedTestConfig;
import com.inceptai.dobby.utils.DobbyLog;
import com.inceptai.dobby.utils.HtmlReportGenerator;
import com.inceptai.dobby.utils.Utils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static com.inceptai.dobby.ai.DataInterpreter.MetricType.ABYSMAL;
import static com.inceptai.dobby.ai.DataInterpreter.MetricType.AVERAGE;
import static com.inceptai.dobby.ai.DataInterpreter.MetricType.EXCELLENT;
import static com.inceptai.dobby.ai.DataInterpreter.MetricType.GOOD;
import static com.inceptai.dobby.ai.DataInterpreter.MetricType.POOR;
import static com.inceptai.dobby.utils.Utils.UNKNOWN_LATENCY_STRING;
import static com.inceptai.dobby.utils.Utils.ZERO_POINT_ZERO;
import static com.inceptai.dobby.utils.Utils.nonLinearBwScale;

public class WifiDocMainFragment extends Fragment implements View.OnClickListener, NewBandwidthAnalyzer.ResultsCallback, Handler.Callback {
    public static final String TAG = "WifiDocMainFragment";
    private static final int PERMISSION_COARSE_LOCATION_REQUEST_CODE = 101;
    private static final String ARG_PARAM1 = "param1";
    private static final int MSG_UPDATED_CIRCULAR_GAUGE = 1001;
    private static final int MSG_WIFI_GRADE_AVAILABLE = 1002;
    private static final int MSG_PING_GRADE_AVAILABLE = 1003;
    private static final int MSG_SUGGESTION_AVAILABLE = 1004;
    private static final int MSG_SHOW_STATUS = 1005;
    private static final int MSG_SWITCH_STATE = 1006;
    private static final int MSG_RESUME_HANDLER = 1007;
    private static final int MSG_WIFI_OFFLINE = 1008;
    private static final int MSG_REQUEST_LAYOUT = 1009;
    private static final long SUGGESTION_FRESHNESS_TS_MS = 30000; // 30 seconds
    private static final int MAX_HANDLER_PAUSE_MS = 5000;

    private static final int UI_STATE_INIT_AND_READY = 101; // Ready to run tests. Initial state.
    private static final int UI_STATE_RUNNING_TESTS = 102; // Running tests.
    private static final int UI_STATE_READY_WITH_RESULTS = 103;
    private int uiState = UI_STATE_INIT_AND_READY;

    private static final int BW_TEST_INITIATED = 200;
    private static final int BW_CONFIG_FETCHED = 201;
    private static final int BW_UPLOAD_RUNNING = 202;
    private static final int BW_DOWNLOAD_RUNNING = 203;
    private static final int BW_SERVER_INFO_FETCHED = 204;
    private static final int BW_BEST_SERVER_DETERMINED = 205;
    private static final int BW_IDLE = 207;

    private int bwTestState = BW_IDLE;

    private OnFragmentInteractionListener mListener;

    private BottomDialog bottomDialog;
    private FrameLayout runTestsFl;
    private FrameLayout aboutFl;
    private FrameLayout feedbackFl;
    private FrameLayout leaderboardFl;
    private LinearLayout shareFl;
    private LinearLayout bottomButtonBarLl;
    private FrameLayout expertChatFl;

    private CircularGauge downloadCircularGauge;
    private TextView downloadGaugeTv;
    private TextView downloadGaugeTitleTv;

    private CircularGauge uploadCircularGauge;
    private TextView uploadGaugeTv;
    private TextView uploadGaugeTitleTv;

    private CardView yourNetworkCv;
    private CardView pingCv;
    private CardView statusCv;

    private TextView statusTv;

    private TextView pingRouterTitleTv;
    private TextView pingRouterValueTv;
    private ImageView pingRouterGradeIv;

    private TextView pingDnsPrimaryTitleTv;
    private TextView pingDnsPrimaryValueTv;
    private ImageView pingDnsPrimaryGradeIv;

    private TextView pingDnsSecondTitleTv;
    private TextView pingDnsSecondValueTv;
    private ImageView pingDnsSecondGradeIv;

    private TextView pingWebTitleTv;
    private TextView pingWebValueTv;
    private ImageView pingWebGradeIv;

    private TextView wifiSsidTv;
    private TextView wifiSignalValueTv;
    private ImageView wifiSignalIconIv;
    private TextView routerIpTv;
    private TextView ispNameTv;

    private String statusMessage;
    private String mParam1;
    private DobbyEventBus eventBus;
    private BandwidthObserver bandwidthObserver;
    private Handler handler;
    private List<Message> handlerBacklog;
    private boolean pauseHandler;

    private SuggestionCreator.Suggestion currentSuggestion;

    private String testServerLocation = Utils.UNKNOWN;
    private String testServerLatencyMs = Utils.UNKNOWN;
    private String ispNameString = Utils.UNKNOWN;
    private String routerIpString = Utils.UNKNOWN;
    private double downloadBw;
    private double uploadBw;
    private DataInterpreter.PingGrade pingGrade;
    private DataInterpreter.WifiGrade wifiGrade;

    @Inject
    DobbyAnalytics dobbyAnalytics;

    @Inject
    RemoteConfig remoteConfig;

    @Inject
    HeartBeatManager heartBeatManager;

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

    public int getBwTestState() {
        return bwTestState;
    }

    public void setBwTestState(int bwTestState) {
        this.bwTestState = bwTestState;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ((DobbyApplication) getActivity().getApplication()).getProdComponent().inject(this);
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
        }
        statusMessage = Utils.EMPTY_STRING;
        handlerBacklog = new LinkedList<>();
        pauseHandler = false;
        remoteConfig.fetchAsync();
        heartBeatManager.setDailyHeartBeat();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        handler = new Handler(this);
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_wifi_doc_main_3, container, false);

        fetchViewInstances(view);
        resetData();
        // requestPermissions();
        uiStateVisibilityChanges(view);
        dobbyAnalytics.wifiDocFragmentEntered();
        return view;
    }

    private void uiStateVisibilityChanges(View rootView) {
        DobbyLog.v("uiStateVisibilty called.");
        if (uiState == UI_STATE_INIT_AND_READY) {
            yourNetworkCv.setVisibility(View.INVISIBLE);
            pingCv.setVisibility(View.GONE);
            statusCv.setVisibility(View.VISIBLE);
        } else if (uiState == UI_STATE_READY_WITH_RESULTS) {
            yourNetworkCv.setVisibility(View.VISIBLE);
            pingCv.setVisibility(View.VISIBLE);
            statusCv.setVisibility(View.INVISIBLE);
        } else if (uiState == UI_STATE_RUNNING_TESTS) {
            yourNetworkCv.setVisibility(View.INVISIBLE);
            pingCv.setVisibility(View.GONE);
            statusCv.setVisibility(View.VISIBLE);
        }
        if (rootView != null) {
            rootView.requestLayout();
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
        showLocationPermissionRequest();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        //eventBus.unregisterListener(this);
        if (bandwidthObserver != null) {
            bandwidthObserver.unregisterCallback(this);
        }
        mListener = null;
        if (eventBus != null) {
            eventBus.unregisterListener(this);
        }
    }

    @Override
    public void onClick(View v) {
        if (uiState == UI_STATE_RUNNING_TESTS) {
            Toast.makeText(getContext(), R.string.toast_tests_already_running, Toast.LENGTH_SHORT).show();
            return;
        }
        if (mListener != null) {
            if (uiState == UI_STATE_INIT_AND_READY || uiState == UI_STATE_READY_WITH_RESULTS) {
                setBwTestState(BW_TEST_INITIATED);
                showStatusMessageAsync(R.string.status_fetching_server_config);
                DobbyLog.v("WifiDoc: Issued command for starting bw tests");
                sendSwitchStateMessage(UI_STATE_RUNNING_TESTS);
            }
            mListener.onMainButtonClick();
            dobbyAnalytics.runTestsClicked();
        }
        resetData();
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
        void onMainButtonClick();
        void cancelTests();
        void onLocationPermissionGranted();
    }


    @TargetApi(Build.VERSION_CODES.M)
    public void showLocationPermissionRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Assume thisActivity is the current activity
            int permissionCheck = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION);
            if (permissionCheck != PERMISSION_GRANTED) {
                WifiDocDialogFragment fragment = WifiDocDialogFragment.forLocationPermission(this);
                fragment.show(getActivity().getSupportFragmentManager(), "Request Location Permission.");
            }
        }
    }

    public void requestLocationPermission() {
        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_COARSE_LOCATION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_COARSE_LOCATION_REQUEST_CODE:
                if (grantResults[0] == PERMISSION_GRANTED) {
                    DobbyLog.i("Coarse location permission granted.");
                    if (mListener != null) {
                        mListener.onLocationPermissionGranted();
                    }
                    //Trigger a scan here if needed
                } else {
                    Utils.buildSimpleDialog(getContext(), "Functionality limited",
                            "Since location access has not been granted, this app will not be able to analyze your wifi network. You can still use all the other features.");
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
        } else if (event.getEventType() == DobbyEvent.EventType.SUGGESTIONS_AVAILABLE) {
            Message.obtain(handler, MSG_SUGGESTION_AVAILABLE, event.getPayload()).sendToTarget();
        }

        if (uiState == UI_STATE_RUNNING_TESTS) {
            switch(event.getEventType()) {
                case DobbyEvent.EventType.BWTEST_INFO_AVAILABLE:
                    showStatusMessageAsync(R.string.status_analyzing_speed_test_data);
                    break;
                case DobbyEvent.EventType.BANDWIDTH_GRADE_AVAILABLE:
                    showStatusMessageAsync(R.string.status_speed_test_analysis_ready);
                    break;
                case DobbyEvent.EventType.PING_STARTED:
                    showStatusMessageAsync(R.string.status_running_ping_tests);
                    break;
                case DobbyEvent.EventType.PING_INFO_AVAILABLE:
                    showStatusMessageAsync(R.string.status_analyzing_ping_results);
                    break;
                case DobbyEvent.EventType.PING_GRADE_AVAILABLE:
                    showStatusMessageAsync(R.string.status_ping_analysis_ready);
                    break;
                case DobbyEvent.EventType.WIFI_SCAN_STARTING:
                    showStatusMessageAsync(R.string.status_running_wifi_tests);
                    break;
                case DobbyEvent.EventType.WIFI_SCAN_AVAILABLE:
                    showStatusMessageAsync(R.string.status_analyzing_wifi_data);
                    break;
                case DobbyEvent.EventType.WIFI_GRADE_AVAILABLE:
                    showStatusMessageAsync(R.string.status_wifi_analysis_ready);
                    break;
                case DobbyEvent.EventType.BANDWIDTH_TEST_FAILED_WIFI_OFFLINE:
                    showStatusMessageAsync(R.string.status_bw_test_error_wifi_offline);
                    showStatusMessageAsync(R.string.status_wifi_ping_tests_continue);
                    Message.obtain(handler, MSG_WIFI_OFFLINE).sendToTarget();
                    break;
            }
        }
    }

    @Override
    public void onConfigFetch(SpeedTestConfig config) {
        if (getBwTestState() == BW_TEST_INITIATED) {
            showStatusMessageAsync(R.string.status_fetching_server_list);
        }
        setBwTestState(BW_CONFIG_FETCHED);
        DobbyLog.v("WifiDoc: Fetched config");
    }

    @Override
    public void onServerInformationFetch(ServerInformation serverInformation) {
        if (getBwTestState() == BW_CONFIG_FETCHED) {
            String constructedString = getResources().getString(R.string.status_closest_servers, serverInformation.serverList.size());
            showStatusMessageAsync(constructedString);
        }
        setBwTestState(BW_SERVER_INFO_FETCHED);
        DobbyLog.v("WifiDoc: Fetched server info");
    }

    @Override
    public void onClosestServersSelected(List<ServerInformation.ServerDetails> closestServers) {
        //if (getBwTestState() == BW_SERVER_INFO_FETCHED) {
        //    showStatusMessage("Running latency tests for best server selection ...");
        //}
        DobbyLog.v("WifiDoc: Closest servers");
    }

    @Override
    public void onBestServerSelected(ServerInformation.ServerDetails bestServer) {
        if (getBwTestState() == BW_SERVER_INFO_FETCHED) {
            // showStatusMessageAsync("Closest server in " + bestServer.name + " has a latency of " + String.format("%.2f", bestServer.latencyMs) + " ms.");
            String constructedMessage = getResources().getString(R.string.status_found_closest_server, bestServer.name, bestServer.latencyMs);

            showStatusMessageAsync(constructedMessage);
        }
        if (bestServer.name != null) {
            testServerLocation = bestServer.name;
            testServerLatencyMs = Utils.doubleToString(bestServer.latencyMs);
        }
        setBwTestState(BW_BEST_SERVER_DETERMINED);
        DobbyLog.v("WifiDoc: Best server");
    }

    @Override
    public void onTestFinished(@BandwidthTestCodes.TestMode int testMode, BandwidthStats stats) {
        Message.obtain(handler, MSG_UPDATED_CIRCULAR_GAUGE, Utils.BandwidthValue.from(testMode, (stats.getOverallBandwidth() / 1.0e6))).sendToTarget();
        if (testMode == BandwidthTestCodes.TestMode.UPLOAD) {
            showStatusMessageAsync(R.string.status_finished_bw_tests);
            sendSwitchStateMessage(UI_STATE_READY_WITH_RESULTS);
            uploadBw = stats.getOverallBandwidth();
        } else {
            downloadBw = stats.getOverallBandwidth();
        }
    }

    @Override
    public void onTestProgress(@BandwidthTestCodes.TestMode int testMode, double instantBandwidth) {
        if (testMode == BandwidthTestCodes.TestMode.DOWNLOAD && getBwTestState() != BW_DOWNLOAD_RUNNING) {
            setBwTestState(BW_DOWNLOAD_RUNNING);
            showStatusMessageAsync(R.string.status_running_download_tests);
        } else if (testMode == BandwidthTestCodes.TestMode.UPLOAD && getBwTestState() != BW_UPLOAD_RUNNING) {
            setBwTestState(BW_UPLOAD_RUNNING);
            showStatusMessageAsync(R.string.status_running_upload_tests);
        }
        Message.obtain(handler, MSG_UPDATED_CIRCULAR_GAUGE, Utils.BandwidthValue.from(testMode, (instantBandwidth / 1.0e6))).sendToTarget();
    }

    @Override
    public void onBandwidthTestError(@BandwidthTestCodes.TestMode int testMode,
                                     @BandwidthTestCodes.ErrorCodes int errorCode,
                                     @Nullable String errorMessage) {
        showStatusMessageAsync(R.string.status_error_bw_tests);
    }

    @Override
    public boolean handleMessage(Message msg) {
        if (pauseHandler) {
            if (msg.what == MSG_RESUME_HANDLER) {
                resumeHandler();
            } else {
                handlerBacklog.add(Message.obtain(msg));
                DobbyLog.v("Adding message to backlog." + msg.what);
            }
            return true;
        }
        if (getActivity() == null) {
            DobbyLog.e("WifiDocMainFragment not attached to any activity.");
            return true;
        }
        switch(msg.what) {
            case MSG_UPDATED_CIRCULAR_GAUGE:
                updateBandwidthGauge(msg);
                break;
            case MSG_WIFI_GRADE_AVAILABLE:
                showWifiResults((DataInterpreter.WifiGrade) msg.obj);
                break;
            case MSG_PING_GRADE_AVAILABLE:
                showPingResults((DataInterpreter.PingGrade) msg.obj);
                break;
            case MSG_SUGGESTION_AVAILABLE:
                showSuggestion((SuggestionCreator.Suggestion) msg.obj);
                break;
            case MSG_SHOW_STATUS:
                showStatusMessage((String) msg.obj);
                break;
            case MSG_SWITCH_STATE:
                switchState(msg.arg1);
                break;
            case MSG_WIFI_OFFLINE:
                // Should we do anything here ?
                break;
            case MSG_REQUEST_LAYOUT:
                uiStateVisibilityChanges(getView());
                break;
            default:
                return false;
        }
        return true;
    }

    private void showWifiResults(DataInterpreter.WifiGrade wifiGrade) {
        if (wifiGrade != null) {
            this.wifiGrade = wifiGrade;
            dobbyAnalytics.wifiGrade(wifiGrade);
        }
        String ssid = wifiGrade.getPrimaryApSsid();
        if (ssid != null && !ssid.isEmpty()) {
            ssid = Utils.limitSsid(ssid);
            wifiSsidTv.setText(ssid);
        }
        setWifiResult(wifiSignalValueTv, String.valueOf(wifiGrade.getPrimaryApSignal()),
                wifiSignalIconIv, wifiGrade.getPrimaryApSignalMetric());
    }

    private void showPingResults(DataInterpreter.PingGrade pingGrade) {
        DobbyLog.v("Ping grade available.");
        if (pingGrade == null) {
            DobbyLog.w("Null ping grade.");
            return;
        }

        if (getActivity() == null) {
            DobbyLog.e("Fragment not attached to any activity.");
            return;
        }
        this.pingGrade = pingGrade;

        dobbyAnalytics.pingGrade(pingGrade);
        setPingResult(pingRouterValueTv, String.format("%02.1f", pingGrade.getRouterLatencyMs()),
                pingRouterGradeIv, pingGrade.getRouterLatencyMetric());

        setPingResult(pingDnsPrimaryValueTv, String.format("%02.1f", pingGrade.getDnsServerLatencyMs()),
                pingDnsPrimaryGradeIv, pingGrade.getDnsServerLatencyMetric());

        setPingResult(pingDnsSecondValueTv,  String.format("%02.1f", pingGrade.getAlternativeDnsLatencyMs()),
                pingDnsSecondGradeIv, pingGrade.getAlternativeDnsMetric());

        setPingResult(pingWebValueTv, String.format("%02.1f", pingGrade.getExternalServerLatencyMs()),
                pingWebGradeIv, pingGrade.getExternalServerLatencyMetric());
    }

    private void resetPingData() {
        if (getActivity() == null) {
            DobbyLog.e("Fragment not attached to any activity.");
            return;
        }
        setPingResult(pingRouterValueTv, ZERO_POINT_ZERO,
                pingRouterGradeIv, DataInterpreter.MetricType.UNKNOWN);

        setPingResult(pingDnsPrimaryValueTv, ZERO_POINT_ZERO,
                pingDnsPrimaryGradeIv, DataInterpreter.MetricType.UNKNOWN);

        setPingResult(pingDnsSecondValueTv,  ZERO_POINT_ZERO,
                pingDnsSecondGradeIv, DataInterpreter.MetricType.UNKNOWN);

        setPingResult(pingWebValueTv, ZERO_POINT_ZERO,
                pingWebGradeIv, DataInterpreter.MetricType.UNKNOWN);
    }

    private void showSuggestion(SuggestionCreator.Suggestion suggestion) {
        if (getActivity() == null) {
            DobbyLog.e("Fragment not attached to any activity.");
            return;
        }
        ispNameTv.setText(suggestion.getIsp());
        ispNameString = suggestion.getIsp();
        routerIpString = suggestion.getExternalIp();
        routerIpTv.setText(suggestion.getExternalIp());
        if (suggestion == null) {
            DobbyLog.w("Null suggestion received from eventbus.");
            return;
        }
        if (uiState != UI_STATE_READY_WITH_RESULTS) {
            switchState(UI_STATE_READY_WITH_RESULTS);
        }
        if (currentSuggestion != null && isSuggestionFresh(currentSuggestion)) {
            DobbyLog.i("Already have a fresh enough suggestion, ignoring new suggestion");
            return;
        }
        currentSuggestion = suggestion;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Title:").append(suggestion.getTitle()).append("\n").append("Text:");
        for(String line : suggestion.getShortSuggestionList()) {
            stringBuilder.append(line).append("\n");
        }
        dobbyAnalytics.briefSuggestionsShown(stringBuilder.toString());
        // suggestionsValueTv.setText(stringBuilder.toString());
        showSuggestionsUi();
        DobbyLog.v("Received suggestions:" + stringBuilder.toString());
    }


    private static boolean isSuggestionFresh(SuggestionCreator.Suggestion suggestion) {
        return (System.currentTimeMillis() - suggestion.getCreationTimestampMs()) < SUGGESTION_FRESHNESS_TS_MS;
    }

    private void fetchViewInstances(View rootView) {
        runTestsFl = (FrameLayout) rootView.findViewById(R.id.bottom_run_tests_fl);
        runTestsFl.setOnClickListener(this);

        shareFl = (LinearLayout) rootView.findViewById(R.id.button_top_left_fl);
       // leaderboardFl = (FrameLayout) rootView.findViewById(R.id.button_top_right_fl);
        aboutFl = (FrameLayout) rootView.findViewById(R.id.button_bottom_left_fl);
        feedbackFl = (FrameLayout) rootView.findViewById(R.id.button_bottom_right_fl);
        feedbackFl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSimpleFeedbackForm();
                dobbyAnalytics.feedbackButtonClicked();
                //dobbyAnalytics.feedbackFormShown();
            }
        });

        aboutFl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAboutAndPrivacyPolicy();
            }
        });

        shareFl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dobbyAnalytics.shareResultsEvent();
                shareResults();
            }
        });

        expertChatFl = (FrameLayout) rootView.findViewById(R.id.contact_expert_fl);
        expertChatFl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dobbyAnalytics.contactExpertEvent();
                showExpertChat();
            }
        });

        bottomButtonBarLl = (LinearLayout) rootView.findViewById(R.id.bottom_button_bar);

        yourNetworkCv = (CardView) rootView.findViewById(R.id.net_cardview);
        pingCv = (CardView) rootView.findViewById(R.id.ping_cardview);
        statusCv = (CardView) rootView.findViewById(R.id.status_cardview);

        View downloadView = rootView.findViewById(R.id.cg_download);
        downloadCircularGauge = (CircularGauge) downloadView.findViewById(R.id.bw_gauge);
        downloadGaugeTv = (TextView) downloadView.findViewById(R.id.gauge_tv);
        downloadGaugeTitleTv = (TextView) downloadView.findViewById(R.id.title_tv);
        downloadGaugeTitleTv.setText(R.string.download_bw);

        View uploadView = rootView.findViewById(R.id.cg_upload);
        uploadCircularGauge = (CircularGauge) uploadView.findViewById(R.id.bw_gauge);
        uploadGaugeTv = (TextView) uploadView.findViewById(R.id.gauge_tv);
        uploadGaugeTitleTv = (TextView) uploadView.findViewById(R.id.title_tv);
        uploadGaugeTitleTv.setText(R.string.upload_bw);

        View row1View = rootView.findViewById(R.id.ping_latency_row_inc1);
        pingRouterTitleTv = (TextView) row1View.findViewById(R.id.left_title_tv);
        pingRouterValueTv = (TextView) row1View.findViewById(R.id.left_value_tv);
        pingRouterGradeIv = (ImageView) row1View.findViewById(R.id.left_grade_iv);

        pingWebTitleTv = (TextView) row1View.findViewById(R.id.right_title_tv);
        pingWebValueTv = (TextView) row1View.findViewById(R.id.right_value_tv);
        pingWebGradeIv = (ImageView) row1View.findViewById(R.id.right_grade_iv);

        View row2View = rootView.findViewById(R.id.ping_latency_row_inc2);
        pingDnsPrimaryTitleTv = (TextView) row2View.findViewById(R.id.left_title_tv);
        pingDnsPrimaryValueTv = (TextView) row2View.findViewById(R.id.left_value_tv);
        pingDnsPrimaryGradeIv = (ImageView) row2View.findViewById(R.id.left_grade_iv);

        pingDnsSecondTitleTv = (TextView) row2View.findViewById(R.id.right_title_tv);
        pingDnsSecondValueTv = (TextView) row2View.findViewById(R.id.right_value_tv);
        pingDnsSecondGradeIv = (ImageView) row2View.findViewById(R.id.right_grade_iv);

        pingRouterTitleTv.setText(R.string.router_ping);
        pingDnsPrimaryTitleTv.setText(R.string.dns_primary_ping);
        pingDnsSecondTitleTv.setText(R.string.dns_second_ping);
        pingWebTitleTv.setText(R.string.web_ping);

        // Populate wifi card views
        wifiSsidTv = (TextView) rootView.findViewById(R.id.wifi_ssid_tv);

        wifiSignalValueTv = (TextView) rootView.findViewById(R.id.value_tv);
        wifiSignalIconIv = (ImageView) rootView.findViewById(R.id.icon_iv);
        ispNameTv = (TextView) rootView.findViewById(R.id.isp_name_tv);
        routerIpTv = (TextView) rootView.findViewById(R.id.router_ip_tv);

        statusTv = (TextView) rootView.findViewById(R.id.status_tv);
    }

    private void updateBandwidthGauge(Message msg) {
        Utils.BandwidthValue bandwidthValue = (Utils.BandwidthValue) msg.obj;
        if (bandwidthValue.mode == BandwidthTestCodes.TestMode.UPLOAD) {
            uploadCircularGauge.setValue((int) nonLinearBwScale(bandwidthValue.value));
            uploadGaugeTv.setText(String.format("%2.2f", bandwidthValue.value));
        } else if (bandwidthValue.mode == BandwidthTestCodes.TestMode.DOWNLOAD) {
            downloadCircularGauge.setValue((int) nonLinearBwScale(bandwidthValue.value));
            downloadGaugeTv.setText(String.format("%2.2f", bandwidthValue.value));
        }
    }


    private void setPingResult(TextView valueTv, String value, ImageView gradeIv, @DataInterpreter.MetricType int grade) {
        if (getActivity() == null) {
            DobbyLog.e("Fragment not attached to any activity.");
            return;
        }
        valueTv.setText(value);
        switch (grade) {
            case EXCELLENT:
                setImage(gradeIv, R.drawable.double_tick);
                break;
            case GOOD:
                setImage(gradeIv, R.drawable.tick_mark);
                break;
            case AVERAGE:
                setImage(gradeIv, R.drawable.yellow_tick_mark);
                break;
            case POOR:
                setImage(gradeIv, R.drawable.poor_icon);
                break;
            case ABYSMAL:
                setImage(gradeIv, R.drawable.poor_icon);
                break;
            default:
                setImage(gradeIv, R.drawable.non_functional);
                valueTv.setText(UNKNOWN_LATENCY_STRING);
                break;

        }
        pingCv.requestLayout();
    }

    private void setWifiResult(TextView valueTv, String value, ImageView gradeIv, @DataInterpreter.MetricType int grade) {
        if (getActivity() == null) {
            DobbyLog.e("Fragment not attached to any activity.");
            return;
        }
        valueTv.setText(value);
        switch (grade) {
            case EXCELLENT:
                setImage(gradeIv, R.drawable.signal_5);
                break;
            case GOOD:
                setImage(gradeIv, R.drawable.signal_4);
                break;
            case AVERAGE:
                setImage(gradeIv, R.drawable.signal_3);
                break;
            case POOR:
                setImage(gradeIv, R.drawable.signal_2);
                break;
            case ABYSMAL:
                setImage(gradeIv, R.drawable.signal_1);
                break;
            default:
                setImage(gradeIv, R.drawable.signal_disc);
                break;

        }
    }

    private void resetData() {
        uploadCircularGauge.setValue(0);
        downloadCircularGauge.setValue(0);
        wifiSignalValueTv.setText(String.valueOf(0));
        uploadGaugeTv.setText(ZERO_POINT_ZERO);
        downloadGaugeTv.setText(ZERO_POINT_ZERO);
        // Clear suggestions
        currentSuggestion = null;
        resetPingData();
        ispNameTv.setText(Utils.EMPTY_STRING);
        wifiSsidTv.setText(Utils.EMPTY_STRING);
        //ispNameTv.setText("");
    }

    private void setImage(ImageView view, int resourceId) {
        Utils.setImage(getContext(), view, resourceId);
    }

    private void showStatusMessageAsync(String message) {
        Message msg = Message.obtain(handler, MSG_SHOW_STATUS, message);
        handler.sendMessageDelayed(msg, 100);
    }

    private void showStatusMessageAsync(int resourceId) {
        String message = getResources().getString(resourceId);
        Message msg = Message.obtain(handler, MSG_SHOW_STATUS, message);
        handler.sendMessageDelayed(msg, 100);
    }

    private void sendSwitchStateMessage(int newState) {
        Message.obtain(handler, MSG_SWITCH_STATE, newState, 0).sendToTarget();
    }

    @UiThread
    private void showStatusMessage(String message) {
        // TODO Animate this if needed.
        statusMessage = message + "\n" + statusMessage;
        // statusTv.setText(statusMessage);
        if (bottomDialog != null) {
            bottomDialog.setContent(statusMessage);
        }
    }

    @UiThread
    private void showStatusMessage(int resourceId) {
        String message = getResources().getString(resourceId);
        showStatusMessage(message);
    }

    private synchronized void switchState(int newState) {
        int oldState = uiState;
        uiState = newState;
        uiStateVisibilityChanges(getView());
        if ((oldState == UI_STATE_INIT_AND_READY || oldState == UI_STATE_READY_WITH_RESULTS)
                && uiState == UI_STATE_RUNNING_TESTS) {
            // Tests starting.
            // Disable fab.
            if (bottomDialog == null) {
                bottomDialog = createBottomDialog();
                bottomDialog.show();
            }
            bottomDialog.show();
            bottomDialog.setModeStatus();
        }

        if (oldState == UI_STATE_RUNNING_TESTS && uiState == UI_STATE_READY_WITH_RESULTS) {
            if (bottomDialog != null) {
                bottomDialog.setModeStatusWithDismiss();
            }
        }

        if (uiState == UI_STATE_RUNNING_TESTS) {
            statusTv.setText(R.string.running_tests);
        } else if (uiState == UI_STATE_INIT_AND_READY) {
            statusTv.setText(R.string.ready_status_message);
        }
    }

    private void showSuggestionsUi() {
        if (currentSuggestion == null || bottomDialog == null) {
            Toast.makeText(getContext(), "Unable to show suggestions.", Toast.LENGTH_SHORT).show();
            return;
        }
        String suggestions = currentSuggestion.getTitle();
        bottomDialog.setModeSuggestion();
        bottomDialog.setSuggestion(suggestions);
        bottomDialog.showMoreDismissButtons();
    }

    private BottomDialog createBottomDialog() {
        BottomDialog dialog = new BottomDialog(getContext(), getView(), bottomButtonBarLl);
        dialog.show();
        return dialog;
    }

    private synchronized void pauseHandler() {
        if (pauseHandler) {
            // Handler already paused do nothing.
            return;
        }
        DobbyLog.v("Handler paused.");
        pauseHandler = true;
        Message msg = Message.obtain(handler, MSG_RESUME_HANDLER);
        handler.sendMessageDelayed(msg, MAX_HANDLER_PAUSE_MS);
    }

    private synchronized void resumeHandler() {
        if (!pauseHandler) return;
        pauseHandler = false;
        handler.removeMessages(MSG_RESUME_HANDLER);
        DobbyLog.v("Handler resumed.");
        for (Message msg : handlerBacklog) {
            DobbyLog.v("Sending backlog message BACK." + msg.what);
            handler.sendMessageDelayed(msg, 50);
        }
        handlerBacklog.clear();
    }

    final class BottomDialog implements Button.OnClickListener{
        private static final String TAG_CANCEL_BUTTON = "neg";
        private static final String TAG_DISMISS_BUTTON = "dismiss";
        private static final String TAG_CONTACT_EXPERT_BUTTON = "pos";
        private static final float Y_GUTTER_DP = 7;

        private static final int MODE_STATUS = 1001;
        private static final int MODE_SUGGESTION = 1002;

        // Cancel functionality not available in this mode.
        private static final int MODE_STATUS_DISMISS = 1003;

        private ImageView vIcon;
        private TextView vTitle;
        private TextView vContent;
        private Button vNegative;
        private Button vContactExpert;
        private View rootView;
        private Context context;
        private ConstraintLayout rootLayout;
        private int mode = MODE_STATUS;
        private boolean isVisible = false;
        private double finalTargetY =-1.0;
        private LinearLayout bottomButtonBar;

        BottomDialog(final Context context, View parentView, LinearLayout bottomButtonBar) {
            this.context = context;
            rootLayout = (ConstraintLayout) parentView.findViewById(R.id.root_constraint_layout);
            this.bottomButtonBar = bottomButtonBar;
            rootView = parentView.findViewById(R.id.bottom_dialog_inc);
            vIcon = (ImageView) rootView.findViewById(R.id.bottomDialog_icon);
            vTitle = (TextView) rootView.findViewById(R.id.bottomDialog_title);
            vContent = (TextView) rootView.findViewById(R.id.bottomDialog_content);
            vNegative = (Button) rootView.findViewById(R.id.bottomDialog_cancel);
            vNegative.setTag(TAG_CANCEL_BUTTON);
            vContactExpert = (Button) rootView.findViewById(R.id.bottomDialog_contact_expert);
            vContactExpert.setTag(TAG_CONTACT_EXPERT_BUTTON);
            vNegative.setOnClickListener(this);
            vContactExpert.setOnClickListener(this);
        }

        void show() {
            if (isVisible) {
                return;
            }
            rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (finalTargetY < 0.0) {
                        finalTargetY = rootView.getY();
                        finalTargetY = finalTargetY + Utils.dpToPixelsY(getContext(), Y_GUTTER_DP);
                    }
                    float maxY = rootLayout.getHeight();
                    getSlideInAnimator(maxY).start();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    } else {
                        rootView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    }
                }
            });
            LayoutTransition transition = rootLayout.getLayoutTransition();
            transition.setDuration(1000);
            transition.setInterpolator(LayoutTransition.APPEARING, new AccelerateDecelerateInterpolator());
            transition.setInterpolator(LayoutTransition.DISAPPEARING, new AccelerateDecelerateInterpolator());
            rootView.setVisibility(View.VISIBLE);
            vIcon.setVisibility(View.VISIBLE);
            vTitle.setVisibility(View.VISIBLE);
            vContactExpert.setVisibility(View.GONE);
            vNegative.setVisibility(View.VISIBLE);
            vNegative.setText(R.string.cancel_button);
            vNegative.setTag(TAG_CANCEL_BUTTON);
            bottomButtonBar.setVisibility(View.INVISIBLE);
            rootLayout.requestLayout();
            pauseHandler();
            isVisible = true;
        }

        private Animator getSlideInAnimator(float maxY) {
            Animator animator = ObjectAnimator.ofFloat(rootView, "y", maxY, (float) finalTargetY).setDuration(1000);
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    pauseHandler();
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    handler.sendEmptyMessageDelayed(MSG_REQUEST_LAYOUT, 500);
                    resumeHandler();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    resumeHandler();
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            return animator;
        }

        void setContent(String content) {
            if (mode != MODE_STATUS) {
                DobbyLog.i("Ignoring status message in dialog.");
                return;
            }
            vContent.setText(content);
        }

        void setSuggestion(String suggestion) {
            if (mode != MODE_SUGGESTION) {
                DobbyLog.i("Ignoring status message in dialog.");
                return;
            }
            vContent.setText(suggestion);
        }

        boolean isVisible() {
            return isVisible;
        }

        void setModeSuggestion() {
            mode = MODE_SUGGESTION;
            vTitle.setText(R.string.suggestions_title);
        }

        void setModeStatus() {
            mode = MODE_STATUS;
            vTitle.setText(R.string.status_title);
        }

        void setModeStatusWithDismiss() {
            mode = MODE_STATUS_DISMISS;
            showOnlyDismissButton();
        }

        void showMoreDismissButtons() {
            vNegative.setText(R.string.dismiss_button);
            vNegative.setTag(TAG_DISMISS_BUTTON);
            vNegative.setVisibility(View.VISIBLE);
            vContactExpert.setVisibility(View.VISIBLE);
        }

        void showOnlyDismissButton() {
            mode = MODE_STATUS_DISMISS;
            vNegative.setText(R.string.dismiss_button);
            vNegative.setVisibility(View.VISIBLE);
            vContactExpert.setVisibility(View.INVISIBLE);
        }

        void dismiss() {
            float targetY = rootView.getY();
            float maxY = rootLayout.getHeight();
            ObjectAnimator animator = ObjectAnimator.ofFloat(rootView, "y", targetY, maxY).setDuration(1000);
            isVisible = false;
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    pauseHandler();
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    dismissAndShowCanonicalViews();
                    rootView.setY((float) finalTargetY);
                    isVisible = false;
                    setModeStatus();
                    clearStatusMessages();
                    vContent.setText(Utils.EMPTY_STRING);
                    resumeHandler();
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                    dismissAndShowCanonicalViews();
                    resumeHandler();
                    vContent.setText(Utils.EMPTY_STRING);
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
            animator.start();
        }

        void dismissAndShowCanonicalViews() {
            rootView.setVisibility(View.INVISIBLE);
            bottomButtonBar.setVisibility(View.VISIBLE);
            isVisible = false;
        }

        void clearStatusMessages() {
            statusMessage = Utils.EMPTY_STRING;
        }

        @Override
        public void onClick(View v) {
            if (TAG_CONTACT_EXPERT_BUTTON.equals(v.getTag())) {
                showExpertChat();
            } else if (TAG_CANCEL_BUTTON.equals(v.getTag())) {
                if (mode == MODE_STATUS) {
                    cancelTests();
                }
                dismiss();
            } else if (TAG_DISMISS_BUTTON.equals(v.getTag())) {
                dismiss();
            }
        }
    }

    private void showMoreSuggestions() {
        if (currentSuggestion == null) {
            DobbyLog.v("Attempting to show more suggestions when currentSuggestions are null.");
        }
        WifiDocDialogFragment fragment = WifiDocDialogFragment.forSuggestion(currentSuggestion.getTitle(),
                currentSuggestion.getLongSuggestionList());
        fragment.show(getActivity().getSupportFragmentManager(), "Suggestions");
        dobbyAnalytics.moreSuggestionsShown(currentSuggestion.getTitle(),
                new ArrayList<String>(currentSuggestion.getShortSuggestionList()));
    }

    private void showExpertChat() {
        startActivity(new Intent(getContext(), ExpertChatActivity.class));
    }

    private void showAboutAndPrivacyPolicy() {
        WifiDocDialogFragment fragment = WifiDocDialogFragment.forAboutAndPrivacyPolicy();
        fragment.show(getActivity().getSupportFragmentManager(), "About");
        dobbyAnalytics.aboutShown();
    }

    private void showFeedbackForm() {
        WifiDocDialogFragment fragment = WifiDocDialogFragment.forFeedback(R.id.wifi_doc_placeholder_fl);
        fragment.show(getActivity().getSupportFragmentManager(), "Feedback");
        dobbyAnalytics.feedbackFormShown();
    }

    private void showSimpleFeedbackForm() {
        String userUuid = ((DobbyApplication)getActivity().getApplicationContext()).getUserUuid();
        WifiDocDialogFragment fragment = WifiDocDialogFragment.forSimpleFeedback(userUuid);
        fragment.show(getActivity().getSupportFragmentManager(), "Feedback");
        dobbyAnalytics.feedbackFormShown();
    }

    private void cancelTests() {
        dobbyAnalytics.testsCancelled();
        showStatusMessage(R.string.status_cancelling_tests);
        if (mListener != null) {
            mListener.cancelTests();
        }
        showStatusMessage(R.string.status_tests_cancelled);
        sendSwitchStateMessage(UI_STATE_INIT_AND_READY);
        resetData();
    }

    private void shareResults() {
        if (uiState != UI_STATE_READY_WITH_RESULTS) {
            Snackbar.make(getView(), "No results available for sharing !", Snackbar.LENGTH_SHORT).show();
            return;
        }
        String htmlText = HtmlReportGenerator.createHtmlFor(getContext(), testServerLocation, testServerLatencyMs,
                routerIpString, ispNameString, uploadBw, downloadBw,  pingGrade, wifiGrade);
        Intent shareIntent = ShareCompat.IntentBuilder.from(getActivity())
                .setType("text/html")
                .setHtmlText(htmlText)
                .setSubject("Speed test results by WifiTester.")
                .getIntent();
        startActivity(shareIntent);
    }
}
