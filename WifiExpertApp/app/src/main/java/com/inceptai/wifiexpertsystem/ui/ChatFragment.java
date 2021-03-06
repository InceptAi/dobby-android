package com.inceptai.wifiexpertsystem.ui;

import android.content.Context;
import android.graphics.Color;
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

import com.inceptai.wifiexpertsystem.DobbyApplication;
import com.inceptai.wifiexpertsystem.R;
import com.inceptai.wifiexpertsystem.analytics.DobbyAnalytics;
import com.inceptai.wifiexpertsystem.expertSystem.inferencing.DataInterpreter;
import com.inceptai.wifiexpertsystem.expertSystem.inferencing.SuggestionCreator;
import com.inceptai.wifiexpertsystem.expertSystem.messages.StructuredUserResponse;
import com.inceptai.wifiexpertsystem.utils.DobbyLog;
import com.inceptai.wifiexpertsystem.utils.Utils;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.speedtest.BandwidthObserver;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.speedtest.BandwidthProgressSnapshot;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.speedtest.BandwidthResult;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.speedtest.BandwidthStats;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.speedtest.ServerInformation;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.speedtest.SpeedTestConfig;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.wifi.WifiNetworkOverview;
import com.inceptai.wifimonitoringservice.actionlibrary.utils.ActionLibraryCodes;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.schedulers.Schedulers;

import static com.inceptai.wifiexpertsystem.expertSystem.messages.StructuredUserResponse.ResponseType.CANCEL;
import static com.inceptai.wifiexpertsystem.expertSystem.messages.StructuredUserResponse.ResponseType.CONTACT_HUMAN_EXPERT;
import static com.inceptai.wifiexpertsystem.expertSystem.messages.StructuredUserResponse.ResponseType.LIST_ALL_FUNCTIONS;
import static com.inceptai.wifiexpertsystem.expertSystem.messages.StructuredUserResponse.ResponseType.RUN_ALL_DIAGNOSTICS;
import static com.inceptai.wifiexpertsystem.expertSystem.messages.StructuredUserResponse.ResponseType.RUN_BW_TESTS;
import static com.inceptai.wifiexpertsystem.expertSystem.messages.StructuredUserResponse.ResponseType.RUN_WIFI_TESTS;
import static com.inceptai.wifiexpertsystem.expertSystem.messages.StructuredUserResponse.ResponseType.SHOW_LAST_SUGGESTION_DETAILS;
import static com.inceptai.wifiexpertsystem.utils.Utils.ZERO_POINT_ZERO;
import static com.inceptai.wifiexpertsystem.utils.Utils.nonLinearBwScale;


