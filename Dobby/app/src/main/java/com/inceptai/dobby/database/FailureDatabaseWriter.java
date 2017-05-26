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
public class FailureDatabaseWriter {
    private static final String FAILURE_DB_ROOT = BuildConfig.FLAVOR + "/" + BuildConfig.BUILD_TYPE + "/" + "failures";
    private DatabaseReference mDatabase;
    private ExecutorService executorService;

    @Inject
    public FailureDatabaseWriter(DobbyThreadpool dobbyThreadpool) {
        mDatabase = FirebaseDatabase.getInstance().getReference();
        executorService = dobbyThreadpool.getExecutorService();
    }

    private void writeNewFailure(FailureRecord failureRecord) {
        // Create new inference record at /inferences/$inference-id
        Map<String, Object> childUpdates = new HashMap<>();
        //Update the inferencing
        String failureKey = mDatabase.child(FAILURE_DB_ROOT).push().getKey();
        DobbyLog.i("Failure key: " + failureKey);
        Map<String, Object> failureValues = failureRecord.toMap();
        childUpdates.put("/" + FAILURE_DB_ROOT + "/" + failureKey, failureValues);
        mDatabase.child(FAILURE_DB_ROOT).child(failureKey).addValueEventListener(postListener);
        mDatabase.updateChildren(childUpdates);
        //TODO: Update the user index with the inference. Create a user if it doesn't exist.
        //String keyForUserInferenceList = mDatabase.child("users").child(inferenceRecord.uid).child("inferences").push().getKey();
    }

    public void writeFailureToDatabase(final FailureRecord failureRecord) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                writeNewFailure(failureRecord);
            }
        });
    }

    private ValueEventListener postListener = new ValueEventListener() {
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
