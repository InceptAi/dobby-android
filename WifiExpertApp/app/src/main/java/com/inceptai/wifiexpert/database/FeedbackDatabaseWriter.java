package com.inceptai.wifiexpert.database;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.inceptai.wifiexpert.BuildConfig;
import com.inceptai.wifiexpert.DobbyThreadPool;
import com.inceptai.wifiexpert.utils.DobbyLog;

import java.util.Date;
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
    private static final String FEEDBACK_NODE_NAME = "feedbacks";
    private static final String USERS_NODE_NAME = "users";
    //private static final String FEEDBACK_DB_ROOT = BuildConfig.FLAVOR + "/" + BuildConfig.BUILD_TYPE  + "/" + FEEDBACK_NODE_NAME;
    private static final String USERS_DB_ROOT = BuildConfig.FLAVOR + "/" + BuildConfig.BUILD_TYPE + "/" + USERS_NODE_NAME;
    private DatabaseReference mDatabase;
    private ExecutorService executorService;

    @Inject
    public FeedbackDatabaseWriter(DobbyThreadPool dobbyThreadPool) {
        mDatabase = FirebaseDatabase.getInstance().getReference();
        executorService = dobbyThreadPool.getExecutorService();
    }

    private void writeNewFeedback(FeedbackRecord feedbackRecord) {
        String userKey;
        if (feedbackRecord.uid != null) {
            userKey = feedbackRecord.uid;
        } else {
            userKey = mDatabase.child(USERS_DB_ROOT).push().getKey();
        }
        String feedbackKey = mDatabase.child(USERS_NODE_NAME).child(userKey).child(FEEDBACK_NODE_NAME).push().getKey();
        Map<String, Object> inferenceValues = feedbackRecord.toMap();
        DobbyLog.i("Failure key: " + feedbackKey);
        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("/" + USERS_DB_ROOT + "/" + userKey + "/" + FEEDBACK_NODE_NAME + "/" + feedbackKey , inferenceValues);
        mDatabase.child(USERS_DB_ROOT).child(userKey).child(FEEDBACK_NODE_NAME).child(feedbackKey).addValueEventListener(feedbackListener);
        mDatabase.updateChildren(userUpdates);
    }

    private void writeSimpleFeedback(String userUuid, boolean whetherLiked) {
        String feedbackRoot = BuildConfig.FLAVOR + "/" + BuildConfig.BUILD_TYPE + "/" + "new_feedback/";
        feedbackRoot += whetherLiked ? "yes" : "no";
        Date date = new Date();
        mDatabase.child(feedbackRoot).child(userUuid).setValue(date.toString());
    }

    public void writeFeedbackToDatabase(final FeedbackRecord feedbackRecord) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                writeNewFeedback(feedbackRecord);
            }
        });
    }

    public void writeSimpleFeedbackAsync(final String userUuid, final boolean whetherLiked) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                writeSimpleFeedback(userUuid, whetherLiked);
            }
        });
    }

    private ValueEventListener feedbackListener = new ValueEventListener() {
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
