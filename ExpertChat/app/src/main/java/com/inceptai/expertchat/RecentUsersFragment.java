package com.inceptai.expertchat;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by arunesh on 6/23/17.
 */

public class RecentUsersFragment extends Fragment {

    private OnUserSelected mListener;
    private String selectedUserId;
    private ProgressBar progressBar;
    private ListView roomListView;
    private UserArrayAdapter arrayAdapter;
    private ExpertChatService expertChatService;
    private List<UserData> debugUsers;
    private List<UserData> releaseUsers;
    private String flavor;

    private static class UserArrayAdapter extends BaseAdapter {

        private String selectedUserId;
        private List<UserData> userDataList;
        private Context context;

        public UserArrayAdapter(Context context, @NonNull List<UserData> initialList, @NonNull String selectedUserId) {
            super();
            this.context = context;
            this.userDataList = initialList;
            this.selectedUserId = selectedUserId;
        }

        @Override
        public int getCount() {
            return userDataList.size();
        }

        @Override
        public Object getItem(int position) {
            return userDataList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            UserData userData = (UserData) getItem(position);
            String uuid = userData.getUserUuid();
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.user_lv_item, parent, false);
            }
            TextView uuidTv = (TextView) convertView.findViewById(R.id.userUuidTv);
            TextView flavorTv = (TextView) convertView.findViewById(R.id.flavorTv);
            TextView buildTypeTv = (TextView) convertView.findViewById(R.id.buildTypeTv);
            uuidTv.setText(userData.getUserUuid());
            flavorTv.setText(Utils.unknownIfEmpty(userData.appFlavor));
            buildTypeTv.setText(Utils.unknownIfEmpty(userData.buildType));
            if (uuid.equals(this.selectedUserId)) {
                convertView.setBackgroundResource(R.drawable.center_gradient_light);
            } else {
                convertView.setBackgroundColor(context.getResources().getColor(android.R.color.white));
            }
            return convertView;
        }

        public void addAll(List<UserData> dataList) {
            userDataList.addAll(dataList);
            notifyDataSetChanged();
        }

    }

    public RecentUsersFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        expertChatService = ExpertChatService.fetchInstance(getContext().getApplicationContext());
        debugUsers = new ArrayList<>();
        releaseUsers = new ArrayList<>();
        Bundle args = getArguments();
        if (args != null) {
            flavor = args.getString(MainActivity.FRAGMENT_ARG_FLAVOR_TYPE, Utils.WIFIDOC_FLAVOR);
        } else {
            flavor = Utils.WIFIDOC_FLAVOR;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.i(Utils.TAG, "NotifRecentsFragment: onCreateView called.");
        View rootView = inflater.inflate(R.layout.fragment_user_selection, container, false);
        progressBar = (ProgressBar) rootView.findViewById(R.id.mainProgressBar);
        roomListView = (ListView) rootView.findViewById(R.id.mainListView);
        selectedUserId = expertChatService.getSelectedUserId();
        fetchWifidocRecentUsers();
        arrayAdapter = new UserArrayAdapter(getContext(), new ArrayList<UserData>(), selectedUserId);
        roomListView.setAdapter(arrayAdapter);
        roomListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onButtonPressed((UserData) arrayAdapter.getItem(position));
            }
        });
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        progressBar.setVisibility(View.GONE);
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
    }

    private void fetchWifidocRecentUsers() {
        String recentsDebug = getRecents() + "/" + Utils.BUILD_TYPE_DEBUG + "/";
        DatabaseReference debugRef = FirebaseDatabase.getInstance().getReference().child(recentsDebug);
        debugRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot == null) {
                    return;
                }

                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    String uuid = data.getKey();
                    UserData userData = UserDataBackend.fetchUserWith(uuid, flavor, Utils.BUILD_TYPE_DEBUG);
                    debugUsers.add(userData);
                }
                arrayAdapter.addAll(debugUsers);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        final String recentsRelease = getRecents() + "/" + Utils.BUILD_TYPE_RELEASE + "/";
        DatabaseReference releaseRef = FirebaseDatabase.getInstance().getReference().child(recentsRelease);
        releaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot == null) {
                    return;
                }

                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    String uuid = data.getKey();
                    UserData userData = UserDataBackend.fetchUserWith(uuid, flavor, Utils.BUILD_TYPE_RELEASE);
                    releaseUsers.add(userData);
                }
                arrayAdapter.addAll(releaseUsers);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private String getRecents() {
        if (flavor.equals(Utils.WIFIDOC_FLAVOR)) {
            return Utils.WIFIDOC_RECENTS;
        } else {
            return Utils.DOBBY_RECENTS;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();
    }
}
