package com.inceptai.wifiexpertsystem.database;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.inceptai.wifiexpertsystem.BuildConfig;
import com.inceptai.wifiexpertsystem.DobbyThreadPool;
import com.inceptai.wifiexpertsystem.utils.DobbyLog;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by vivek on 4/30/17.
 */
@Singleton
public class FailureDatabaseWriter {
    private static final String FAILURE_NODE_NAME = "failures";
    private static final String USERS_NODE_NAME = "users";
    private static final String USERS_DB_ROOT = BuildConfig.FLAVOR + "/" + BuildConfig.BUILD_TYPE + "/" + USERS_NODE_NAME;

    private DatabaseReference mDatabase;
    private ExecutorService executorService;

    @Inject
    public FailureDatabaseWriter(DobbyThreadPool dobbyThreadpool) {
        mDatabase = FirebaseDatabase.getInstance().getReference();
        executorService = dobbyThreadpool.getExecutorService();
    }

    private void writeNewFailure(FailureRecord failureRecord) {
        // Create new inference record at /inferences/$inference-id
        String userKey;
        if (failureRecord.uid != null) {
            userKey = failureRecord.uid;
        } else {
            userKey = mDatabase.child(USERS_DB_ROOT).push().getKey();
        }
        String failureKey = mDatabase.child(USERS_NODE_NAME).child(userKey).child(FAILURE_NODE_NAME).push().getKey();
        Map<String, Object> inferenceValues = failureRecord.toMap();
        DobbyLog.i("Failure key: " + failureKey);
        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("/" + USERS_DB_ROOT + "/" + userKey + "/" + FAILURE_NODE_NAME + "/" + failureKey , inferenceValues);
        mDatabase.child(USERS_DB_ROOT).child(userKey).child(FAILURE_NODE_NAME).child(failureKey).addValueEventListener(failureListener);
        mDatabase.updateChildren(userUpdates);
    }

    public void writeFailureToDatabase(final FailureRecord failureRecord) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                writeNewFailure(failureRecord);
            }
        });
    }

    private ValueEventListener failureListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            final DataSnapshot snapshot = dataSnapshot;
            // Get Post object and use the values to update the UI
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    FailureRecord failureRecord = snapshot.getValue(FailureRecord.class);
                    if (failureRecord != null) {
                        DobbyLog.v("Wrote to record: " + failureRecord.toString());
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
