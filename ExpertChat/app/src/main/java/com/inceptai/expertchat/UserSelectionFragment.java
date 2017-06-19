package com.inceptai.expertchat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class UserSelectionFragment extends Fragment implements ChildEventListener {
    public static String FRAGMENT_TAG = "UserSelectionFragment";
    private static String SELECTED_USERID = "userUuid";
    private static final String FLAVOR = "flavor";  // debug or release.
    private static final String BUILD_TYPE = "buildType";

    private OnUserSelected mListener;
    private String selectedUserId;
    private String flavor;
    private String buildType;
    private String chatRoomBase;
    private ProgressBar progressBar;
    private DatabaseReference mFirebaseDatabaseReference;
    private ListView roomListView;
    private RoomArrayAdapter arrayAdapter;
    private ExpertChatService expertChatService;

    private static class RoomArrayAdapter extends ArrayAdapter<String> {

        private String selectedUserId;

        public RoomArrayAdapter(@NonNull Context context, @LayoutRes int resource, @IdRes int textViewResourceId, @NonNull List<String> initialList, @NonNull String selectedUserId) {
            super(context, resource, textViewResourceId, initialList);
            this.selectedUserId = selectedUserId;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            String uuid = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
                TextView tv = (TextView) convertView.findViewById(android.R.id.text1);
                if (uuid.equals(this.selectedUserId)) {
                    tv.setText("SELECTED: " + uuid);
                    tv.setSelected(true);
                } else {
                    tv.setText(uuid);
                }
            }
            return convertView;
        }
    }

    public UserSelectionFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        expertChatService = ExpertChatService.fetchInstance(getContext().getApplicationContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.i(Utils.TAG, "UserSelectionFragment: onCreateView called.");
        View rootView = inflater.inflate(R.layout.fragment_user_selection, container, false);
        progressBar = (ProgressBar) rootView.findViewById(R.id.mainProgressBar);
        roomListView = (ListView) rootView.findViewById(R.id.mainListView);
        selectedUserId = expertChatService.getSelectedUserId();
        arrayAdapter = new RoomArrayAdapter(getContext(), android.R.layout.simple_list_item_1, android.R.id.text1, new ArrayList<String>(), selectedUserId);
        roomListView.setAdapter(arrayAdapter);
        roomListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onButtonPressed(arrayAdapter.getItem(position));
            }
        });
        return rootView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(String userId) {
        Log.i(Utils.TAG, "new UUID: " + userId);
        selectedUserId = userId;
        if (mListener != null) {
            mListener.onUserSelected(userId);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnUserSelected) {
            mListener = (OnUserSelected) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnUserSelected");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        arrayAdapter.clear();

        chatRoomBase = expertChatService.getChatRoomBase();
        flavor = expertChatService.getFlavor();
        buildType = expertChatService.getBuildType();

        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference().child(chatRoomBase);
        mFirebaseDatabaseReference.addChildEventListener(this);
        mFirebaseDatabaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot == null) {
                    noUserChatsFound();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        mFirebaseDatabaseReference.removeEventListener(this);
        getFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
    }

    private void noUserChatsFound() {
        progressBar.setVisibility(View.GONE);
    }

    public interface OnUserSelected {
        void onUserSelected(String userId);
    }

    @Override
    public void onChildAdded(final DataSnapshot dataSnapshot, String s) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
                arrayAdapter.add(dataSnapshot.getKey());
            }
        });
    }

    @Override
    public void onChildChanged(DataSnapshot dataSnapshot, String s) {

    }

    @Override
    public void onChildRemoved(DataSnapshot dataSnapshot) {

    }

    @Override
    public void onChildMoved(DataSnapshot dataSnapshot, String s) {

    }

    @Override
    public void onCancelled(DatabaseError databaseError) {

    }
}
