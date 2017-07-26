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
public class RepairDatabaseWriter {
    private static final String REPAIR_NODE_NAME = "repairs";
    private static final String USERS_NODE_NAME = "users";
    private static final String USERS_DB_ROOT = BuildConfig.FLAVOR + "/" + BuildConfig.BUILD_TYPE + "/" + USERS_NODE_NAME;

    private DatabaseReference mDatabase;
    private ExecutorService executorService;

    @Inject
    public RepairDatabaseWriter(DobbyThreadpool dobbyThreadpool) {
        mDatabase = FirebaseDatabase.getInstance().getReference();
        executorService = dobbyThreadpool.getExecutorService();
    }

    private void writeNewRepair(RepairRecord repairRecord) {
        // Create new inference record at /inferences/$inference-id
        String userKey;
        if (repairRecord.uid != null) {
            userKey = repairRecord.uid;
        } else {
            userKey = mDatabase.child(USERS_DB_ROOT).push().getKey();
        }
        String repairKey = mDatabase.child(USERS_NODE_NAME).child(userKey).child(REPAIR_NODE_NAME).push().getKey();
        Map<String, Object> inferenceValues = repairRecord.toMap();
        DobbyLog.i("Repair key: " + repairKey);
        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("/" + USERS_DB_ROOT + "/" + userKey + "/" + REPAIR_NODE_NAME + "/" + repairKey , inferenceValues);
        mDatabase.child(USERS_DB_ROOT).child(userKey).child(REPAIR_NODE_NAME).child(repairKey).addValueEventListener(failureListener);
        mDatabase.updateChildren(userUpdates);
    }

    public void writeRepairToDatabase(final RepairRecord repairRecord) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                writeNewRepair(repairRecord);
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
                    RepairRecord repairRecord = snapshot.getValue(RepairRecord.class);
                    if (repairRecord != null) {
                        DobbyLog.v("Wrote to record: " + repairRecord.toString());
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
