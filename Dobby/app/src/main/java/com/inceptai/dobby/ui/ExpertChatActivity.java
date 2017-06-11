package com.inceptai.dobby.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
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

import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.R;
import com.inceptai.dobby.expert.ExpertChat;
import com.inceptai.dobby.expert.ExpertChatService;
import com.inceptai.dobby.utils.Utils;

import org.w3c.dom.Text;

import java.util.ArrayList;

import static com.inceptai.dobby.utils.Utils.EMPTY_STRING;

public class ExpertChatActivity extends AppCompatActivity implements ExpertChatService.ChatCallback, Handler.Callback {
    public static final String CHAT_MESSAGES_CHILD = "expert_chat_rooms";
    private static final String PREF_FIRST_CHAT = "first_expert_chat";

    public static final String INTENT_NOTIF_SOURCE = "IntentNotifSource";

    private static final int MSG_UPDATE_CHAT = 1001;
    private static final int MSG_UPDATE_ETA = 1002;

    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar progressBar;
    private EditText mMessageEditText;
    private Button mSendButton;
    private TextView etaTextView;

    private ExpertChatService expertChatService;
    private WifiDocExpertChatRecyclerViewAdapter recyclerViewAdapter;
    private DobbyApplication dobbyApplication;

    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expert_chat);

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
                ExpertChat expertChat = new
                        ExpertChat(mMessageEditText.getText().toString(), ExpertChat.MSG_TYPE_USER_TEXT);
                expertChatService.pushData(expertChat);
                mMessageEditText.setText("");
            }
        });

        expertChatService = ExpertChatService.fetchInstance(dobbyApplication.getUserUuid());
        fetchChatMessages();
        if (isFirstChat()) {
            progressBar.setVisibility(View.GONE);
            WifiDocDialogFragment fragment = WifiDocDialogFragment.forExpertOnBoarding();
            fragment.show(getSupportFragmentManager(), "Wifi Expert Chat");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchChatMessages();
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
        String message = getResources().getString(R.string.expected_response_time_for_expert);
        if (!isPresent) {
            message += " Less than 12 hours.";
        } else {
            message += " Less than " + Utils.timeSecondsToString(newEtaSeconds);
        }
        Message.obtain(handler, MSG_UPDATE_ETA, message).sendToTarget();
    }

    @Override
    public void onEtaAvailable(long newEtaSeconds, boolean isPresent) {
        if (etaTextView != null && etaTextView.getVisibility() == View.INVISIBLE) {
            onEtaUpdated(newEtaSeconds, isPresent);
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
    }

    private void addChatEntry(ExpertChat expertChat) {
        recyclerViewAdapter.addChatEntry(expertChat);
        mMessageRecyclerView.smoothScrollToPosition(recyclerViewAdapter.getItemCount());
        progressBar.setVisibility(View.GONE);
    }

    @Override
    protected void onStop() {
        expertChatService.disconnect();
        expertChatService.unregisterChatCallback();
        super.onStop();
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
