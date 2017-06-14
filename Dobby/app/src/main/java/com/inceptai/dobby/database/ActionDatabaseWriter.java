package com.inceptai.dobby.database;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.inceptai.dobby.BuildConfig;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.utils.DobbyLog;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by vivek on 4/30/17.
 */
@Singleton
public class ActionDatabaseWriter {
    private static final String ACTIONS_NODE_NAME = "expert_actions";
    private static final String USERS_NODE_NAME = "users";
    private static final String USERS_DB_ROOT = BuildConfig.FLAVOR + "/" + BuildConfig.BUILD_TYPE + "/" + USERS_NODE_NAME;
    private DatabaseReference mDatabase;
    private ExecutorService executorService;

    @Inject
    public ActionDatabaseWriter(DobbyThreadpool dobbyThreadpool) {
        mDatabase = FirebaseDatabase.getInstance().getReference();
        executorService = dobbyThreadpool.getExecutorService();
    }

    private void writeNewAction(ActionRecord actionRecord) {
        String userKey;
        if (actionRecord.uid != null) {
            userKey = actionRecord.uid;
        } else {
            userKey = mDatabase.child(USERS_DB_ROOT).push().getKey();
        }
        String actionKey = mDatabase.child(USERS_NODE_NAME).child(userKey).child(ACTIONS_NODE_NAME).push().getKey();
        Map<String, Object> actionValues = actionRecord.toMap();
        DobbyLog.i("Actions key: " + actionKey);
        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("/" + USERS_DB_ROOT + "/" + userKey + "/" + ACTIONS_NODE_NAME + "/" + actionKey , actionValues);
        mDatabase.child(USERS_DB_ROOT).child(userKey).child(ACTIONS_NODE_NAME).child(actionKey).addValueEventListener(actionListener);
        mDatabase.updateChildren(userUpdates);
    }

    public void writeActionToDatabase(final ActionRecord actionRecord) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                writeNewAction(actionRecord);
            }
        });
    }

    private ValueEventListener actionListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            final DataSnapshot snapshot = dataSnapshot;
            // Get Post object and use the values to update the UI
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    ActionRecord actionRecord = snapshot.getValue(ActionRecord.class);
                    if (actionRecord != null) {
                        DobbyLog.v("Wrote to User record: " + actionRecord.toString());
                    } else {
                        DobbyLog.v("Got null record from db");
                    }
                }
            });
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {
            // Getting Post failed, log a message
            final DatabaseError error = databaseError;
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    DobbyLog.w("loadPost:onCancelled" + error.toException());
                }
            });
        }
    };
}
