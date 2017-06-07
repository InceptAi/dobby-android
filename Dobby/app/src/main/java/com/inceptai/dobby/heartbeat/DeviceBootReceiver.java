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

public class DeviceBootReceiver extends BroadcastReceiver {
    @Inject HeartBeatManager heartBeatManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        ((DobbyApplication) context.getApplicationContext()).getProdComponent().inject(this);
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            heartBeatManager.setAlarm(context);
            DobbyLog.v("Setting the alarm from boot");
            Toast.makeText(context, "Alarm Set", Toast.LENGTH_SHORT).show();
        }
    }
}
