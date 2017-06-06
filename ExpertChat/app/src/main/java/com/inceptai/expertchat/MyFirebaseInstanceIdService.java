package com.inceptai.expertchat;

import android.util.Log;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

public class MyFirebaseInstanceIdService extends FirebaseInstanceIdService {

    private static final String TAG = "MyFirebaseIIDService";
    private static final String TOKEN_CHILD = "/expert/";

    /**
     * The Application's current Instance ID token is no longer valid and thus a new one must be requested.
     */
    @Override
    public void onTokenRefresh() {
        // If you need to handle the generation of a token, initially or after a refresh this is
        // where you should do that.
        String token = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "FCM Token: " + token);

        String tokenChild = TOKEN_CHILD + Utils.getExpertAvatar(getApplicationContext());
        FirebaseDatabase.getInstance().getReference().child(tokenChild).setValue(token);
        Log.i(TAG, "Writing token to: " + tokenChild);

        // Once a token is generated, we subscribe to topic.
        // FirebaseMessaging.getInstance().subscribeToTopic(FRIENDLY_ENGAGE_TOPIC);
    }
}
