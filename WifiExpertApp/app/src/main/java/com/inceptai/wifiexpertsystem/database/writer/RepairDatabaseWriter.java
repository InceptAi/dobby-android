package com.inceptai.wifiexpertsystem.database.writer;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.inceptai.wifiexpertsystem.BuildConfig;
import com.inceptai.wifiexpertsystem.DobbyThreadPool;
import com.inceptai.wifiexpertsystem.database.model.RepairRecord;
import com.inceptai.wifiexpertsystem.utils.DobbyLog;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Created by vivek on 4/30/17.
 */
public class RepairDatabaseWriter {
    private static final String REPAIR_NODE_NAME = "repairs";
    private static final String USERS_NODE_NAME = "users";
    private static final String USERS_DB_ROOT = BuildConfig.FLAVOR + "/" + BuildConfig.BUILD_TYPE + "/" + USERS_NODE_NAME;
    private static final String REPAIRS_DB_ROOT = BuildConfig.FLAVOR + "/" + BuildConfig.BUILD_TYPE + "/" + REPAIR_NODE_NAME;

    private DatabaseReference mDatabase;
    private ExecutorService executorService;

    public RepairDatabaseWriter(DobbyThreadPool dobbyThreadpool) {
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
        Map<String, Object> repairValues = repairRecord.toMap();
        DobbyLog.i("Repair key: " + repairKey);

        //Writing to users/UUID/repairs/repairKey
        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("/" + USERS_DB_ROOT + "/" + userKey + "/" + REPAIR_NODE_NAME + "/" + repairKey , repairValues);
        mDatabase.child(USERS_DB_ROOT).child(userKey).child(REPAIR_NODE_NAME).child(repairKey).addValueEventListener(failureListener);
        mDatabase.updateChildren(userUpdates);

        //Writing to repairs/repairKeyEndPoint
        Map<String, Object> repairUpdates = new HashMap<>();
        repairUpdates.put("/" + REPAIRS_DB_ROOT + "/" + repairKey , repairValues);
        mDatabase.updateChildren(repairUpdates);

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
