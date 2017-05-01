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
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.jorgecastilloprz.FABProgressCircle;
import com.google.common.eventbus.Subscribe;
import com.inceptai.dobby.R;
import com.inceptai.dobby.ai.DataInterpreter;
import com.inceptai.dobby.ai.SuggestionCreator;
import com.inceptai.dobby.eventbus.DobbyEvent;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.model.BandwidthStats;
import com.inceptai.dobby.speedtest.BandwidthObserver;
import com.inceptai.dobby.speedtest.BandwithTestCodes;
import com.inceptai.dobby.speedtest.NewBandwidthAnalyzer;
import com.inceptai.dobby.speedtest.ServerInformation;
import com.inceptai.dobby.speedtest.SpeedTestConfig;
import com.inceptai.dobby.utils.DobbyLog;
import com.inceptai.dobby.utils.Utils;

import java.util.List;

import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;

import static com.inceptai.dobby.ai.DataInterpreter.MetricType.ABYSMAL;
import static com.inceptai.dobby.ai.DataInterpreter.MetricType.AVERAGE;
import static com.inceptai.dobby.ai.DataInterpreter.MetricType.EXCELLENT;
import static com.inceptai.dobby.ai.DataInterpreter.MetricType.GOOD;
import static com.inceptai.dobby.ai.DataInterpreter.MetricType.POOR;

public class WifiDocMainFragment extends Fragment implements View.OnClickListener, NewBandwidthAnalyzer.ResultsCallback, Handler.Callback{
    public static final String TAG = "WifiDocMainFragment";
    private static final String ZERO_POINT_ZERO = "0.0";
    private static final int PERMISSION_COARSE_LOCATION_REQUEST_CODE = 101;
    private static final String ARG_PARAM1 = "param1";
    private static final int MSG_UPDATED_CIRCULAR_GAUGE = 1001;
    private static final int MSG_WIFI_GRADE_AVAILABLE = 1002;
    private static final int MSG_PING_GRADE_AVAILABLE = 1003;
    private static final int MSG_SUGGESTION_AVAILABLE = 1004;
    private static final int MSG_SHOW_STATUS = 1005;
    private static final int MSG_SWITCH_STATE = 1006;
    private static final long SUGGESTION_FRESHNESS_TS_MS = 30000; // 30 seconds

    private static final int UI_STATE_INIT_AND_READY = 101; // Ready to run tests. Initial state.
    private static final int UI_STATE_RUNNING_TESTS = 102; // Running tests.
    private static final int UI_STATE_READY_WITH_RESULTS = 103;
    private int uiState = UI_STATE_INIT_AND_READY;

    private static final int BW_CONFIG_FETCH = 201;
    private static final int BW_UPLOAD = 202;
    private static final int BW_DOWNLOAD = 203;
    private static final int BW_IDLE = 204;
    private int bwTestState = BW_IDLE;

    private OnFragmentInteractionListener mListener;

    private FloatingActionButton mainFab;
    private FABProgressCircle fabProgressCircle;

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

    private TextView suggestionsValueTv;

    private String mParam1;
    private DobbyEventBus eventBus;
    private BandwidthObserver bandwidthObserver;
    private Handler handler;

    private SuggestionCreator.Suggestion currentSuggestion;
    MaterialTapTargetPrompt fabPrompt;

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
        View view = inflater.inflate(R.layout.fragment_wifi_doc_main_2, container, false);