/**
 * Fragment shows the UI for the chat-based interaction with the AI agent.
 * Use the {@link ChatFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChatFragment extends Fragment implements Handler.Callback {
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
    private static final int MSG_SHOW_PING_RESULT_CARDVIEW = 15;


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

    private boolean useVoiceOutput = false;

    private int uiState = UI_STATE_FULL_CHAT;

    private int bwTestState = BW_IDLE;

    private boolean shownDetailsHint = false;

    private boolean createdFirstTime = true;

    private DisposableObserver disposableBandwidthObserver;
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
        void onUserQuery(String text, boolean isStructuredResponse, @StructuredUserResponse.ResponseType int responseType, int responseId);
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
        disposeExistingBandwidthObserver();
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


    public void showNetworkResultsCardView(WifiNetworkOverview wifiNetworkOverview) {
        ChatEntry chatEntry = new ChatEntry(Utils.EMPTY_STRING, ChatEntry.OVERALL_NETWORK_CARDVIEW);
        chatEntry.setPrimarySSID(wifiNetworkOverview.getSsid());
        chatEntry.setPrimarySignal(wifiNetworkOverview.getSignal());
        chatEntry.setPrimarySignalMetric(wifiNetworkOverview.getSignalMetric());
        chatEntry.setIspName(wifiNetworkOverview.getIsp());
        chatEntry.setRouterIp(wifiNetworkOverview.getExternalIP());
        recyclerViewAdapter.addEntryAtBottom(chatEntry);
        chatRv.scrollToPosition(recyclerViewAdapter.getItemCount() - 1);
    }

    //All the methods that show stuff to user
    public void addPingResultsCardView(final DataInterpreter.PingGrade pingGrade) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Message.obtain(handler, MSG_SHOW_PING_RESULT_CARDVIEW,  pingGrade).sendToTarget();
            }
        }, botMessageDelay);
    }


    public void addBandwidthResultsCardView(final double downloadMpbs, final double uploadMpbs) {
        dobbyAnalytics.wifiExpertBandwidthCardShown();
        final BandwidthCardInfo bandwidthCardInfo = new BandwidthCardInfo(downloadMpbs, uploadMpbs);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Message.obtain(handler, MSG_SHOW_BANDWIDTH_RESULT_CARDVIEW,  bandwidthCardInfo).sendToTarget();
            }
        }, botMessageDelay);
    }

    public void addOverallNetworkResultsCardView(final WifiNetworkOverview wifiNetworkOverview) {
        dobbyAnalytics.wifiExpertWifiCardShown();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Message.obtain(handler, MSG_SHOW_OVERALL_NETWORK_STATUS, wifiNetworkOverview).sendToTarget();
            }
        }, botMessageDelay);
    }

    public void observeBandwidthNonUi(final Observable bandwidthObservable) {
        DobbyLog.v("CF: observeBandwidthNonUi");
        observeBandwidthStats(bandwidthObservable);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Message.obtain(handler, MSG_SHOW_BW_GAUGE, bandwidthObservable).sendToTarget();
            }
        }, botMessageDelay);
    }

    public void showBotResponse(final String text) {
        DobbyLog.v("ChatF: showBotResponse text " + text);
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
    public void showUserActionOptions(final List<StructuredUserResponse> structuredUserResponses) {
        DobbyLog.v("In showUserActionOptions of CF: responseTypes: ");
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Message.obtain(handler, MSG_SHOW_USER_ACTION_BUTTONS, structuredUserResponses).sendToTarget();
            }
        }, botMessageDelay);
    }

    public void showExpertChatMessage(final String text) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Message.obtain(handler, MSG_SHOW_EXPERT_CHAT, text).sendToTarget();
            }
        }, botMessageDelay);
    }


    public void cancelTests() {
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
            case MSG_SHOW_BW_GAUGE:
                showBandwidthGauge((Observable) msg.obj);
                break;
            case MSG_UPDATE_CIRCULAR_GAUGE:
                updateBandwidthGauge(msg);
                break;
            case MSG_UI_STATE_CHANGE:
                uiStateChange((int)msg.obj);
                break;
            case MSG_SHOW_USER_ACTION_BUTTONS:
                showUserActionButtons((List<StructuredUserResponse>) msg.obj);
                break;
            case MSG_SHOW_BANDWIDTH_RESULT_CARDVIEW:
                BandwidthCardInfo bandwidthCardInfo = (BandwidthCardInfo)msg.obj;
                addDobbyChat(getString(R.string.bandwidth_card_view_message), false);
                showBandwidthResultsCardView(bandwidthCardInfo.getUploadMbps(), bandwidthCardInfo.getDownloadMbps());
                break;
            case MSG_SHOW_PING_RESULT_CARDVIEW:
                DataInterpreter.PingGrade pingGrade = (DataInterpreter.PingGrade) msg.obj;
                addDobbyChat(getString(R.string.ping_card_view_message), false);
                showPingResultsCardView(pingGrade);
                break;
            case MSG_SHOW_OVERALL_NETWORK_STATUS:
                WifiNetworkOverview wifiNetworkOverview = (WifiNetworkOverview) msg.obj;
                if (!Utils.isNullOrEmpty(wifiNetworkOverview.getSsid())) {
                    addDobbyChat(getString(R.string.wifi_status_view_message), false);
                    showNetworkResultsCardView(wifiNetworkOverview);
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

    private void showPingResultsCardView(DataInterpreter.PingGrade pingGrade) {
        ChatEntry chatEntry = new ChatEntry(Utils.EMPTY_STRING, ChatEntry.PING_RESULTS_CARDVIEW);
        chatEntry.setPingGrade(pingGrade);
        recyclerViewAdapter.addEntryAtBottom(chatEntry);
        chatRv.scrollToPosition(recyclerViewAdapter.getItemCount() - 1);
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
        }
        WifiExpertDialogFragment fragment = WifiExpertDialogFragment.forSuggestion(suggestion.getTitle(),
                suggestion.getLongSuggestionList());
        fragment.show(getActivity().getSupportFragmentManager(), "Suggestions");
        dobbyAnalytics.moreSuggestionsShown(suggestion.getTitle(),
                new ArrayList<String>(suggestion.getShortSuggestionList()));
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


    private void showBandwidthGauge(Observable bandwidthObservable) {
        DobbyLog.v("CF: showBandwidthGuage");
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

    private String insertNewLineInLongString(String longString, int minCharactersBetweenBreak) {
        List<String> longStringWords = Arrays.asList(longString.split(" "));
        List<String> listToReturn = new ArrayList<>();
        int totalCharacters = 0;
        for (String word: longStringWords) {
            totalCharacters = totalCharacters + word.length();
            listToReturn.add(word);
            if (totalCharacters > minCharactersBetweenBreak) {
                listToReturn.add("\n");
                totalCharacters = 0;
            }
        }
        StringBuilder newLineStringBuilder = new StringBuilder();
        for (String word: listToReturn) {
            newLineStringBuilder.append(word);
            newLineStringBuilder.append(" ");
        }
        return newLineStringBuilder.toString();
    }


    private void showUserActionButtons(List<StructuredUserResponse> structuredUserResponses) {
        final int MIN_CHARACTERS_BEFORE_NEWLINE = 20;
        actionMenu.removeAllViewsInLayout();
        for (final StructuredUserResponse structuredUserResponse: structuredUserResponses) {
            final String buttonText = insertNewLineInLongString(structuredUserResponse.getResponseString(), MIN_CHARACTERS_BEFORE_NEWLINE);
            //Put a new line after every 4 words
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
            //Make the actions prominent
            button.setBackgroundResource(R.drawable.rounded_shape_action_button_contact_expert);
            button.setTextColor(Color.DKGRAY); // light gray

//            if (structuredUserResponse.getResponseType() == StructuredUserResponse.ResponseType.CONTACT_HUMAN_EXPERT) {
//                button.setBackgroundResource(R.drawable.rounded_shape_action_button_contact_expert);
//                button.setTextColor(Color.DKGRAY); // light gray
//            } else {
//                button.setBackgroundResource(R.drawable.rounded_shape_action_button);
//                button.setTextColor(Color.LTGRAY); // light gray
//            }


            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    logUserResponseButtonClickedEvent(structuredUserResponse.getResponseType());
                    processTextQuery(structuredUserResponse.getResponseString(), true, structuredUserResponse.getResponseType(), structuredUserResponse.getResponseId());
                }
            });
            actionMenu.addView(button);
        }
    }


    private void processTextQuery(String text) {
        processTextQuery(text, false, StructuredUserResponse.ResponseType.UNKNOWN, 0);
    }

    private void processTextQuery(String text, boolean isStructuredResponse, @StructuredUserResponse.ResponseType int responseType, int responseId) {
        //Ignore empty strings
        if (text.equals(Utils.EMPTY_STRING)) {
            return;
        }
        useVoiceOutput = false;
        if (mListener != null) {
            mListener.onUserQuery(text, isStructuredResponse, responseType, responseId);
        }
    }

    private void uiStateChangeNonUi(int newState) {
        Message.obtain(handler, MSG_UI_STATE_CHANGE, newState).sendToTarget();
    }

    private void uiStateChange(int newState) {
        uiState = newState;
        makeUiChanges(getView());
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
        if (bandwidthValue.mode == ActionLibraryCodes.BandwidthTestMode.UPLOAD) {
            uploadCircularGauge.setValue((int) nonLinearBwScale(bandwidthValue.value));
            uploadGaugeTv.setText(String.format("%2.2f", bandwidthValue.value));
        } else if (bandwidthValue.mode == ActionLibraryCodes.BandwidthTestMode.DOWNLOAD) {
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

    private void disposeExistingBandwidthObserver() {
        if (disposableBandwidthObserver != null && !disposableBandwidthObserver.isDisposed()) {
            disposableBandwidthObserver.dispose();
        }
    }

    //Replaced with observable
    private void observeBandwidthStats(Observable bandwidthObservable) {
        disposeExistingBandwidthObserver();
        disposableBandwidthObserver = new DisposableObserver<BandwidthProgressSnapshot>() {
            @Override
            public void onNext(BandwidthProgressSnapshot bandwidthProgressSnapshot) {
                DobbyLog.v("CF: OnNext");
                handleBandwidthProgressSnapshot(bandwidthProgressSnapshot);
            }

            @Override
            public void onError(Throwable e) {
                //finish the action here
                DobbyLog.v("CF: Error");
                BandwidthObserver.BandwidthTestException bandwidthTestException = (BandwidthObserver.BandwidthTestException)e;
                onBandwidthTestError(bandwidthTestException.getTestMode(), bandwidthTestException.getErrorCode(), bandwidthTestException.getErrorMessage());
            }

            @Override
            public void onComplete() {
                //finish it here too
                DobbyLog.v("CF: OnComplete");
            }
        };

        DobbyLog.v("CF: In observeBandwidthStats");
        bandwidthObservable
                .subscribeOn(Schedulers.newThread())
                .subscribeWith(disposableBandwidthObserver);
    }

    private void handleBandwidthProgressSnapshot(final BandwidthProgressSnapshot bandwidthProgressSnapshot) {
        if (bandwidthProgressSnapshot == null) {
            return;
        }
        switch (bandwidthProgressSnapshot.getResultType()) {
            case ActionLibraryCodes.BandwidthTestSnapshotType.SPEED_TEST_CONFIG:
                onConfigFetch(bandwidthProgressSnapshot.getSpeedTestConfig());
                break;
            case ActionLibraryCodes.BandwidthTestSnapshotType.SERVER_INFORMATION:
                onServerInformationFetch(bandwidthProgressSnapshot.getServerInformation());
                break;
            case ActionLibraryCodes.BandwidthTestSnapshotType.CLOSEST_SERVERS:
                onClosestServersSelected(bandwidthProgressSnapshot.getClosestServers());
                break;
            case ActionLibraryCodes.BandwidthTestSnapshotType.BEST_SERVER_DETAILS:
                onBestServerSelected(bandwidthProgressSnapshot.getBestServerDetails());
                break;
            case ActionLibraryCodes.BandwidthTestSnapshotType.INSTANTANEOUS_BANDWIDTH:
                onTestProgress(bandwidthProgressSnapshot.getTestMode(), bandwidthProgressSnapshot.getBandwidth());
                break;
            case ActionLibraryCodes.BandwidthTestSnapshotType.FINAL_BANDWIDTH:
                DobbyLog.v("CF: In BandwidthTestSnapshotType.FINAL_BANDWIDTH");
                BandwidthResult finalResult = bandwidthProgressSnapshot.getFinalBandwidthResult();
                if (finalResult.getDownloadStats() != null) {
                    DobbyLog.v("CF: In BandwidthTestSnapshotType.FINAL_BANDWIDTH download stats");
                    onTestFinished(ActionLibraryCodes.BandwidthTestMode.DOWNLOAD, finalResult.getDownloadStats());
                }
                if (finalResult.getUploadStats() != null) {
                    DobbyLog.v("CF: In BandwidthTestSnapshotType.FINAL_BANDWIDTH upload stats");
                    onTestFinished(ActionLibraryCodes.BandwidthTestMode.UPLOAD, finalResult.getUploadStats());
                }
                break;
        }
    }
    //  NewBandwidthAnalyzer.ResultCallback methods -------
    public void onConfigFetch(SpeedTestConfig config) {
        if (getBwTestState() == BW_TEST_INITIATED) {
            showStatus(R.string.status_fetching_server_list);
        }
        setBwTestState(BW_CONFIG_FETCHED);
        DobbyLog.v("WifiDoc: Fetched config");
    }

    public void onServerInformationFetch(ServerInformation serverInformation) {
        if (getBwTestState() == BW_CONFIG_FETCHED) {
            String constructedString = getResources().getString(R.string.status_closest_servers, serverInformation.serverList.size());
            showStatus(constructedString);
        }
        setBwTestState(BW_SERVER_INFO_FETCHED);
        DobbyLog.v("WifiExpert: Fetched server info");
    }

    public void onClosestServersSelected(List<ServerInformation.ServerDetails> closestServers) {
        DobbyLog.v("WifiExpert Closest servers");
    }

    public void onBestServerSelected(ServerInformation.ServerDetails bestServer) {
        if (getBwTestState() == BW_SERVER_INFO_FETCHED) {
            String constructedMessage = getResources().getString(R.string.status_found_closest_server, bestServer.name, bestServer.latencyMs);
            showStatus(constructedMessage);
        }
        setBwTestState(BW_BEST_SERVER_DETERMINED);
        DobbyLog.v("WifiExpert: Best server");
    }

    public void onTestProgress(@ActionLibraryCodes.BandwidthTestMode int testMode, double instantBandwidth) {
        if (testMode == ActionLibraryCodes.BandwidthTestMode.DOWNLOAD && getBwTestState() != BW_DOWNLOAD_RUNNING) {
            setBwTestState(BW_DOWNLOAD_RUNNING);
            showStatus(R.string.status_running_download_tests);
        } else if (testMode == ActionLibraryCodes.BandwidthTestMode.UPLOAD && getBwTestState() != BW_UPLOAD_RUNNING) {
            setBwTestState(BW_UPLOAD_RUNNING);
            showStatus(R.string.status_running_upload_tests);
        }
        Message.obtain(handler, MSG_UPDATE_CIRCULAR_GAUGE, Utils.BandwidthValue.from(testMode, (instantBandwidth / 1.0e6))).sendToTarget();
    }

    public void onTestFinished(@ActionLibraryCodes.BandwidthTestMode int testMode, BandwidthStats stats) {
        Message.obtain(handler, MSG_UPDATE_CIRCULAR_GAUGE, Utils.BandwidthValue.from(testMode, (stats.getOverallBandwidth() / 1.0e6))).sendToTarget();
        if (testMode == ActionLibraryCodes.BandwidthTestMode.UPLOAD) {
            showStatus(R.string.status_finished_bw_tests);
            dismissBandwidthGaugeNonUi();
        }
    }

    public void onBandwidthTestError(@ActionLibraryCodes.BandwidthTestMode int testMode,
                                     @ActionLibraryCodes.ErrorCodes int errorCode,
                                     @Nullable String errorMessage) {
        showStatus(R.string.status_error_bw_tests);
        dismissBandwidthGaugeNonUi();
    }

    private class BandwidthCardInfo {
        private double downloadMbps;
        private double uploadMbps;

        BandwidthCardInfo(double downloadMbps, double uploadMbps) {
            this.downloadMbps = downloadMbps;
            this.uploadMbps = uploadMbps;
        }

        public double getDownloadMbps() {
            return downloadMbps;
        }

        public double getUploadMbps() {
            return uploadMbps;
        }
    }
}
