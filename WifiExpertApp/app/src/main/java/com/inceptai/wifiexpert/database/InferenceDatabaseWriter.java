package com.inceptai.wifiexpert.database;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.inceptai.wifiexpert.BuildConfig;
import com.inceptai.wifiexpert.DobbyThreadPool;
import com.inceptai.wifiexpert.utils.DobbyLog;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by vivek on 4/30/17.
 */
@Singleton
public class InferenceDatabaseWriter {
    private static final String INFERENCES_NODE_NAME = "inferences";
    private static final String USERS_NODE_NAME = "users";
    //private static final String INFERENCE_DB_ROOT = BuildConfig.FLAVOR + "/" + BuildConfig.BUILD_TYPE + "/" + INFERENCES_NODE_NAME;
    private static final String USERS_DB_ROOT = BuildConfig.FLAVOR + "/" + BuildConfig.BUILD_TYPE + "/" + USERS_NODE_NAME;
    private DatabaseReference mDatabase;
    private ExecutorService executorService;

    @Inject
    public InferenceDatabaseWriter(DobbyThreadPool dobbyThreadpool) {
        mDatabase = FirebaseDatabase.getInstance().getReference();
        executorService = dobbyThreadpool.getExecutorService();
    }

    private void writeNewInference(InferenceRecord inferenceRecord) {
        // Create new inference record at /inferences/$inference-id
        String userKey;
        if (inferenceRecord.uid != null) {
            userKey = inferenceRecord.uid;
        } else {
            userKey = mDatabase.child(USERS_DB_ROOT).push().getKey();
        }
        String inferenceKey = mDatabase.child(USERS_NODE_NAME).child(userKey).child(INFERENCES_NODE_NAME).push().getKey();
        Map<String, Object> inferenceValues = inferenceRecord.toMap();
        DobbyLog.i("Inference key: " + inferenceKey);
        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("/" + USERS_DB_ROOT + "/" + userKey + "/" + INFERENCES_NODE_NAME + "/" + inferenceKey , inferenceValues);
        mDatabase.child(USERS_DB_ROOT).child(userKey).child(INFERENCES_NODE_NAME).child(inferenceKey).addValueEventListener(inferenceListener);
        mDatabase.updateChildren(userUpdates);
    }

    public void writeInferenceToDatabase(final InferenceRecord inferenceRecord) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                writeNewInference(inferenceRecord);
            }
        });
    }

    private ValueEventListener inferenceListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            final DataSnapshot snapshot = dataSnapshot;
            // Get Post object and use the values to update the UI
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    InferenceRecord inferenceRecord = snapshot.getValue(InferenceRecord.class);
                    if (inferenceRecord != null) {
                        DobbyLog.v("Wrote to User record: " + inferenceRecord.toString());
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
