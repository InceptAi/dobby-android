package com.inceptai.dobby.ui;

import android.content.Context;
import android.graphics.Color;
import android.net.wifi.WifiInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.R;
import com.inceptai.dobby.ai.DataInterpreter;
import com.inceptai.dobby.ai.RtDataSource;
import com.inceptai.dobby.ai.SuggestionCreator;
import com.inceptai.dobby.ai.UserResponse;
import com.inceptai.dobby.analytics.DobbyAnalytics;
import com.inceptai.dobby.model.BandwidthStats;
import com.inceptai.dobby.speedtest.BandwidthObserver;
import com.inceptai.dobby.speedtest.BandwidthTestCodes;
import com.inceptai.dobby.speedtest.NewBandwidthAnalyzer;
import com.inceptai.dobby.speedtest.ServerInformation;
import com.inceptai.dobby.speedtest.SpeedTestConfig;
import com.inceptai.dobby.utils.DobbyLog;
import com.inceptai.dobby.utils.Utils;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import static com.inceptai.dobby.ai.UserResponse.ResponseType.CANCEL;
import static com.inceptai.dobby.ai.UserResponse.ResponseType.CONTACT_HUMAN_EXPERT;
import static com.inceptai.dobby.ai.UserResponse.ResponseType.LIST_ALL_FUNCTIONS;
import static com.inceptai.dobby.ai.UserResponse.ResponseType.RUN_ALL_DIAGNOSTICS;
import static com.inceptai.dobby.ai.UserResponse.ResponseType.RUN_BW_TESTS;
import static com.inceptai.dobby.ai.UserResponse.ResponseType.RUN_WIFI_TESTS;
import static com.inceptai.dobby.ai.UserResponse.ResponseType.SHOW_LAST_SUGGESTION_DETAILS;
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

    public static final String FRAGMENT_TAG = "DobbyChatFragment";

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
    private static final int MSG_SHOW_DETAILED_SUGGESTIONS = 10;
    private static final int MSG_SHOW_EXPERT_CHAT = 11;
    private static final int MSG_SHOW_EXPERT_INDICATOR = 12;
    private static final int MSG_HIDE_EXPERT_INDICATOR = 13;
    private static final int MSG_SHOW_USER_CHAT = 14;
    private static final int MSG_SHOW_REPAIR_STATUS = 15;


    private static final int BW_TEST_INITIATED = 200;
    private static final int BW_CONFIG_FETCHED = 201;
    private static final int BW_UPLOAD_RUNNING = 202;
    private static final int BW_DOWNLOAD_RUNNING = 203;
    private static final int BW_SERVER_INFO_FETCHED = 204;
    private static final int BW_BEST_SERVER_DETERMINED = 205;
    private static final int BW_IDLE = 207;

    // TODO: Rename and change types of parameters
    private long botMessageDelay = 0;
    private long lastBotMessageScheduledAt = 0;
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
    private TextView expertIndicatorTextView;
    private Button animatedRepairButton;

    private boolean useVoiceOutput = false;

    private int uiState = UI_STATE_FULL_CHAT;

    private int bwTestState = BW_IDLE;

    private boolean shownDetailsHint = false;

    private boolean createdFirstTime = true;

    @Inject
    DobbyAnalytics dobbyAnalytics;

    /**
     * Interface for parent activities to implement.
     */
    public interface OnFragmentInteractionListener {

        /**
         * Called when user enters a text.
         * @param text
         */
        void onUserQuery(String text, boolean isButtonActionText);
        void onMicPressed();
        void onRecyclerViewReady();
        void onFragmentGone();
        void onFirstTimeResumed();
        void onFragmentReady();
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
        ((DobbyApplication) getActivity().getApplication()).getProdComponent().inject(this);
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        createdFirstTime = true;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        DobbyLog.v("CF: onCreateView started");
        // Inflate the layout for this fragment
        View fragmentView = inflater.inflate(R.layout.fragment_chat, container, false);
        setupTextToSpeech();
        chatRv = (RecyclerView) fragmentView.findViewById(R.id.chatRv);
        recyclerViewAdapter = new ChatRecyclerViewAdapter(getContext(), new LinkedList<ChatEntry>());
        chatRv.setAdapter(recyclerViewAdapter);
        chatRv.setLayoutManager(new LinearLayoutManager(getContext()));

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
                    processTextQuery(text, false);
                } else if (actionId == EditorInfo.IME_ACTION_DONE ||
                        actionId == EditorInfo.IME_ACTION_GO ||
                        actionId == EditorInfo.IME_ACTION_NEXT) {
                    DobbyLog.i("ENTER 2");
                    processTextQuery(text, false);
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

        expertIndicatorTextView = (TextView) fragmentView.findViewById(R.id.chatting_with_human_tv);

        DobbyLog.v("CF: Finished with onCreateView");
        return fragmentView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (mListener != null) {
            mListener.onRecyclerViewReady();
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
        DobbyLog.v("In onDetach");
        super.onDetach();
        //Cleanup text2speech
        textToSpeech.shutdown();
        mListener = null;
    }

    @Override
    public void onStart() {
        DobbyLog.v("CF: In onStart");
        super.onStart();
    }

    @Override
    public void onStop() {
        DobbyLog.v("CF: In onStop");
        super.onStop();
    }

    @Override
    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        super.dump(prefix, fd, writer, args);
    }

    @Override
    public void onResume() {
        DobbyLog.v("CF: In onResume");
        super.onResume();
        DobbyLog.v("Sending onFragmentReady callback to listener");
        if (mListener != null) {
            mListener.onFragmentReady();
        }
        if (createdFirstTime) {
            createdFirstTime = false;
            dobbyAnalytics.wifiExpertFragmentEntered();
            if (mListener != null) {
                mListener.onFirstTimeResumed();
            }
        }
    }

    @Override
    public void onPause() {
        DobbyLog.v("CF: In onPause");
        super.onPause();
    }

    @Override
    public void onDestroy() {
        DobbyLog.v("CF: In onDestroy");
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        DobbyLog.v("CF: In onDestroyView");
        super.onDestroyView();
        if (mListener != null) {
            DobbyLog.v("Sending onFragmentGone callback to listener");
            mListener.onFragmentGone();
        }
        createdFirstTime = false;
    }

    public void setBotMessageDelay(long botMessageDelay) {
        this.botMessageDelay = botMessageDelay;
    }

    public void showExpertIndicatorWithText(String text) {
        Message.obtain(handler, MSG_SHOW_EXPERT_INDICATOR, text).sendToTarget();
    }

    public void hideExpertIndicator() {
        Message.obtain(handler, MSG_HIDE_EXPERT_INDICATOR).sendToTarget();
    }

    public void addUserChat(String text) {
        ChatEntry chatEntry = new ChatEntry(text.trim(), ChatEntry.USER_CHAT);
        recyclerViewAdapter.addEntryAtBottom(chatEntry);
        chatRv.scrollToPosition(recyclerViewAdapter.getItemCount() - 1);
    }



    public void showDetailedSuggestionsView(SuggestionCreator.Suggestion suggestion) {
        if (suggestion != null) {
            Message.obtain(handler, MSG_SHOW_DETAILED_SUGGESTIONS, suggestion).sendToTarget();
        } else {
            showBotResponse(getString(R.string.detailed_suggestion_not_available));
        }
    }

    public void addPingResultsCardview(DataInterpreter.PingGrade pingGrade) {
        ChatEntry chatEntry = new ChatEntry(Utils.EMPTY_STRING, ChatEntry.PING_RESULTS_CARDVIEW);
        chatEntry.setPingGrade(pingGrade);
        recyclerViewAdapter.addEntryAtBottom(chatEntry);
        chatRv.scrollToPosition(recyclerViewAdapter.getItemCount() - 1);
    }

    public void showWifiRepairCardView(WifiInfo wifiInfo) {
        if (wifiInfo == null) {
            return;
        }
        ChatEntry chatEntry = new ChatEntry(Utils.EMPTY_STRING, ChatEntry.OVERALL_NETWORK_CARDVIEW);
        DataInterpreter.WifiGrade wifiGrade = DataInterpreter.interpret(wifiInfo.getRssi(), wifiInfo.getSSID(), wifiInfo.getLinkSpeed(), 0);
        chatEntry.setWifiGrade(wifiGrade);
        chatEntry.setIspName(Utils.EMPTY_STRING);
        chatEntry.setRouterIp(Utils.EMPTY_STRING);
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

    //All the methods that show stuff to user
    public void addRepairResultsCardView(final WifiInfo wifiInfo) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Message.obtain(handler, MSG_SHOW_REPAIR_STATUS, wifiInfo).sendToTarget();
            }
        }, botMessageDelay);
    }

    public void addBandwidthResultsCardView(final DataInterpreter.BandwidthGrade bandwidthGrade) {
        dobbyAnalytics.wifiExpertBandwidthCardShown();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Message.obtain(handler, MSG_SHOW_BANDWIDTH_RESULT_CARDVIEW, bandwidthGrade).sendToTarget();
            }
        }, botMessageDelay);
    }

    public void addOverallNetworkResultsCardView(final DataInterpreter.WifiGrade wifiGrade, final String ispName, final String externalIp) {
        dobbyAnalytics.wifiExpertWifiCardShown();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Message.obtain(handler, MSG_SHOW_OVERALL_NETWORK_STATUS, new OverallNetworkInfo(wifiGrade, ispName, externalIp)).sendToTarget();
            }
        }, botMessageDelay);
        //Message.obtain(handler, MSG_SHOW_OVERALL_NETWORK_STATUS, new OverallNetworkInfo(wifiGrade, ispName, externalIp)).sendToTarget();
    }

    public void observeBandwidthNonUi(final BandwidthObserver observer) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Message.obtain(handler, MSG_SHOW_BW_GAUGE, observer).sendToTarget();
            }
        }, botMessageDelay);
        //Message.obtain(handler, MSG_SHOW_BW_GAUGE, observer).sendToTarget();
    }

    public void showBotResponse(final String text) {
        DobbyLog.v("ChatF: showBotResponse text " + text);
        //showStatus(getString(R.string.agent_is_typing), 0);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Message.obtain(handler, MSG_SHOW_DOBBY_CHAT, text).sendToTarget();
            }
        }, botMessageDelay);
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                Message.obtain(handler, MSG_SHOW_DOBBY_CHAT, text).sendToTarget();
//            }
//        }, computeDelayForNextBotMessage());
        //Message.obtain(handler, MSG_SHOW_DOBBY_CHAT, text).sendToTarget();
    }

    public void showUserResponse(final String text) {
        DobbyLog.v("ChatF: showUserResponse text " + text);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Message.obtain(handler, MSG_SHOW_USER_CHAT, text).sendToTarget();
            }
        }, botMessageDelay);
        //Message.obtain(handler, MSG_SHOW_DOBBY_CHAT, text).sendToTarget();
    }


    //No delay for following
    public void showRtGraph(RtDataSource<Float, Integer> rtDataSource) {
        Message.obtain(handler, MSG_SHOW_RT_GRAPH, rtDataSource).sendToTarget();
    }

    public void showUserActionOptions(final List<Integer> userResponseTypes) {
        DobbyLog.v("In showUserActionOptions of CF: responseTypes: " + userResponseTypes);
        //Show all the buttons programatically and tie the response to send the user query
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Message.obtain(handler, MSG_SHOW_USER_ACTION_BUTTONS, userResponseTypes).sendToTarget();
            }
        }, botMessageDelay);
        //Message.obtain(handler, MSG_SHOW_USER_ACTION_BUTTONS, userResponseTypes).sendToTarget();
    }

    public void showExpertChatMessage(final String text) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Message.obtain(handler, MSG_SHOW_EXPERT_CHAT, text).sendToTarget();
            }
        }, botMessageDelay);
        //Message.obtain(handler, MSG_SHOW_EXPERT_CHAT, text).sendToTarget();
    }


    public void cancelTests() {
        //resetData();
        //uiStateChange(UI_STATE_FULL_CHAT);
        dismissBandwidthGaugeNonUi();
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
                DobbyLog.v("In handleMessage for DobbyChat show status");
                String status = (String) msg.obj;
                addDobbyChat(status, true);
                break;
            case MSG_SHOW_USER_CHAT:
                // Add to the recycler view.
                DobbyLog.v("In handleMessage for DobbyChat");
                String userText = (String) msg.obj;
                addUserChat(userText);
                break;
            case MSG_SHOW_EXPERT_CHAT:
                // Add to the recycler view.
                DobbyLog.v("In handleMessage for ExpertChat");
                String expertChatText = (String) msg.obj;
                addExpertChat(expertChatText);
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
                //addDobbyChat(overallNetworkInfo.getWifiGrade().userReadableInterpretation(), false);
                break;
            case MSG_SHOW_REPAIR_STATUS:
                WifiInfo wifiInfo = (WifiInfo)msg.obj;
                if (wifiInfo != null) {
                    showWifiRepairCardView(wifiInfo);
                }
                break;
            case MSG_SHOW_DETAILED_SUGGESTIONS:
                SuggestionCreator.Suggestion suggestionToShow = (SuggestionCreator.Suggestion) msg.obj;
                showDetailedSuggestionsAlert(suggestionToShow);
                addDobbyChat(getString(R.string.detailed_suggestion_status_message), false);
                if (! shownDetailsHint) {
                    addDobbyChat(getString(R.string.detailed_suggestion_tip), false);
                    shownDetailsHint = true;
                }
                break;
            case MSG_SHOW_EXPERT_INDICATOR:
                String expertIndicatorText = (String) msg.obj;
                if (expertIndicatorTextView != null) {
                    expertIndicatorTextView.setText(expertIndicatorText);
                    expertIndicatorTextView.setVisibility(View.VISIBLE);
                }
                break;
            case MSG_HIDE_EXPERT_INDICATOR:
                expertIndicatorTextView.setVisibility(View.GONE);
                break;
        }
        return false;
    }

    public void dismissBandwidthGaugeNonUi() {
        uiStateChangeNonUi(UI_STATE_FULL_CHAT);
    }

    public void addSpokenText(String userText) {
        //addUserChat(userText);
        if (textToSpeech != null) {
            useVoiceOutput = true;
        }
    }

    private long computeDelayForNextBotMessage() {
        long currentSystemTimeInMillis = System.currentTimeMillis();
        long delay = 0;
        if (currentSystemTimeInMillis < lastBotMessageScheduledAt) {
            delay = (lastBotMessageScheduledAt - currentSystemTimeInMillis) + botMessageDelay;
        } else {
            delay = botMessageDelay;
        }
        lastBotMessageScheduledAt = currentSystemTimeInMillis + delay;
        return delay;
    }

    private void showBandwidthResultsCardView(double uploadMbps, double downloadMbps) {
        ChatEntry chatEntry = new ChatEntry(Utils.EMPTY_STRING, ChatEntry.BW_RESULTS_GAUGE_CARDVIEW);
        chatEntry.setBandwidthResults(uploadMbps, downloadMbps);
        recyclerViewAdapter.addEntryAtBottom(chatEntry);
        chatRv.scrollToPosition(recyclerViewAdapter.getItemCount() - 1);
    }

    private void showDetailedSuggestionsAlert(SuggestionCreator.Suggestion suggestion) {
        if (suggestion == null) {
            DobbyLog.v("Attempting to show more suggestions when currentSuggestions are null.");
            return;
        }
        WifiDocDialogFragment fragment = WifiDocDialogFragment.forSuggestion(suggestion.getTitle(),
                suggestion.getLongSuggestionList());
        //fragment.show(getActivity().getSupportFragmentManager(), "Suggestions");
        //fragment.show(getActivity().getSupportFragmentManager(), "Repair");
        FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
        ft.add(fragment, "Suggestions");
        ft.commitAllowingStateLoss();
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

    public void showStatus(final String message, long delay) {
        DobbyLog.v("ChatF: showStatus text " + message);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Message.obtain(handler, MSG_SHOW_STATUS, message).sendToTarget();
            }
        }, delay);
        //Message.obtain(handler, MSG_SHOW_STATUS, message).sendToTarget();
    }

    public void showStatus(final String message) {
        showStatus(message, botMessageDelay);
    }

    private void addExpertChat(String expertChatText) {
        ChatEntry chatEntry = new ChatEntry(expertChatText.trim(), ChatEntry.EXPERT_CHAT, false);
        recyclerViewAdapter.addEntryAtBottom(chatEntry);
        chatRv.scrollToPosition(recyclerViewAdapter.getItemCount() - 1);
    }


    private void showBandwidthGauge(BandwidthObserver observer) {
        observer.registerCallback(this);
        uiStateChange(UI_STATE_SHOW_BW_GAUGE);
    }

    private void logUserResponseButtonClickedEvent(int userResponseType) {
        switch (userResponseType) {
            case CANCEL:
                dobbyAnalytics.wifiExpertCancelBandwidthTestsClicked();
                break;
            case RUN_ALL_DIAGNOSTICS:
                dobbyAnalytics.wifiExpertSlowInternetButtonClicked();
                break;
            case RUN_BW_TESTS:
                dobbyAnalytics.wifiExpertRunTestButtonClicked();
                break;
            case RUN_WIFI_TESTS:
                dobbyAnalytics.wifiExpertCheckWifiButtonClicked();
                break;
            case LIST_ALL_FUNCTIONS:
                dobbyAnalytics.wifiExpertListDobbyFunctions();
                break;
            case SHOW_LAST_SUGGESTION_DETAILS:
                dobbyAnalytics.wifiExpertMoreDetailsButtonClicked();
                break;
            case CONTACT_HUMAN_EXPERT:
                dobbyAnalytics.wifiExpertContactExpertButtonClicked();
                break;
        }
    }

    private void showUserActionButtons(List<Integer> userResponseTypes) {
        stopRotatingRepairButtonIfSet();
        actionMenu.removeAllViewsInLayout();
        for (final int userResponseType: userResponseTypes) {
            final String buttonText = UserResponse.getStringForResponseType(userResponseType);
            //Returning if context is null
            if (buttonText == null || buttonText.equals(Utils.EMPTY_STRING) || getContext() == null) {
                continue;
            }
            Button button = new Button(getContext(), null, android.R.attr.buttonStyleSmall);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            params.setMargins(10, 0, 10, 10);
            button.setLayoutParams(params);
            button.setText(buttonText);
            button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            //button.setMinHeight((int)Utils.convertPixelsToDp(10, this.getContext())); // In pixels
            //button.setTextColor(getResources().getColor(R.color.basicRed));
            button.setMinHeight(0);
            button.setMinWidth(0);
            button.setClickable(true);
            button.setAllCaps(false);

            if (userResponseType == UserResponse.ResponseType.CONTACT_HUMAN_EXPERT) {
                button.setBackgroundResource(R.drawable.rounded_shape_action_button_contact_expert);
                button.setTextColor(Color.DKGRAY); // light gray
            } else if (userResponseType == UserResponse.ResponseType.TURN_ON_WIFI_MONITORING) {
                button.setBackgroundResource(R.drawable.rounded_shape_action_button_contact_expert);
                button.setTextColor(Color.DKGRAY); // light gray
//            } else if (userResponseType == UserResponse.ResponseType.START_WIFI_REPAIR) {
//                button.setBackgroundResource(R.drawable.rounded_shape_action_button_contact_expert);
//                button.setTextColor(Color.DKGRAY); // light gray
            } else if (userResponseType == UserResponse.ResponseType.REPAIRING) {
                button.setClickable(false);
                button.setText(Utils.EMPTY_STRING);
                button.setBackgroundResource(R.drawable.ic_repair);
                //button.setTextColor(Color.DKGRAY); // light gray
                animatedRepairButton = button;
            } else {
                button.setBackgroundResource(R.drawable.rounded_shape_action_button);
                button.setTextColor(Color.LTGRAY); // light gray
            }


            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    logUserResponseButtonClickedEvent(userResponseType);
                    processTextQuery(buttonText, true);
                }
            });
            actionMenu.addView(button);
        }
        rotateRepairButtonIfSet();
    }

    private void rotateRepairButtonIfSet() {
        if (animatedRepairButton == null) {
            return;
        }
        RotateAnimation anim = new RotateAnimation(0.0f, 360.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        //Setup anim with desired properties
        anim.setInterpolator(new LinearInterpolator());
        anim.setRepeatCount(Animation.INFINITE); //Repeat animation indefinitely
        anim.setDuration(700); //Put desired duration per anim cycle here, in milliseconds
        animatedRepairButton.startAnimation(anim);
    }

    private void stopRotatingRepairButtonIfSet() {
        if (animatedRepairButton == null) {
            return;
        }
        animatedRepairButton.setAnimation(null);
        animatedRepairButton = null;
    }

    private void processTextQuery(String text, boolean isButtonActionText) {
        //Ignore empty strings
        if (text.equals(Utils.EMPTY_STRING)) {
            return;
        }
        //addUserChat(text);
        useVoiceOutput = false;
        // Parent activity callback.
        if (mListener != null) {
            mListener.onUserQuery(text, isButtonActionText);
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
        entry.addGraph(new GraphData<Float, Integer>(rtDataSource, BandwidthTestCodes.TestMode.DOWNLOAD));
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
    public void onTestFinished(@BandwidthTestCodes.TestMode int testMode, BandwidthStats stats) {
        Message.obtain(handler, MSG_UPDATE_CIRCULAR_GAUGE, Utils.BandwidthValue.from(testMode, (stats.getOverallBandwidth() / 1.0e6))).sendToTarget();
        if (testMode == BandwidthTestCodes.TestMode.UPLOAD) {
            showStatus(R.string.status_finished_bw_tests);
            dismissBandwidthGaugeNonUi();
        }
    }

    @Override
    public void onTestProgress(@BandwidthTestCodes.TestMode int testMode, double instantBandwidth) {
        if (testMode == BandwidthTestCodes.TestMode.DOWNLOAD && getBwTestState() != BW_DOWNLOAD_RUNNING) {
            setBwTestState(BW_DOWNLOAD_RUNNING);
            showStatus(R.string.status_running_download_tests);
        } else if (testMode == BandwidthTestCodes.TestMode.UPLOAD && getBwTestState() != BW_UPLOAD_RUNNING) {
            setBwTestState(BW_UPLOAD_RUNNING);
            showStatus(R.string.status_running_upload_tests);
        }
        Message.obtain(handler, MSG_UPDATE_CIRCULAR_GAUGE, Utils.BandwidthValue.from(testMode, (instantBandwidth / 1.0e6))).sendToTarget();
    }

    @Override
    public void onBandwidthTestError(@BandwidthTestCodes.TestMode int testMode, @BandwidthTestCodes.ErrorCodes int errorCode, @Nullable String errorMessage) {
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
