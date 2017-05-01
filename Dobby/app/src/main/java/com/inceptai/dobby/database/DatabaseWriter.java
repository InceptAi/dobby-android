package com.inceptai.dobby.database;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.inceptai.dobby.DobbyThreadpool;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by vivek on 4/30/17.
 */
@Singleton
public class DatabaseWriter {
    private DatabaseReference mDatabase;
    private ExecutorService executorService;

    @Inject
    public DatabaseWriter(DobbyThreadpool dobbyThreadpool) {
        mDatabase = FirebaseDatabase.getInstance().getReference();
        executorService = dobbyThreadpool.getExecutorService();
    }

    private void writeNewInference(InferenceRecord inferenceRecord) {
        // Create new inference record at /inferences/$inferenceid
        
        Map<String, Object> childUpdates = new HashMap<>();
        //Update the inferencing
        String inferenceKey = mDatabase.child("inferences").push().getKey();
        Map<String, Object> inferenceValues = inferenceRecord.toMap();
        childUpdates.put("/inferences/" + inferenceKey, inferenceValues);
        //TODO: Update the user index with the inference. Create a user if it doesn't exist.
        //String keyForUserInferenceList = mDatabase.child("users").child(inferenceRecord.uid).child("inferences").push().getKey();
        //childUpdates.put("/inferences/" + inferenceRecord.uid + "/" + key, inferenceValues);
        mDatabase.updateChildren(childUpdates);

        //mDatabase.child("inferences").setValue("Hello World");
    }

    public void writeInferenceToDatabase(final InferenceRecord inferenceRecord) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                writeNewInference(inferenceRecord);
            }
        });
    }

}
