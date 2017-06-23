package com.inceptai.expertchat;

import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.util.List;

import static com.inceptai.expertchat.Utils.TAG;

/**
 * Created by arunesh on 6/22/17.
 */

public class UserDataBackend {

    public static UserData fetchUser(String userUuid) {
        return SQLite.select().from(UserData.class).where(UserData_Table.userUuid.eq(userUuid)).querySingle();
    }

    public static List<UserData> fetchUsers() {
        return SQLite.select().from(UserData.class).queryList();
    }

    public static ListenableFuture<UserData> computeUserAppFlavorAndBuild(UserData userData) {
        String userUuid = userData.getUserUuid();
        if (userUuid == null || userUuid.isEmpty()) {
            Log.i(TAG, "Null or empty UUID for firebase flavor/build fetch.");
            return null;
        }
        SettableFuture<UserData> future = SettableFuture.create();
        new UserPathListener(Utils.BUILD_TYPE_DEBUG, Utils.DOBBY_FLAVOR, userData, future).tryFetch();
        new UserPathListener(Utils.BUILD_TYPE_RELEASE, Utils.DOBBY_FLAVOR, userData, future).tryFetch();
        new UserPathListener(Utils.BUILD_TYPE_DEBUG, Utils.WIFIDOC_FLAVOR, userData, future).tryFetch();
        new UserPathListener(Utils.BUILD_TYPE_RELEASE, Utils.WIFIDOC_FLAVOR, userData, future).tryFetch();
        return future;
    }

    private static class UserPathListener implements ValueEventListener {

        String buildType;
        String flavor;
        UserData userData;
        SettableFuture<UserData> future;

        UserPathListener(String buildType, String flavor, UserData userData, SettableFuture<UserData> future) {
            this.buildType = buildType;
            this.flavor = flavor;
            this.future = future;
            this.userData = userData;
        }

        void tryFetch() {
            String path = flavor + "/" + buildType + "/users/" + userData.getUserUuid();
            FirebaseDatabase.getInstance().getReference(path).addListenerForSingleValueEvent(this);
        }

        @Override
        public void onDataChange(DataSnapshot dataSnapshot) {
            if (dataSnapshot.getValue() != null) {
                if (!userData.appFlavor.isEmpty() && !userData.buildType.isEmpty()) {
                    Log.e(TAG, "USERDATA already has app flavor and build type. Not writing duplicates.");
                    future.set(userData);
                    return;
                }
                userData.appFlavor = flavor;
                userData.buildType = buildType;
                userData.save();
                future.set(userData);
            }
        }

        @Override
        public void onCancelled(DatabaseError databaseError) {

        }
    }
}
