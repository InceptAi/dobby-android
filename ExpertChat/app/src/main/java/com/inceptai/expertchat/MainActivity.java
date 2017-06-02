package com.inceptai.expertchat;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
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

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ChildEventListener {

    private ProgressBar progressBar;
    private DatabaseReference mFirebaseDatabaseReference;
    private ListView roomListView;
    private RoomArrayAdapter arrayAdapter;

    private static class RoomArrayAdapter extends ArrayAdapter<String> {

        public RoomArrayAdapter(@NonNull Context context, @LayoutRes int resource, @IdRes int textViewResourceId, @NonNull List<String> initialList) {
            super(context, resource, textViewResourceId, initialList);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            String uuid = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);
                TextView tv = (TextView) convertView.findViewById(android.R.id.text1);
                tv.setText(uuid);
            }
            return convertView;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        progressBar = (ProgressBar) findViewById(R.id.mainProgressBar);
        roomListView = (ListView) findViewById(R.id.mainListView);
        arrayAdapter = new RoomArrayAdapter(this, android.R.layout.simple_list_item_1, android.R.id.text1, new ArrayList<String>());
        roomListView.setAdapter(arrayAdapter);
        roomListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String userId = arrayAdapter.getItem(position);
                Intent intent = new Intent(MainActivity.this, ExpertChatActivity.class);
                intent.putExtra(ExpertChatActivity.UUID_EXTRA, userId);
                startActivity(intent);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        arrayAdapter.clear();
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference().child(ExpertChatActivity.CHAT_ROOM_CHILD_BASE_WIFI_TESTER);
        mFirebaseDatabaseReference.addChildEventListener(this);
    }

    @Override
    public void onChildAdded(final DataSnapshot dataSnapshot, String s) {
        runOnUiThread(new Runnable() {
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

    @Override
    protected void onStop() {
        mFirebaseDatabaseReference.removeEventListener(this);
        super.onStop();
    }
}
