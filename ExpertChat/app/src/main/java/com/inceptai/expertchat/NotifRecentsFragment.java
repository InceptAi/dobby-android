package com.inceptai.expertchat;

import android.content.Context;
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

import com.google.common.util.concurrent.ListeningExecutorService;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by arunesh on 6/22/17.
 */

public class NotifRecentsFragment extends Fragment {
    public static final String FRAGMENT_TAG = "NotifRecentsFragment";

    private OnUserSelected mListener;
    private String selectedUserId;
    private ProgressBar progressBar;
    private ListView roomListView;
    private UserArrayAdapter arrayAdapter;
    private ExpertChatService expertChatService;
    private List<UserData> debugDobbyUsers;
    private List<UserData> releaseDobbyUsers;
    private List<UserData> debugWifidocUsers;
    private List<UserData> releaseWifidocUsers;
    private List<UserData> uncategorizedUsers;

    private static class UserArrayAdapter extends ArrayAdapter<UserData> {

        private String selectedUserId;

        public UserArrayAdapter(@NonNull Context context, @LayoutRes int resource, @IdRes int textViewResourceId, @NonNull List<UserData> initialList, @NonNull String selectedUserId) {
            super(context, resource, textViewResourceId, initialList);
            this.selectedUserId = selectedUserId;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            UserData userData = getItem(position);
            String uuid = userData.getUserUuid();
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.user_lv_item, parent, false);
                TextView uuidTv = (TextView) convertView.findViewById(R.id.userUuidTv);
                TextView flavorTv = (TextView) convertView.findViewById(R.id.flavorTv);
                TextView buildTypeTv = (TextView) convertView.findViewById(R.id.buildTypeTv);
                uuidTv.setText(userData.getUserUuid());
                flavorTv.setText(Utils.unknownIfEmpty(userData.appFlavor));
                buildTypeTv.setText(Utils.unknownIfEmpty(userData.buildType));
                if (uuid.equals(this.selectedUserId)) {
                    convertView.setBackgroundColor(getContext().getResources().getColor(R.color.basicGreen));
                }
            }
            return convertView;
        }
    }

    public NotifRecentsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        expertChatService = ExpertChatService.fetchInstance(getContext().getApplicationContext());
        debugDobbyUsers = new ArrayList<>();
        debugWifidocUsers = new ArrayList<>();
        releaseDobbyUsers = new ArrayList<>();
        releaseWifidocUsers = new ArrayList<>();
        uncategorizedUsers = new ArrayList<>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.i(Utils.TAG, "NotifRecentsFragment: onCreateView called.");
        View rootView = inflater.inflate(R.layout.fragment_user_selection, container, false);
        progressBar = (ProgressBar) rootView.findViewById(R.id.mainProgressBar);
        roomListView = (ListView) rootView.findViewById(R.id.mainListView);
        selectedUserId = expertChatService.getSelectedUserId();
        fetchUsers();
        List<UserData> finalList = new ArrayList<>();
        finalList.addAll(releaseDobbyUsers);
        finalList.addAll(releaseWifidocUsers);
        finalList.addAll(debugDobbyUsers);
        finalList.addAll(debugWifidocUsers);
        finalList.addAll(uncategorizedUsers);
        arrayAdapter = new UserArrayAdapter(getContext(), android.R.layout.simple_list_item_1,
                android.R.id.text1, finalList, selectedUserId);
        roomListView.setAdapter(arrayAdapter);
        roomListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onButtonPressed(arrayAdapter.getItem(position));
            }
        });
        return rootView;
    }

    private void fetchUsers() {
        List<UserData> allUsers = UserDataBackend.fetchUsers();
        for (UserData userData : allUsers) {
            if (userData.appFlavor != null && userData.appFlavor.equals(Utils.DOBBY_FLAVOR)) {
                if (userData.buildType != null && userData.buildType.equals(Utils.BUILD_TYPE_DEBUG)) {
                    debugDobbyUsers.add(userData);
                } else if (userData.buildType != null && userData.buildType.equals(Utils.BUILD_TYPE_RELEASE)) {
                    releaseDobbyUsers.add(userData);
                } else {
                    uncategorizedUsers.add(userData);
                }
            } else if (userData.appFlavor != null && userData.appFlavor.equals(Utils.WIFIDOC_FLAVOR)) {
                if (userData.buildType != null && userData.buildType.equals(Utils.BUILD_TYPE_DEBUG)) {
                    debugWifidocUsers.add(userData);
                } else if (userData.buildType != null && userData.buildType.equals(Utils.BUILD_TYPE_RELEASE)) {
                    releaseWifidocUsers.add(userData);
                } else {
                    uncategorizedUsers.add(userData);
                }
            } else {
                uncategorizedUsers.add(userData);
            }
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(UserData userData) {
        String userId = userData.getUserUuid();
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
    }

    @Override
    public void onPause() {
        super.onPause();
        getFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
    }
    private void noUserChatsFound() {
        progressBar.setVisibility(View.GONE);
    }

}
