package com.inceptai.dobby.ui;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.R;
import com.inceptai.dobby.expert.ExpertChat;
import com.inceptai.dobby.expert.ExpertChatService;

import java.util.ArrayList;

public class ExpertChatActivity extends AppCompatActivity implements ExpertChatService.ChatCallback, Handler.Callback {
    public static final String CHAT_MESSAGES_CHILD = "expert_chat_rooms";
    private static final int MSG_UPDATE_CHAT = 1001;

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView expertMessageTv;
        TextView userMessageTv;

        public MessageViewHolder(View v) {
            super(v);
            expertMessageTv = (TextView) itemView.findViewById(R.id.wd_expert_chat_tv);
            userMessageTv = (TextView) itemView.findViewById(R.id.wd_user_chat_tv);
        }
    }

    // Firebase instance variables
    private DatabaseReference mFirebaseDatabaseReference;
    private FirebaseRecyclerAdapter<ExpertChat, MessageViewHolder> mFirebaseAdapter;

    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar progressBar;
    private EditText mMessageEditText;
    private Button mSendButton;
    private ImageView mAddMessageImageView;

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
        mMessageRecyclerView = (RecyclerView) findViewById(R.id.messageRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);
        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);

        dobbyApplication = (DobbyApplication) getApplication();

        recyclerViewAdapter = new WifiDocExpertChatRecyclerViewAdapter(this, new ArrayList<ExpertChat>());
        // New child entries
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mFirebaseAdapter = new FirebaseRecyclerAdapter<ExpertChat, MessageViewHolder>(
                ExpertChat.class,
                R.layout.expert_chat_message_item,
                MessageViewHolder.class,
                mFirebaseDatabaseReference.child(CHAT_MESSAGES_CHILD)) {

            @Override
            protected void populateViewHolder(final MessageViewHolder viewHolder,
                                              ExpertChat friendlyMessage, int position) {
                progressBar.setVisibility(ProgressBar.INVISIBLE);
                if (friendlyMessage.getText() != null) {
                    viewHolder.expertMessageTv.setText(friendlyMessage.getText());
                    viewHolder.expertMessageTv.setVisibility(TextView.VISIBLE);
                } else {
                    viewHolder.expertMessageTv.setVisibility(TextView.GONE);
                }

            }
        };

        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition =
                        mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    mMessageRecyclerView.scrollToPosition(positionStart);
                }
            }
        });
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
                mFirebaseDatabaseReference.child(CHAT_MESSAGES_CHILD)
                        .push().setValue(expertChat);
                mMessageEditText.setText("");
            }
        });

        mAddMessageImageView = (ImageView) findViewById(R.id.addMessageImageView);
        mAddMessageImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Select image for image message on click.
            }
        });

        expertChatService = ExpertChatService.newInstance(dobbyApplication.getUserUuid());
        expertChatService.setCallback(this);
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
    }

    @Override
    protected void onStop() {
        expertChatService.disconnect();
        super.onStop();
    }
}
