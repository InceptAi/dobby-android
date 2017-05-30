package com.inceptai.dobby.ui;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.inceptai.dobby.R;
import com.inceptai.dobby.ai.DataInterpreter;
import com.inceptai.dobby.ai.RtDataSource;
import com.inceptai.dobby.ai.UserResponse;
import com.inceptai.dobby.model.BandwidthStats;
import com.inceptai.dobby.speedtest.BandwidthObserver;
import com.inceptai.dobby.speedtest.BandwithTestCodes;
import com.inceptai.dobby.speedtest.NewBandwidthAnalyzer;
import com.inceptai.dobby.speedtest.ServerInformation;
import com.inceptai.dobby.speedtest.SpeedTestConfig;
import com.inceptai.dobby.utils.DobbyLog;
import com.inceptai.dobby.utils.Utils;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import static com.inceptai.dobby.utils.Utils.ZERO_POINT_ZERO;
import static com.inceptai.dobby.utils.Utils.nonLinearBwScale;

/**
 * Fragment shows the UI for the chat-based interaction with the AI agent.
 * Use the {@link ChatFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChatFragment extends Fragment implements Handler.Callback, NewBandwidthAnalyzer.ResultsCallback {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private static final int UI_STATE_FULL_CHAT = 1001;
    private static final int UI_STATE_SHOW_BW_GAUGE = 1002;

    public static final String FRAGMENT_TAG = "ChatFragment";

    // Handler message types.
    private static final int MSG_SHOW_DOBBY_CHAT = 1;
    private static final int MSG_SHOW_RT_GRAPH = 2;
    private static final int MSG_SHOW_BW_GAUGE = 3;
    private static final int MSG_UPDATE_CIRCULAR_GAUGE = 4;
    private static final int MSG_UI_STATE_CHANGE = 5;
    private static final int MSG_SHOW_USER_ACTION_BUTTONS = 6;
    private static final int MSG_SHOW_BANDWIDTH_RESULT_CARDVIEW = 7;
    private static final int MSG_SHOW_STATUS = 8;
    private static final int MSG_SHOW_OVERALL_NETWORK_STATUS = 9;


    private static final int BW_TEST_INITIATED = 200;
    private static final int BW_CONFIG_FETCHED = 201;
    private static final int BW_UPLOAD_RUNNING = 202;
    private static final int BW_DOWNLOAD_RUNNING = 203;
    private static final int BW_SERVER_INFO_FETCHED = 204;
    private static final int BW_BEST_SERVER_DETERMINED = 205;
    private static final int BW_IDLE = 207;


    private static final int MIN_DELAY_BOT_REPLY_MS = 500;
    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private RecyclerView chatRv;
    private ChatRecyclerViewAdapter recyclerViewAdapter;
    private EditText queryEditText;
    private ImageView micButtonIv;
    private OnFragmentInteractionListener mListener;
    private Handler handler;
    private LinearLayout bwGaugeLayout;
    private LinearLayout actionMenu;

    private CircularGauge downloadCircularGauge;
    private TextView downloadGaugeTv;
    private TextView downloadGaugeTitleTv;

    private CircularGauge uploadCircularGauge;
    private TextView uploadGaugeTv;
    private TextView uploadGaugeTitleTv;

    private TextToSpeech textToSpeech;

    private boolean useVoiceOutput = false;

    private int uiState = UI_STATE_FULL_CHAT;

    private int bwTestState = BW_IDLE;

    /**
     * Interface for parent activities to implement.
     */
    public interface OnFragmentInteractionListener {

        /**
         * Called when user enters a text.
         * @param text
         */
        void onUserQuery(String text);
        void onMicPressed();
        void onRecyclerViewReady();
    }

    public ChatFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ChatFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ChatFragment newInstance(String param1, String param2) {
        ChatFragment fragment = new ChatFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        DobbyLog.v("CF: onCreateView started");
        // Inflate the layout for this fragment
        View fragmentView = inflater.inflate(R.layout.fragment_chat, container, false);
        setupTextToSpeech();

        chatRv = (RecyclerView) fragmentView.findViewById(R.id.chatRv);
        recyclerViewAdapter = new ChatRecyclerViewAdapter(this.getContext(), new LinkedList<ChatEntry>());
        chatRv.setAdapter(recyclerViewAdapter);
        chatRv.setLayoutManager(new LinearLayoutManager(this.getContext()));
        /*
        LinearLayoutManager linearLayoutManager = (LinearLayoutManager)chatRv.getLayoutManager();
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        */

        handler = new Handler(this);
        bwGaugeLayout = (LinearLayout) fragmentView.findViewById(R.id.bw_gauge_ll);
        actionMenu = (LinearLayout) fragmentView.findViewById(R.id.action_menu);

        queryEditText = (EditText) fragmentView.findViewById(R.id.queryEditText);
        queryEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                String text = queryEditText.getText().toString();
                DobbyLog.i("Action ID: " + actionId);
                if (event != null) {
                    DobbyLog.i("key event: " + event.toString());
                }
                if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    DobbyLog.i("ENTER 1");
                    processTextQuery(text);
                } else if (actionId == EditorInfo.IME_ACTION_DONE ||
                        actionId == EditorInfo.IME_ACTION_GO ||
                        actionId == EditorInfo.IME_ACTION_NEXT) {
                    DobbyLog.i("ENTER 2");
                    processTextQuery(text);
                }
                queryEditText.getText().clear();
                return false;
            }
        });



        micButtonIv = (ImageView) fragmentView.findViewById(R.id.micButtonIv);
        micButtonIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onMicPressed();
                } else {
                    Toast.makeText(getContext(), "Mic not supported yet !", Toast.LENGTH_LONG).show();
                }
            }
        });

        View downloadView = fragmentView.findViewById(R.id.cg_download_test);
        downloadCircularGauge = (CircularGauge) downloadView.findViewById(R.id.bw_gauge);
        downloadGaugeTv = (TextView) downloadView.findViewById(R.id.gauge_tv);
        downloadGaugeTitleTv = (TextView) downloadView.findViewById(R.id.title_tv);
        downloadGaugeTitleTv.setText(R.string.download_bw);

        View uploadView = fragmentView.findViewById(R.id.cg_upload_test);
        uploadCircularGauge = (CircularGauge) uploadView.findViewById(R.id.bw_gauge);
        uploadGaugeTv = (TextView) uploadView.findViewById(R.id.gauge_tv);
        uploadGaugeTitleTv = (TextView) uploadView.findViewById(R.id.title_tv);
        uploadGaugeTitleTv.setText(R.string.upload_bw);

        if (mListener != null) {
            mListener.onRecyclerViewReady();
        }

        DobbyLog.v("CF: Finished with onCreateView");
        return fragmentView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            // mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
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
        mListener = null;
    }

    public void addUserChat(String text) {
        ChatEntry chatEntry = new ChatEntry(text.trim(), ChatEntry.USER_CHAT);
        recyclerViewAdapter.addEntryAtBottom(chatEntry);
        chatRv.scrollToPosition(recyclerViewAdapter.getItemCount() - 1);
    }

    public void addBandwidthResultsCardView(DataInterpreter.BandwidthGrade bandwidthGrade) {
        Message.obtain(handler, MSG_SHOW_BANDWIDTH_RESULT_CARDVIEW, bandwidthGrade).sendToTarget();
    }

    public void addOverallNetworkResultsCardView(DataInterpreter.WifiGrade wifiGrade, String ispName, String externalIp) {
        Message.obtain(handler, MSG_SHOW_OVERALL_NETWORK_STATUS, new OverallNetworkInfo(wifiGrade, ispName, externalIp)).sendToTarget();
    }

    public void addPingResultsCardview(DataInterpreter.PingGrade pingGrade) {
        ChatEntry chatEntry = new ChatEntry(Utils.EMPTY_STRING, ChatEntry.PING_RESULTS_CARDVIEW);
        chatEntry.setPingGrade(pingGrade);
        recyclerViewAdapter.addEntryAtBottom(chatEntry);
        chatRv.scrollToPosition(recyclerViewAdapter.getItemCount() - 1);
    }

    public void showNetworkResultsCardView(DataInterpreter.WifiGrade wifiGrade, String ispName, String routerIp) {
        ChatEntry chatEntry = new ChatEntry(Utils.EMPTY_STRING, ChatEntry.OVERALL_NETWORK_CARDVIEW);
        chatEntry.setWifiGrade(wifiGrade);
        chatEntry.setIspName(ispName);
        chatEntry.setRouterIp(routerIp);
        recyclerViewAdapter.addEntryAtBottom(chatEntry);
        chatRv.scrollToPosition(recyclerViewAdapter.getItemCount() - 1);
    }

    public void observeBandwidthNonUi(BandwidthObserver observer) {
        Message.obtain(handler, MSG_SHOW_BW_GAUGE, observer).sendToTarget();
    }

    public void showResponse(String text) {
        DobbyLog.v("ChatF: showResponse text " + text);
        Message.obtain(handler, MSG_SHOW_DOBBY_CHAT, text).sendToTarget();
    }

    public void showRtGraph(RtDataSource<Float, Integer> rtDataSource) {
        Message.obtain(handler, MSG_SHOW_RT_GRAPH, rtDataSource).sendToTarget();
    }

    public void cancelTests() {
        //resetData();
        //uiStateChange(UI_STATE_FULL_CHAT);
        dismissBandwidthGaugeNonUi();
    }

    public void showUserActionOptions(List<Integer> userResponseTypes) {
        DobbyLog.v("In showUserActionOptions of CF: responseTypes: " + userResponseTypes);
        //Show all the buttons programatically and tie the response to send the user query
        Message.obtain(handler, MSG_SHOW_USER_ACTION_BUTTONS, userResponseTypes).sendToTarget();
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_SHOW_DOBBY_CHAT:
                // Add to the recycler view.
                DobbyLog.v("In handleMessage for DobbyChat");
                String text = (String) msg.obj;
                addDobbyChat(text, false);
                break;
            case MSG_SHOW_STATUS:
                // Add to the recycler view.
                DobbyLog.v("In handleMessage for DobbyChat");
                String status = (String) msg.obj;
                addDobbyChat(status, true);
                break;
            case MSG_SHOW_RT_GRAPH:
                RtDataSource<Float, Integer> rtDataSource = (RtDataSource<Float, Integer>) msg.obj;
                addRtGraph(rtDataSource);
                break;
            case MSG_SHOW_BW_GAUGE:
                showBandwidthGauge((BandwidthObserver) msg.obj);
                break;
            case MSG_UPDATE_CIRCULAR_GAUGE:
                updateBandwidthGauge(msg);
                break;
            case MSG_UI_STATE_CHANGE:
                uiStateChange((int)msg.obj);
                break;
            case MSG_SHOW_USER_ACTION_BUTTONS:
                showUserActionButtons((List<Integer>) msg.obj);
                break;
            case MSG_SHOW_BANDWIDTH_RESULT_CARDVIEW:
                DataInterpreter.BandwidthGrade bandwidthGrade = (DataInterpreter.BandwidthGrade) msg.obj;
                addDobbyChat(getString(R.string.bandwidth_card_view_message), false);
                showBandwidthResultsCardView(bandwidthGrade.getUploadMbps(), bandwidthGrade.getDownloadMbps());
                break;
            case MSG_SHOW_OVERALL_NETWORK_STATUS:
                OverallNetworkInfo overallNetworkInfo = (OverallNetworkInfo) msg.obj;
                if (!(overallNetworkInfo.getWifiGrade().isWifiDisconnected() || overallNetworkInfo.getWifiGrade().isWifiOff())) {
                    addDobbyChat(getString(R.string.wifi_status_view_message), false);
                    showNetworkResultsCardView(overallNetworkInfo.getWifiGrade(),
                            overallNetworkInfo.getIsp(), overallNetworkInfo.getIp());
                }
                addDobbyChat(overallNetworkInfo.getWifiGrade().userReadableInterpretation(), false);
                break;
        }
        return false;
    }

    public void dismissBandwidthGaugeNonUi() {
        uiStateChangeNonUi(UI_STATE_FULL_CHAT);
    }

    public void addSpokenText(String userText) {
        addUserChat(userText);
        if (textToSpeech != null) {
            useVoiceOutput = true;
        }
    }

    private void showBandwidthResultsCardView(double uploadMbps, double downloadMbps) {
        ChatEntry chatEntry = new ChatEntry(Utils.EMPTY_STRING, ChatEntry.BW_RESULTS_GAUGE_CARDVIEW);
        chatEntry.setBandwidthResults(uploadMbps, downloadMbps);
        recyclerViewAdapter.addEntryAtBottom(chatEntry);
        chatRv.scrollToPosition(recyclerViewAdapter.getItemCount() - 1);
    }

    private void makeUiChanges(View rootView) {
        if (uiState == UI_STATE_FULL_CHAT) {
            // make guage gone.
            resetData();
            bwGaugeLayout.setVisibility(View.GONE);
        } else if (uiState == UI_STATE_SHOW_BW_GAUGE) {
            bwGaugeLayout.setVisibility(View.VISIBLE);
        }
        if (rootView != null) {
            rootView.requestLayout();
        }
        chatRv.requestLayout();
    }

    private void speak(String text) {
        if (textToSpeech != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
            }else{
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
            }
        }
    }

    private void setupTextToSpeech() {
        textToSpeech = new TextToSpeech(getContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    int result = textToSpeech.setLanguage(Locale.US);
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        DobbyLog.e("TTS: Locale language is not supported");
                        textToSpeech = null;
                    }
                } else {
                    DobbyLog.e("TTS Initilization Failed!");
                    textToSpeech = null;
                }
            }
        });
    }

    public void addDobbyChat(String text, boolean isStatusMessage) {
        DobbyLog.i("Adding dobby chat: " + text);
        ChatEntry chatEntry = new ChatEntry(text.trim(), ChatEntry.DOBBY_CHAT, isStatusMessage);
        recyclerViewAdapter.addEntryAtBottom(chatEntry);
        chatRv.scrollToPosition(recyclerViewAdapter.getItemCount() - 1);
        if (useVoiceOutput) {
            speak(text);
        }
    }

    private void showBandwidthGauge(BandwidthObserver observer) {
        observer.registerCallback(this);
        uiStateChange(UI_STATE_SHOW_BW_GAUGE);
    }


    private void showUserActionButtons(List<Integer> userResponseTypes) {
        actionMenu.removeAllViewsInLayout();
        for (final int userResponseType: userResponseTypes) {
            final String buttonText = UserResponse.getStringForResponseType(userResponseType);
            if (buttonText == null || buttonText.equals(Utils.EMPTY_STRING)) {
                continue;
            }
            Button button = new Button(this.getContext(), null, android.R.attr.buttonStyleSmall);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(10, 0, 10, 10);
            button.setLayoutParams(params);
            button.setTextColor(Color.LTGRAY); // light gray
            button.setText(buttonText);
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            //button.setMinHeight((int)Utils.convertPixelsToDp(10, this.getContext())); // In pixels
            //button.setTextColor(getResources().getColor(R.color.basicRed));
            button.setMinHeight(0);
            button.setMinWidth(0);
            button.setClickable(true);
            button.setAllCaps(false);
            button.setBackgroundResource(R.drawable.rounded_shape_action_button);
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    processTextQuery(buttonText);
                }
            });
            actionMenu.addView(button);
        }
    }

    private void processTextQuery(String text) {
        if (text.length() < 2) {
            return;
        }
        addUserChat(text);
        useVoiceOutput = false;
        // Parent activity callback.
        if (mListener != null) {
            mListener.onUserQuery(text);
        }
    }

    private void uiStateChangeNonUi(int newState) {
        Message.obtain(handler, MSG_UI_STATE_CHANGE, newState).sendToTarget();
    }

    private void uiStateChange(int newState) {
        uiState = newState;
        makeUiChanges(getView());
    }

    private void addRtGraph(RtDataSource<Float, Integer> rtDataSource) {
        DobbyLog.i("Adding GRAPH entry.");
        ChatEntry entry = new ChatEntry(Utils.EMPTY_STRING, ChatEntry.RT_GRAPH);
        entry.addGraph(new GraphData<Float, Integer>(rtDataSource, BandwithTestCodes.TestMode.DOWNLOAD));
        recyclerViewAdapter.addEntryAtBottom(entry);
        chatRv.scrollToPosition(recyclerViewAdapter.getItemCount() - 1);
    }

    private int getBwTestState() {
        return bwTestState;
    }

    private void setBwTestState(int state) {
        bwTestState = state;
    }

    private void showStatus(int resourceId) {
        String message = getResources().getString(resourceId);
        showStatus(message);
    }

    private void showStatus(String message) {
        DobbyLog.v("ChatF: showResponse text " + message);
        Message.obtain(handler, MSG_SHOW_STATUS, message).sendToTarget();
    }

    private void updateBandwidthGauge(Message msg) {
        Utils.BandwidthValue bandwidthValue = (Utils.BandwidthValue) msg.obj;
        if (bandwidthValue.mode == BandwithTestCodes.TestMode.UPLOAD) {
            uploadCircularGauge.setValue((int) nonLinearBwScale(bandwidthValue.value));
            uploadGaugeTv.setText(String.format("%2.2f", bandwidthValue.value));
        } else if (bandwidthValue.mode == BandwithTestCodes.TestMode.DOWNLOAD) {
            downloadCircularGauge.setValue((int) nonLinearBwScale(bandwidthValue.value));
            downloadGaugeTv.setText(String.format("%2.2f", bandwidthValue.value));
        }
    }

    private void resetData() {
        uploadCircularGauge.setValue(0);
        downloadCircularGauge.setValue(0);
        uploadGaugeTv.setText(ZERO_POINT_ZERO);
        downloadGaugeTv.setText(ZERO_POINT_ZERO);
    }

    //  NewBandwidthAnalyzer.ResultCallback methods -------
    @Override
    public void onConfigFetch(SpeedTestConfig config) {
        if (getBwTestState() == BW_TEST_INITIATED) {
            showStatus(R.string.status_fetching_server_list);
        }
        setBwTestState(BW_CONFIG_FETCHED);
        DobbyLog.v("WifiDoc: Fetched config");
    }

    @Override
    public void onServerInformationFetch(ServerInformation serverInformation) {
        if (getBwTestState() == BW_CONFIG_FETCHED) {
            String constructedString = getResources().getString(R.string.status_closest_servers, serverInformation.serverList.size());
            showStatus(constructedString);
        }
        setBwTestState(BW_SERVER_INFO_FETCHED);
        DobbyLog.v("WifiExpert: Fetched server info");
    }

    @Override
    public void onClosestServersSelected(List<ServerInformation.ServerDetails> closestServers) {
        DobbyLog.v("WifiExpert Closest servers");
    }

    @Override
    public void onBestServerSelected(ServerInformation.ServerDetails bestServer) {
        if (getBwTestState() == BW_SERVER_INFO_FETCHED) {
            String constructedMessage = getResources().getString(R.string.status_found_closest_server, bestServer.name, bestServer.latencyMs);
            showStatus(constructedMessage);
        }
        setBwTestState(BW_BEST_SERVER_DETERMINED);
        DobbyLog.v("WifiExpert: Best server");
    }

    @Override
    public void onTestFinished(@BandwithTestCodes.TestMode int testMode, BandwidthStats stats) {
        Message.obtain(handler, MSG_UPDATE_CIRCULAR_GAUGE, Utils.BandwidthValue.from(testMode, (stats.getOverallBandwidth() / 1.0e6))).sendToTarget();
        if (testMode == BandwithTestCodes.TestMode.UPLOAD) {
            showStatus(R.string.status_finished_bw_tests);
            dismissBandwidthGaugeNonUi();
        }
    }

    @Override
    public void onTestProgress(@BandwithTestCodes.TestMode int testMode, double instantBandwidth) {
        if (testMode == BandwithTestCodes.TestMode.DOWNLOAD && getBwTestState() != BW_DOWNLOAD_RUNNING) {
            setBwTestState(BW_DOWNLOAD_RUNNING);
            showStatus(R.string.status_running_download_tests);
        } else if (testMode == BandwithTestCodes.TestMode.UPLOAD && getBwTestState() != BW_UPLOAD_RUNNING) {
            setBwTestState(BW_UPLOAD_RUNNING);
            showStatus(R.string.status_running_upload_tests);
        }
        Message.obtain(handler, MSG_UPDATE_CIRCULAR_GAUGE, Utils.BandwidthValue.from(testMode, (instantBandwidth / 1.0e6))).sendToTarget();
    }

    @Override
    public void onBandwidthTestError(@BandwithTestCodes.TestMode int testMode, @BandwithTestCodes.ErrorCodes int errorCode, @Nullable String errorMessage) {
        showStatus(R.string.status_error_bw_tests);
        dismissBandwidthGaugeNonUi();
    }

    private class OverallNetworkInfo {
        public DataInterpreter.WifiGrade wifiGrade;
        public String isp;
        public String ip;

        public OverallNetworkInfo(DataInterpreter.WifiGrade wifiGrade, String isp, String ip) {
            this.wifiGrade = wifiGrade;
            this.isp = isp;
            this.ip = ip;
        }

        public DataInterpreter.WifiGrade getWifiGrade() {
            return wifiGrade;
        }

        public String getIsp() {
            return isp;
        }

        public String getIp() {
            return ip;
        }
    }
}
