package com.inceptai.expertchat;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ChatFragment extends Fragment {
    public static final String FRAGMENT_TAG = "ChatFragment";
    public static final String CHAT_ROOM_CHILD_BASE_DOBBY = "dobby_chat_rooms";
    public static final String CHAT_ROOM_CHILD_BASE_WIFI_TESTER = "wifitester_chat_rooms";

    private static final String USER_UUID = "userUuid";
    private static final String FLAVOR = "flavor";  // debug or release.

    public static final String ANONYMOUS = "anonymous";
    private static final String MESSAGE_SENT_EVENT = "message_sent";

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
    private String flavor;
    private String childPath;
    private TextView roomTitleTv;

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

    public ChatFragment() {
        // Required empty public constructor
    }

    public static Bundle getArgumentBundle(String userUuid, String flavor) {
        Bundle args = new Bundle();
        args.putString(USER_UUID, userUuid);
        args.putString(FLAVOR, flavor);
        return args;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userUuid = getArguments().getString(USER_UUID);
            flavor = getArguments().getString(FLAVOR);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_expert_chat, container, true);
        setup(rootView);
        return rootView;
    }

    private void setup(View rootView) {
        childPath = CHAT_ROOM_CHILD_BASE_WIFI_TESTER + "/" + flavor + "/" + userUuid;
        Log.i(Utils.TAG, "ChildPath: " + childPath);
        roomTitleTv = (TextView) rootView.findViewById(R.id.roomTitleTv);
        roomTitleTv.setText(childPath);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        mUsername = ANONYMOUS;

        mProgressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);
        mMessageRecyclerView = (RecyclerView) rootView.findViewById(R.id.messageRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(getContext());
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
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(getContext());

        mMessageEditText = (EditText) rootView.findViewById(R.id.messageEditText);
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

        mSendButton = (Button) rootView.findViewById(R.id.sendButton);
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
}
