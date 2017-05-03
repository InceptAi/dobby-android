package com.inceptai.dobby.database;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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
public class InferenceDatabaseWriter {
    private static final String INFERENCE_DB_ROOT = "inferences";
    private DatabaseReference mDatabase;
    private ExecutorService executorService;

    @Inject
    public InferenceDatabaseWriter(DobbyThreadpool dobbyThreadpool) {
        mDatabase = FirebaseDatabase.getInstance().getReference();
        executorService = dobbyThreadpool.getExecutorService();
    }

    private void writeNewInference(InferenceRecord inferenceRecord) {
        // Create new inference record at /inferences/$inference-id
        Map<String, Object> childUpdates = new HashMap<>();
        //Update the inferencing
        String inferenceKey = mDatabase.child(INFERENCE_DB_ROOT).push().getKey();
        DobbyLog.i("Inference key: " + inferenceKey);
        Map<String, Object> inferenceValues = inferenceRecord.toMap();
        childUpdates.put("/" + INFERENCE_DB_ROOT + "/" + inferenceKey, inferenceValues);
        mDatabase.child(INFERENCE_DB_ROOT).child(inferenceKey).addValueEventListener(postListener);
        mDatabase.updateChildren(childUpdates);
        //TODO: Update the user index with the inference. Create a user if it doesn't exist.
        //String keyForUserInferenceList = mDatabase.child("users").child(inferenceRecord.uid).child("inferences").push().getKey();
    }

    public void writeInferenceToDatabase(final InferenceRecord inferenceRecord) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                writeNewInference(inferenceRecord);
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
                    InferenceRecord inferenceRecord = snapshot.getValue(InferenceRecord.class);
                    if (inferenceRecord != null) {
                        DobbyLog.v("Wrote to record: " + inferenceRecord.toString());
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
