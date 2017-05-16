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
public class FeedbackDatabaseWriter {
    private static final String FEEDBACK_DB_ROOT = BuildConfig.FLAVOR + "/" + BuildConfig.BUILD_TYPE  + "/" + "feedbacks";
    private DatabaseReference mDatabase;
    private ExecutorService executorService;

    @Inject
    public FeedbackDatabaseWriter(DobbyThreadpool dobbyThreadpool) {
        mDatabase = FirebaseDatabase.getInstance().getReference();
        executorService = dobbyThreadpool.getExecutorService();
    }

    private void writeNewFeedback(FeedbackRecord feedbackRecord) {
        // Create new inference record at /inferences/$inference-id
        Map<String, Object> childUpdates = new HashMap<>();
        //Update the inferencing
        String feedbackKey = mDatabase.child(FEEDBACK_DB_ROOT).push().getKey();
        DobbyLog.i("feedback key: " + feedbackKey);
        Map<String, Object> feedbackValues = feedbackRecord.toMap();
        childUpdates.put("/" + FEEDBACK_DB_ROOT + "/" + feedbackKey, feedbackValues);
        mDatabase.child(FEEDBACK_DB_ROOT).child(feedbackKey).addValueEventListener(feedbackPostListener);
        mDatabase.updateChildren(childUpdates);
    }

    public void writeFeedbackToDatabase(final FeedbackRecord feedbackRecord) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                writeNewFeedback(feedbackRecord);
            }
        });
    }

    private ValueEventListener feedbackPostListener = new ValueEventListener() {
        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            final DataSnapshot snapshot = dataSnapshot;
            // Get Post object and use the values to update the UI
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    FeedbackRecord feedbackRecord = snapshot.getValue(FeedbackRecord.class);
                    if (feedbackRecord != null) {
                        DobbyLog.v("Wrote to record: " + feedbackRecord.toString());
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
