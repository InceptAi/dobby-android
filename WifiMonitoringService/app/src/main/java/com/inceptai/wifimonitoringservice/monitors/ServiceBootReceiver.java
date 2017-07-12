package com.inceptai.wifimonitoringservice.monitors;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.inceptai.wifimonitoringservice.utils.ServiceAlarm;

/**
 * Created by vivek on 7/10/17.
 */

public class ServiceBootReceiver extends BroadcastReceiver {
    /*
     * The idea here is that we want something lightweight to run at
	 * BOOT_COMPLETED, so a minimal BroadcastReceiver implementation.
	 *
	 * Because of BroadcastReceiver lifecycle, a thread started from it will be
	 * GCed. So we're starting a minimal service, BootService, which runs a wait
	 * thread which launches WFMonitorService after 30 seconds
	 *
	 * @see android.content.BroadcastReceiver#onReceive(android.content.Context,
	 * android.content.Intent)
	 */

    @Override
    public void onReceive(Context context, Intent intent) {
		/*
		 * For boot completed, check DISABLE SERVICE if false, start the service
		 * loader run
		 */
//        if (!PrefUtil.readBoolean(context, Pref.DISABLESERVICE.key())) {
//        }
        ServiceAlarm.setOneTimServiceWatchDogAlarmWithDelay(context.getApplicationContext());
    }
}