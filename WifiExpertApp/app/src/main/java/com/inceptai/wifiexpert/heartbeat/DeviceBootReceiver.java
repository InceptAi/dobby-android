package com.inceptai.wifiexpert.heartbeat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.inceptai.wifiexpert.DobbyApplication;
import com.inceptai.wifiexpert.utils.DobbyLog;

import javax.inject.Inject;

/**
 * Created by vivek on 6/6/17.
 */

public class DeviceBootReceiver extends BroadcastReceiver {
    @Inject HeartBeatManager heartBeatManager;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!(context.getApplicationContext() instanceof DobbyApplication)) {
            return;
        }
        ((DobbyApplication) context.getApplicationContext()).getProdComponent().inject(this);
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            heartBeatManager.setAlarm(context);
            DobbyLog.v("Setting the alarm from boot");
            //Toast.makeText(context, "Alarm Set", Toast.LENGTH_SHORT).show();
        }
    }
}
