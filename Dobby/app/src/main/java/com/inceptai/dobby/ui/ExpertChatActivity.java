package com.inceptai.dobby.ui;

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

import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.R;
import com.inceptai.dobby.expert.ExpertChat;
import com.inceptai.dobby.expert.ExpertChatService;
import com.inceptai.dobby.utils.Utils;

import java.util.ArrayList;

public class ExpertChatActivity extends AppCompatActivity implements ExpertChatService.ChatCallback, Handler.Callback {
    public static final String CHAT_MESSAGES_CHILD = "expert_chat_rooms";
    private static final String PREF_FIRST_CHAT = "first_expert_chat";

    private static final int MSG_UPDATE_CHAT = 1001;

    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar progressBar;
    private EditText mMessageEditText;
    private Button mSendButton;

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

        expertChatService = ExpertChatService.newInstance(dobbyApplication.getUserUuid());
        expertChatService.setCallback(this);
        if (isFirstChat()) {
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public void onMessageAvailable(ExpertChat expertChat) {
        Message.obtain(handler, MSG_UPDATE_CHAT, expertChat).sendToTarget();
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_UPDATE_CHAT:
                addChatEntry((ExpertChat) msg.obj);
                return true;
        }
        return false;
    }

    private void addChatEntry(ExpertChat expertChat) {
        recyclerViewAdapter.addChatEntry(expertChat);
        mMessageRecyclerView.smoothScrollToPosition(recyclerViewAdapter.getItemCount());
        progressBar.setVisibility(View.GONE);
    }

    @Override
    protected void onStop() {
        expertChatService.disconnect();
        super.onStop();
    }

    private void saveChatStarted() {
        Utils.saveSharedSetting(this,
                PREF_FIRST_CHAT, Utils.TRUE_STRING);
    }

    private boolean isFirstChat() {
        return Boolean.valueOf(Utils.readSharedSetting(this,
                PREF_FIRST_CHAT, Utils.FALSE_STRING));
    }
}
