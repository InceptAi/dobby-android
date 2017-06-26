package com.inceptai.expertchat;

import android.util.Log;

import com.google.common.base.Preconditions;
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

    public static UserData fetchUserWith(String userUuid, String flavor, String buildType) {
        Preconditions.checkArgument(Utils.notNullOrEmpty(userUuid));
        Preconditions.checkArgument(Utils.notNullOrEmpty(flavor));
        Preconditions.checkArgument(Utils.notNullOrEmpty(buildType));

        UserData userData = SQLite.select().from(UserData.class).where(UserData_Table.userUuid.eq(userUuid),
                UserData_Table.appFlavor.eq(flavor), UserData_Table.buildType.eq(buildType)).querySingle();
        if (userData == null) {
            userData = new UserData();
            userData.setUserUuid(userUuid);
            userData.setAppFlavor(flavor);
            userData.setBuildType(buildType);
            userData.save();
        }
        return userData;
    }

    public static UserData createOrFetchUser(String userUuid) {
        if (userUuid == null || userUuid.isEmpty()) {
            return null;
        }

        UserData userData = fetchUser(userUuid);
        if (userData == null) {
            userData = new UserData();
            userData.setUserUuid(userUuid);
        }

        if (!userData.hasBuildTypeAndFlavor()) {
            computeUserAppFlavorAndBuild(userData);
        }
        return userData;
    }

    public static List<UserData> fetchUsers() {
        return SQLite.select().from(UserData.class).queryList();
    }

    public static List<UserData> fetchNotifRecents() {
        return SQLite.select().from(UserData.class).where(UserData_Table.interactionType.notEq(UserData.INTERACTION_UNKNOWN)).queryList();
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
                if (userData.appFlavor != null && !userData.appFlavor.isEmpty() && userData.buildType != null && !userData.buildType.isEmpty()) {
                    if (userData.buildType.equals(Utils.BUILD_TYPE_RELEASE) || buildType.equals(Utils.BUILD_TYPE_DEBUG)) {
                        Log.e(TAG, "USERDATA already has app flavor and build type. Not writing duplicates.");
                        future.set(userData);
                        return;
                    }
                    // We do allow overwrite if existing build type is debug.
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
