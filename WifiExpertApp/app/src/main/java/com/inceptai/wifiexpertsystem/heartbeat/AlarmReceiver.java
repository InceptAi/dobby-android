package com.inceptai.wifiexpertsystem.heartbeat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.inceptai.wifiexpertsystem.DobbyApplication;
import com.inceptai.wifiexpertsystem.utils.DobbyLog;

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
        if (!(context.getApplicationContext() instanceof DobbyApplication)) {
            return;
        }
        ((DobbyApplication) context.getApplicationContext()).getProdComponent().inject(this);
        final String action = intent.getAction();
        if (action.equals(HeartBeatManager.DAILY_HEARTBEAT_INTENT)) {
            //String userId = intent.getStringExtra(USER_ID);
            //Send analytics event to firebase here
            // For our recurring task, we'll just display a message
            DobbyLog.v("OnReceive Alarm Manager.");
            //Toast.makeText(context, "I'm running", Toast.LENGTH_SHORT).show();
            heartBeatManager.performHeartBeatTask();
        }
    }
}
