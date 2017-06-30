package com.inceptai.dobby.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.inceptai.dobby.DobbyAnalytics;
import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.R;
import com.inceptai.dobby.ai.DataInterpreter;
import com.inceptai.dobby.ai.DobbyAi;
import com.inceptai.dobby.ai.RtDataSource;
import com.inceptai.dobby.ai.SuggestionCreator;
import com.inceptai.dobby.expert.ExpertChat;
import com.inceptai.dobby.expert.ExpertChatService;
import com.inceptai.dobby.speedtest.BandwidthObserver;
import com.inceptai.dobby.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import static com.inceptai.dobby.utils.Utils.EMPTY_STRING;

public class ExpertChatActivity extends AppCompatActivity implements
        ExpertChatService.ChatCallback,
        DobbyAi.ResponseCallback,
        Handler.Callback {
    public static final String CHAT_MESSAGES_CHILD = "expert_chat_rooms";
    private static final String PREF_FIRST_CHAT = "first_expert_chat";


    private static final int MSG_UPDATE_CHAT = 1001;
    private static final int MSG_UPDATE_ETA = 1002;

    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar progressBar;
    private EditText mMessageEditText;
    private Button mSendButton;
    private TextView etaTextView;

    private WifiDocExpertChatRecyclerViewAdapter recyclerViewAdapter;
    private DobbyApplication dobbyApplication;
    private long currentEtaSeconds;
    private boolean isPresent;
    private boolean chatInHumanMode;

    private Handler handler;

    @Inject
    DobbyAnalytics dobbyAnalytics;
    @Inject
    DobbyAi dobbyAi;
    @Inject
    ExpertChatService expertChatService;

    private boolean isFirstRun = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((DobbyApplication) getApplication()).getProdComponent().inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expert_chat);

        chatInHumanMode = false;
        handler = new Handler(this);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        getSupportActionBar().setTitle(R.string.expert_chat_title);
        mMessageRecyclerView = (RecyclerView) findViewById(R.id.messageRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);
        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);

        dobbyApplication = (DobbyApplication) getApplication();

        recyclerViewAdapter = new WifiDocExpertChatRecyclerViewAdapter(this, new ArrayList<ExpertChat>());

        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);
        mMessageRecyclerView.setAdapter(recyclerViewAdapter);
        etaTextView = (TextView) findViewById(R.id.eta_tv);
        etaTextView.setVisibility(View.INVISIBLE);

        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        mSendButton = (Button) findViewById(R.id.sendButton);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Send messages on click.
                processUserMessage(mMessageEditText.getText().toString());
                mMessageEditText.setText("");
            }
        });

		//TODO: Check do we need this
        fetchChatMessages();

        dobbyAi.setResponseCallback(this);
        dobbyAi.initChatToBotState(); //Resets booleans indicating which mode of expert are we in

        if (isFirstChat()) {
            isFirstRun = true;
            progressBar.setVisibility(View.GONE);
            WifiDocDialogFragment fragment = WifiDocDialogFragment.forExpertOnBoarding();
            fragment.show(getSupportFragmentManager(), "Wifi Expert Chat");
            dobbyAnalytics.chatActivityEnteredFirstTime();
        }
        currentEtaSeconds = ExpertChatService.ETA_OFFLINE;
        isPresent = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchChatMessages();
        processIntent(getIntent());
        if (isFirstRun) {
            addGeneralMessage("Welcome to Expert Chat !");
            addGeneralMessage("Say hello to start the conversation.");
        } else {
            addGeneralMessage("Welcome back !");
        }

        // addGeneralMessage(getEtaString(currentEtaSeconds, isPresent));
        expertChatService.sendUserEnteredMetaMessage();
        expertChatService.disableNotifications();
        expertChatService.checkIn(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    private void processIntent(Intent intent) {
        String notifSource = intent.getStringExtra(ExpertChatService.INTENT_NOTIF_SOURCE);
        if (notifSource != null) {
            dobbyAnalytics.expertChatNotificationConsumed();
        }
    }

    private void processUserMessage(String text) {
        if (!chatInHumanMode) {
            dobbyAi.sendQuery(text, true);
        }
    }


    private void fetchChatMessages() {
        expertChatService.setCallback(this);
        if (!expertChatService.isListenerConnected()) {
            recyclerViewAdapter.clear();
            expertChatService.fetchChatMessages();
        }
    }

    @Override
    public void onMessageAvailable(ExpertChat expertChat) {
        if (chatInHumanMode && expertChat.getMessageType() == ExpertChat.MSG_TYPE_BOT_TEXT) {
            //Don't show bot messages when human is in control
            return;
        }
        Message.obtain(handler, MSG_UPDATE_CHAT, expertChat).sendToTarget();
        saveChatStarted();
    }

    @Override
    public void onNoHistoryAvailable() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onEtaUpdated(long newEtaSeconds, boolean isPresent) {
        updateEta(newEtaSeconds, isPresent, false);
    }


    //Dobby.AI response callbacks
    @Override
    public void showBotResponseToUser(String text) {
        expertChatService.pushBotChatMessage(text);
    }

    @Override
    public void showRtGraph(RtDataSource<Float, Integer> rtDataSource) {
    }

    @Override
    public void observeBandwidth(BandwidthObserver observer) {
    }

    @Override
    public void cancelTests() {
    }

    @Override
    public void showUserActionOptions(List<Integer> userResponseTypes) {


    }

    @Override
    public void showBandwidthViewCard(DataInterpreter.BandwidthGrade bandwidthGrade) {

    }

    @Override
    public void showNetworkInfoViewCard(DataInterpreter.WifiGrade wifiGrade, String isp, String ip) {

    }

    @Override
    public void showDetailedSuggestions(SuggestionCreator.Suggestion suggestion) {

    }

    @Override
    public void contactExpertAndGetETA() {

    }

    @Override
    public void onUserMessageAvailable(String text, boolean isActionText, boolean sendMessageToExpert) {
        expertChatService.pushUserChatMessage(text, isActionText, sendMessageToExpert);
    }

    @Override
    public void showStatus(String text) {
    }

    @Override
    public void switchedToExpertMode() {
        chatInHumanMode = true;
    }

    @Override
    public void switchedToBotMode() {
        chatInHumanMode = false;
    }

    @Override
    public void switchedToExpertIsListeningMode() {

    }

    @Override
    public void userAskedForExpert() {

    }

    private void updateEta(long newEtaSeconds, boolean isPresent, boolean showInChat) {
        String messagePrefix = getResources().getString(R.string.expected_response_time_for_expert);
        String message = EMPTY_STRING;
        if (!isPresent || newEtaSeconds > ExpertChatService.ETA_12HOURS) {
            message = "Our experts are currently offline. You shall receive a response in about 12 hours.";
        } else {
            message = messagePrefix + " Less than " + Utils.timeSecondsToString(newEtaSeconds);
        }
        currentEtaSeconds = newEtaSeconds;
        this.isPresent = isPresent;
        Message.obtain(handler, MSG_UPDATE_ETA, message).sendToTarget();
        if (showInChat) {
            addGeneralMessage(message);
        }
    }

    private void addGeneralMessage(String generalMessage) {
        ExpertChat expertChat = new ExpertChat();
        expertChat.setMessageType(ExpertChat.MSG_TYPE_GENERAL_MESSAGE);
        expertChat.setText(generalMessage);
        Message.obtain(handler, MSG_UPDATE_CHAT, expertChat).sendToTarget();
    }

    @Override
    public void onEtaAvailable(long newEtaSeconds, boolean isPresent) {
        if (etaTextView != null && etaTextView.getVisibility() == View.INVISIBLE) {
            updateEta(newEtaSeconds, isPresent, true /* show in chat */);
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_UPDATE_CHAT:
                addChatEntry((ExpertChat) msg.obj);
                return true;
            case MSG_UPDATE_ETA:
                showEta((String) msg.obj);
                return true;
        }
        return false;
    }

    private void showEta(String message) {
        etaTextView.setText(message);
        etaTextView.setVisibility(View.VISIBLE);
        dobbyAnalytics.showETAToUser(message);
    }

    private void addChatEntry(ExpertChat expertChat) {
        recyclerViewAdapter.addChatEntry(expertChat);
        mMessageRecyclerView.smoothScrollToPosition(recyclerViewAdapter.getItemCount());
        progressBar.setVisibility(View.GONE);
        if (expertChat.getMessageType() == ExpertChat.MSG_TYPE_USER_TEXT) {
            dobbyAnalytics.receivedMessageFromUser();
        } else if (expertChat.getMessageType() == ExpertChat.MSG_TYPE_EXPERT_TEXT) {
            dobbyAnalytics.receivedMessageFromExpert();
        } else if (expertChat.getMessageType() == ExpertChat.MSG_TYPE_EXPERT_TEXT) {
            dobbyAnalytics.receivedMessageFromBot();
        }
    }

    @Override
    protected void onStop() {
        expertChatService.sendUserLeftMetaMessage();
        expertChatService.disconnect();
        expertChatService.unregisterChatCallback();
        expertChatService.enableNotifications();
        super.onStop();
    }

    @Override
    protected void onStart() {
        expertChatService.registerToEventBusListener();
        super.onStart();
    }

    private void saveChatStarted() {
        Utils.saveSharedSetting(this,
                PREF_FIRST_CHAT, Utils.FALSE_STRING);
    }

    private boolean isFirstChat() {
        return Boolean.valueOf(Utils.readSharedSetting(this,
                PREF_FIRST_CHAT, Utils.TRUE_STRING));
    }
}
