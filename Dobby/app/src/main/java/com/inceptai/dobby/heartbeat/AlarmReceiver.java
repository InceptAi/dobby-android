package com.inceptai.dobby.heartbeat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.utils.DobbyLog;

import javax.inject.Inject;

/**
 * Created by vivek on 6/6/17.
 */

public class AlarmReceiver extends BroadcastReceiver {

    @Inject
    HeartBeatManager heartBeatManager;

    public AlarmReceiver() {}

    @Override
    public void onReceive(Context context, Intent intent) {
        ((DobbyApplication) context.getApplicationContext()).getProdComponent().inject(this);
        final String action = intent.getAction();
        if (action.equals(HeartBeatManager.DAILY_HEARTBEAT_INTENT)) {
            //String userId = intent.getStringExtra(USER_ID);
            //Send analytics event to firebase here
            // For our recurring task, we'll just display a message
            DobbyLog.v("OnReceive Alarm Manager.");
            Toast.makeText(context, "I'm running", Toast.LENGTH_SHORT).show();
            heartBeatManager.performHeartBeatTask();
        }
    }
}