        fetchViewInstances(view);
        resetData();
        showTapOnboarding(view);
        // requestPermissions();
        uiStateVisibilityChanges();
        return view;
    }

    private void uiStateVisibilityChanges() {
        if (uiState == UI_STATE_INIT_AND_READY) {
            yourNetworkCv.setVisibility(View.INVISIBLE);
            pingCv.setVisibility(View.INVISIBLE);
            statusCv.setVisibility(View.VISIBLE);
        } else if (uiState == UI_STATE_READY_WITH_RESULTS) {
            yourNetworkCv.setVisibility(View.VISIBLE);
            pingCv.setVisibility(View.VISIBLE);
            statusCv.setVisibility(View.INVISIBLE);
        } else if (uiState == UI_STATE_RUNNING_TESTS) {
            yourNetworkCv.setVisibility(View.INVISIBLE);
            pingCv.setVisibility(View.INVISIBLE);
            statusCv.setVisibility(View.VISIBLE);
        }
    }

    private void showTapOnboarding(View rootView) {
        fabPrompt = new MaterialTapTargetPrompt.Builder(getActivity())
                .setTarget(rootView.findViewById(R.id.main_fab_button))
                .setFocalToTextPadding(R.dimen.dp40)
                .setPrimaryText("Run some tests")
                .setSecondaryText("Tap the button to let Wifi Tester run some tests.")
                .setAnimationInterpolator(new FastOutSlowInInterpolator())
                .setBackgroundColourFromRes(R.color.orangeMediumLight1)
                .setOnHidePromptListener(new MaterialTapTargetPrompt.OnHidePromptListener()
                {
                    @Override
                    public void onHidePrompt(MotionEvent event, boolean tappedTarget)
                    {
                        fabPrompt = null;
                        //Do something such as storing a value so that this prompt is never shown again
                    }

                    @Override
                    public void onHidePromptComplete()
                    {
                        // Run tests.
                    }
                })
                .create();
        fabPrompt.show();
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
        if (uiState == UI_STATE_RUNNING_TESTS) {
            Toast.makeText(getContext(), "Tests already running.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (mListener != null) {
            if (uiState == UI_STATE_INIT_AND_READY) {
                showStatusMessage("Starting bandwidth test ...");
                sendSwitchStateMessage(UI_STATE_RUNNING_TESTS);
            }
            mListener.onMainButtonClick();
        }
        resetData();
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
                    DobbyLog.i("Coarse location permission granted.");
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
        } else if (event.getEventType() == DobbyEvent.EventType.SUGGESTIONS_AVAILABLE) {
            Message.obtain(handler, MSG_SUGGESTION_AVAILABLE, event.getPayload()).sendToTarget();
        }
    }

    @Override
    public void onConfigFetch(SpeedTestConfig config) {
        bwTestState = BW_CONFIG_FETCH;
        showStatusMessage("Finding closest server ...");
    }

    @Override
    public void onServerInformationFetch(ServerInformation serverInformation) {

    }

    @Override
    public void onClosestServersSelected(List<ServerInformation.ServerDetails> closestServers) {
        showStatusMessage("Starting Download test ..");
    }

    @Override
    public void onBestServerSelected(ServerInformation.ServerDetails bestServer) {

    }

    @Override
    public void onTestFinished(@BandwithTestCodes.TestMode int testMode, BandwidthStats stats) {
        Message.obtain(handler, MSG_UPDATED_CIRCULAR_GAUGE, BandwidthValue.from(testMode, (stats.getOverallBandwidth() / 1.0e6))).sendToTarget();
        if (testMode == BandwithTestCodes.TestMode.UPLOAD) {
            showStatusMessage("Finished bandwidth tests.");
            sendSwitchStateMessage(UI_STATE_READY_WITH_RESULTS);
        }
    }

    @Override
    public void onTestProgress(@BandwithTestCodes.TestMode int testMode, double instantBandwidth) {
        if (testMode == BandwithTestCodes.TestMode.DOWNLOAD && bwTestState != BW_DOWNLOAD) {
            bwTestState = BW_DOWNLOAD;
            showStatusMessage("Running Download test ...");
        } else if (testMode == BandwithTestCodes.TestMode.UPLOAD && bwTestState != BW_UPLOAD) {
            bwTestState = BW_UPLOAD;
            showStatusMessage("Running Upload test ...");
        }
        Message.obtain(handler, MSG_UPDATED_CIRCULAR_GAUGE, BandwidthValue.from(testMode, (instantBandwidth / 1.0e6))).sendToTarget();
    }

    @Override
    public void onBandwidthTestError(@BandwithTestCodes.TestMode int testMode, @BandwithTestCodes.ErrorCodes int errorCode, @Nullable String errorMessage) {

    }

    @Override
    public boolean handleMessage(Message msg) {
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
                // TODO Animate this if needed.
                String message = (String) msg.obj;
                statusTv.setText(message);
                break;
            case MSG_SWITCH_STATE:
                switchState(msg.arg1);
                break;
            default:
                return false;
        }
        return true;
    }

    private void showWifiResults(DataInterpreter.WifiGrade wifiGrade) {
        String ssid = wifiGrade.getPrimaryApSsid();
        if (ssid != null && !ssid.isEmpty()) {
            if (ssid.length() > 10) {
                ssid = ssid.substring(0, 10);
            }
            wifiSsidTv.setText(ssid);
        }
        setWifiResult(wifiSignalValueTv, String.valueOf(wifiGrade.getPrimaryApSignal()),
                wifiSignalIconIv, wifiGrade.getPrimaryApSignalMetric());
        // String availability = String.format("%2.1f", 100.0 *(wifiGradeJson.getPrimaryLinkCongestionPercentage()));
        // setWifiResult(wifiCongestionValueTv, availability, wifiCongestionIconIv, wifiGradeJson.getPrimaryLinkChannelOccupancyMetric());
    }

    private void showPingResults(DataInterpreter.PingGrade pingGrade) {
        DobbyLog.i("Ping grade available.");
        if (pingGrade == null) {
            DobbyLog.w("Null ping grade.");
            return;
        }

        setPingResult(pingRouterValueTv, String.format("%2.1f", pingGrade.getRouterLatencyMs()),
                pingRouterGradeIv, pingGrade.getRouterLatencyMetric());

        setPingResult(pingDnsPrimaryValueTv, String.format("%2.1f", pingGrade.getDnsServerLatencyMs()),
                pingDnsPrimaryGradeIv, pingGrade.getDnsServerLatencyMetric());

        setPingResult(pingDnsSecondValueTv,  String.format("%2.1f", pingGrade.getAlternativeDnsLatencyMs()),
                pingDnsSecondGradeIv, pingGrade.getAlternativeDnsMetric());

        setPingResult(pingWebValueTv, String.format("%2.2f", pingGrade.getExternalServerLatencyMs()),
                pingWebGradeIv, pingGrade.getExternalServerLatencyMetric());
    }

    private void showSuggestion(SuggestionCreator.Suggestion suggestion) {
        sendSwitchStateMessage(UI_STATE_READY_WITH_RESULTS);
        if (suggestion == null) {
            DobbyLog.w("Null suggestion received from eventbus.");
            return;
        }

        if (currentSuggestion != null && isSuggestionFresh(currentSuggestion)) {
            DobbyLog.i("Already have a fresh enough suggestion, ignoring new suggestion");
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(suggestion.getTitle() + "\n");
        for(String line : suggestion.getShortSuggestionList()) {
            stringBuilder.append(line + "\n");
        }
        // suggestionsValueTv.setText(stringBuilder.toString());
        DobbyLog.i("Received suggestions:" + stringBuilder.toString());
    }


    private static boolean isSuggestionFresh(SuggestionCreator.Suggestion suggestion) {
        return (System.currentTimeMillis() - suggestion.getCreationTimestampMs()) < SUGGESTION_FRESHNESS_TS_MS;
    }

    private void fetchViewInstances(View rootView) {
        mainFab = (FloatingActionButton) rootView.findViewById(R.id.main_fab_button);
        mainFab.setOnClickListener(this);

        fabProgressCircle = (FABProgressCircle) rootView.findViewById(R.id.fabProgressCircle);

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

        // suggestionsValueTv = (TextView)rootView.findViewById(R.id.suggestion_value_tv);
    }

    private void updateBandwidthGauge(Message msg) {
        BandwidthValue bandwidthValue = (BandwidthValue) msg.obj;
        if (bandwidthValue.mode == BandwithTestCodes.TestMode.UPLOAD) {
            uploadCircularGauge.setValue((int) nonLinearBwScale(bandwidthValue.value));
            uploadGaugeTv.setText(String.format("%2.2f", bandwidthValue.value));
        } else if (bandwidthValue.mode == BandwithTestCodes.TestMode.DOWNLOAD) {
            downloadCircularGauge.setValue((int) nonLinearBwScale(bandwidthValue.value));
            downloadGaugeTv.setText(String.format("%2.2f", bandwidthValue.value));
        }
    }

    private static class BandwidthValue {
        @BandwithTestCodes.TestMode
        int mode;
        double value;
        static BandwidthValue from(int mode, double value) {
            BandwidthValue bandwidthValue = new BandwidthValue();
            bandwidthValue.mode = mode;
            bandwidthValue.value = value;
            return bandwidthValue;
        }
    }

    private static double nonLinearBwScale(double input) {
        // 0 .. 5 maps to 0 .. 50
        if (input <= 5.0) {
            return input * 10.;
        }
        // 5 to 10 maps to 50 .. 62.5
        if (input <= 10.0) {
            return  12.5 * (input - 5.0)  / 5.0 + 50.0;
        }

        // 10 to 20 maps to 62.5 .. 75.
        if (input < 20.0) {
            return 12.5 * (input - 10.0) / 10.0 + 62.5;
        }

        // 20 to 50 maps to 75 to 87.5
        if (input < 50.0) {
            return 12.5 * (input - 20.0) / 30.0 + 75;
        }

        // Upper bound by 100
        input = Math.min(100.0, input);
        // 50 to 100 maps to 87.5 to 100
        return 12.5 * (input - 50.0) / 50.0 + 87.5;
    }

    private void setPingResult(TextView valueTv, String value, ImageView gradeIv, @DataInterpreter.MetricType int grade) {
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
            default:
                setImage(gradeIv, R.drawable.non_functional);
                break;

        }
    }

    private void setWifiResult(TextView valueTv, String value, ImageView gradeIv, @DataInterpreter.MetricType int grade) {
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
    }

    private void setImage(ImageView view, int resourceId) {
        Utils.setImage(getContext(), view, resourceId);
    }

    private void showStatusMessage(String message) {
        Message msg = Message.obtain(handler, MSG_SHOW_STATUS, message);
        handler.sendMessageDelayed(msg, 500);
    }

    private void sendSwitchStateMessage(int newState) {
        Message.obtain(handler, MSG_SWITCH_STATE, newState, 0).sendToTarget();
    }

    private void switchState(int newState) {
        if ((uiState == UI_STATE_INIT_AND_READY || uiState == UI_STATE_READY_WITH_RESULTS )
                && newState == UI_STATE_RUNNING_TESTS) {
            // Tests starting.
            fabProgressCircle.show();
        }

        if (uiState == UI_STATE_RUNNING_TESTS && newState == UI_STATE_READY_WITH_RESULTS) {
            fabProgressCircle.beginFinalAnimation();
        }
        uiState = newState;
        if (newState == UI_STATE_RUNNING_TESTS) {
            statusTv.setText(R.string.running_tests);
        }
        uiStateVisibilityChanges();
    }
}
