package com.inceptai.expertchat;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.appindexing.Action;
import com.google.firebase.appindexing.FirebaseAppIndex;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.firebase.appindexing.Indexable;
import com.google.firebase.appindexing.builders.Indexables;
import com.google.firebase.appindexing.builders.PersonBuilder;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ExpertChatActivity extends AppCompatActivity {
    public static final String CHAT_ROOM_CHILD_BASE_DOBBY = "dobby_chat_rooms";
    public static final String CHAT_ROOM_CHILD_BASE_WIFI_TESTER = "wifitester_chat_rooms";
    public static final String UUID_EXTRA = "UUID";

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout fromExpertLayout;
        LinearLayout fromUserLayout;
        TextView expertMessageTv;
        TextView userMessageTv;

        public MessageViewHolder(View v) {
            super(v);
            fromExpertLayout = (LinearLayout) itemView.findViewById(R.id.expert_chat_ll);
            fromUserLayout = (LinearLayout) itemView.findViewById(R.id.user_chat_ll);
            expertMessageTv = (TextView) itemView.findViewById(R.id.expert_chat_tv);
            userMessageTv = (TextView) itemView.findViewById(R.id.user_chat_tv);
        }
    }

    public static final String ANONYMOUS = "anonymous";
    private static final String MESSAGE_SENT_EVENT = "message_sent";
    private static final String MESSAGE_URL = "http://friendlychat.firebase.google.com/message/";

    private String mUsername;
    private SharedPreferences mSharedPreferences;

    private Button mSendButton;
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private FirebaseRecyclerAdapter<ExpertChat, MessageViewHolder> mFirebaseAdapter;
    private ProgressBar mProgressBar;
    private DatabaseReference mFirebaseDatabaseReference;
    private FirebaseAnalytics mFirebaseAnalytics;
    private EditText mMessageEditText;
    private String userUuid;
    private String childPath;
    private TextView roomTitleTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_expert_chat);

        userUuid = getIntent().getStringExtra(UUID_EXTRA);
        childPath = CHAT_ROOM_CHILD_BASE_WIFI_TESTER + "/" + userUuid;
        roomTitleTv = (TextView) findViewById(R.id.roomTitleTv);
        roomTitleTv.setText(childPath);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mUsername = ANONYMOUS;

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mMessageRecyclerView = (RecyclerView) findViewById(R.id.messageRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mLinearLayoutManager.setStackFromEnd(true);

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mFirebaseAdapter = new FirebaseRecyclerAdapter<ExpertChat, MessageViewHolder>(
                ExpertChat.class,
                R.layout.expert_chat_message_item,
                MessageViewHolder.class,
                mFirebaseDatabaseReference.child(childPath)) {

            @Override
            protected ExpertChat parseSnapshot(DataSnapshot snapshot) {
                ExpertChat expertChat = super.parseSnapshot(snapshot);
                if (expertChat != null) {
                    expertChat.setId(snapshot.getKey());
                }
                Log.w("ExpertChat", "Received chat:" + expertChat.getText());
                return expertChat;
            }

            @Override
            protected void populateViewHolder(final MessageViewHolder viewHolder,
                                              ExpertChat expertChat, int position) {
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                if (ExpertChat.isExpertChat(expertChat)) {
                    viewHolder.fromUserLayout.setVisibility(View.GONE);
                    viewHolder.fromExpertLayout.setVisibility(View.VISIBLE);
                    viewHolder.expertMessageTv.setText(expertChat.getText());
                } else if (ExpertChat.isUserChat(expertChat)) {
                    viewHolder.fromExpertLayout.setVisibility(View.GONE);
                    viewHolder.fromUserLayout.setVisibility(View.VISIBLE);
                    viewHolder.userMessageTv.setText(expertChat.getText());
                }

                if (expertChat.getText() != null) {
                    // write this message to the on-device index
                    FirebaseAppIndex.getInstance().update(getMessageIndexable(expertChat));
                }

                // log a view action on it
                FirebaseUserActions.getInstance().end(getMessageViewAction(expertChat));
            }
        };

        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int ExpertChatCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition = mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the user is at the bottom of the list, scroll
                // to the bottom of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (ExpertChatCount - 1) && lastVisiblePosition == (positionStart - 1))) {
                    mMessageRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);
        mMessageRecyclerView.setAdapter(mFirebaseAdapter);

        // Initialize Firebase Measurement.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

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
                ExpertChat expertChat = new ExpertChat(mMessageEditText.getText().toString(), ExpertChat.MSG_TYPE_EXPERT_TEXT);
                mFirebaseDatabaseReference.child(childPath).push().setValue(expertChat);
                mMessageEditText.setText("");
                mFirebaseAnalytics.logEvent(MESSAGE_SENT_EVENT, null);
            }
        });
    }

    private Action getMessageViewAction(ExpertChat expertChat) {
        return new Action.Builder(Action.Builder.VIEW_ACTION)
                .setObject(expertChat.getText(), MESSAGE_URL.concat(expertChat.getId()))
                .setMetadata(new Action.Metadata.Builder().setUpload(false))
                .build();
    }

    private Indexable getMessageIndexable(ExpertChat expertChat) {
        PersonBuilder sender = Indexables.personBuilder()
                .setIsSelf(mUsername.equals(expertChat.getText()))
                .setName(expertChat.getText())
                .setUrl(MESSAGE_URL.concat(expertChat.getId() + "/sender"));

        PersonBuilder recipient = Indexables.personBuilder()
                .setName(mUsername)
                .setUrl(MESSAGE_URL.concat(expertChat.getId() + "/recipient"));

        Indexable messageToIndex = Indexables.messageBuilder()
                .setName(expertChat.getText())
                .setUrl(MESSAGE_URL.concat(expertChat.getId()))
                .setSender(sender)
                .setRecipient(recipient)
                .build();

        return messageToIndex;
    }
}
