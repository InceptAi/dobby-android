package com.inceptai.dobby.notifications;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.expert.ExpertChatService;
import com.inceptai.dobby.utils.DobbyLog;

import javax.inject.Inject;

/**
 * Created by arunesh on 6/5/17.
 */

public class FirebaseIdService extends FirebaseInstanceIdService {
    @Inject
    ExpertChatService expertChatService;

    @Override
    public void onTokenRefresh() {
        DobbyApplication application = (DobbyApplication) getApplicationContext();
        application.getProdComponent().inject(this);
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        DobbyLog.i("Refreshed token: " + refreshedToken);

        // TODO: Implement this method to send any registration to your app's servers.
        sendRegistrationToServer(refreshedToken);
    }

    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private void sendRegistrationToServer(String token) {
        expertChatService.saveFcmToken(token);
        DobbyLog.i("Saved FCM token: " + token);
    }
}
