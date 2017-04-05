package com.inceptai.dobby.ui;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.inceptai.dobby.ChatEntry;
import com.inceptai.dobby.MainActivity;
import com.inceptai.dobby.R;

import java.util.LinkedList;

import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * Fragment shows the UI for the chat-based interaction with the AI agent.
 * Use the {@link ChatFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ChatFragment extends Fragment implements Handler.Callback {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    public static final String FRAGMENT_TAG = "ChatFragment";

    // Handler message types.
    private static final int MSG_SHOW_DOBBY_CHAT = 1;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private RecyclerView chatRv;
    private ChatRecyclerViewAdapter recyclerViewAdapter;
    private EditText queryEditText;
    private ImageView micButtonIv;
    private OnFragmentInteractionListener mListener;
    private Handler handler;


    /**
     * Interface for parent activities to implement.
     */
    public interface OnFragmentInteractionListener {

        /**
         * Called when user enters a text.
         * @param text
         */
        void onUserQuery(String text);
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
        // Inflate the layout for this fragment
        View fragmentView = inflater.inflate(R.layout.fragment_chat, container, false);

        chatRv = (RecyclerView) fragmentView.findViewById(R.id.chatRv);
        recyclerViewAdapter = new ChatRecyclerViewAdapter(this.getContext(), new LinkedList<ChatEntry>());
        chatRv.setAdapter(recyclerViewAdapter);
        chatRv.setLayoutManager(new LinearLayoutManager(this.getContext()));
        handler = new Handler(this);

        queryEditText = (EditText) fragmentView.findViewById(R.id.queryEditText);
        queryEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                String text = queryEditText.getText().toString();
                Log.i(TAG, "Action ID: " + actionId);
                if (event != null) {
                    Log.i(TAG, "key event: " + event.toString());
                }
                if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_DOWN) {
                    Log.i(TAG, "ENTER 1");
                    processTextQuery(text);
                } else if (actionId == EditorInfo.IME_ACTION_DONE ||
                        actionId == EditorInfo.IME_ACTION_GO ||
                        actionId == EditorInfo.IME_ACTION_NEXT) {
                    Log.i(TAG, "ENTER 2");
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
                Toast.makeText(getContext(), "Mic not supported yet !", Toast.LENGTH_LONG).show();
            }
        });

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


    private void addDobbyChat(String text) {
        Log.i(TAG, "Adding dobby chat: " + text);
        ChatEntry chatEntry = new ChatEntry(text.trim(), ChatEntry.DOBBY_CHAT);
        recyclerViewAdapter.addEntryAtBottom(chatEntry);
        chatRv.scrollToPosition(recyclerViewAdapter.getItemCount() - 1);
    }

    public void addUserChat(String text) {
        ChatEntry chatEntry = new ChatEntry(text.trim(), ChatEntry.USER_CHAT);
        recyclerViewAdapter.addEntryAtBottom(chatEntry);
        chatRv.scrollToPosition(recyclerViewAdapter.getItemCount() - 1);
    }

    private void processTextQuery(String text) {
        if (text.length() < 2) {
            return;
        }
        addUserChat(text);
        // Parent activity callback.
        mListener.onUserQuery(text);
    }

    public void showResponse(String text) {
        Message.obtain(handler, MSG_SHOW_DOBBY_CHAT, text).sendToTarget();
    }


    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_SHOW_DOBBY_CHAT:
                // Add to the recycler view.
                String text = (String) msg.obj;
                addDobbyChat(text);
                break;

        }
        return false;
    }
}
