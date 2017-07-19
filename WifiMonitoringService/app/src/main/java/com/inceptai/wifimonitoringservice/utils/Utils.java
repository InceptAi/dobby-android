package com.inceptai.wifimonitoringservice.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;

import com.inceptai.wifimonitoringservice.WifiMonitoringService;

import java.lang.reflect.Field;

import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1;
import static android.os.Build.VERSION_CODES.JELLY_BEAN_MR1;

/**
 * Created by vivek on 7/11/17.
 */

public class Utils {
    public static String EMPTY_STRING = "";
    public static int computeMovingAverageSignal(int currentSignal, int previousSignal, long currentSeen, long previousSeen, int maxAge) {
        if (currentSeen == 0) {
            currentSeen = System.currentTimeMillis();
        }
        long age = currentSeen - previousSeen;
        if (previousSeen > 0 && age > 0 && age < maxAge / 2) {
            // Average the RSSI with previously seen instances of this scan result
            double alpha = 0.5 - (double) age / (double) maxAge;
            currentSignal = (int) ((double) currentSignal * (1 - alpha) + (double) previousSignal * alpha);
        }
        return currentSignal;
    }

    /**
     * Checks whether the "Avoid poor networks" setting (named "Auto network switch" on
     * some Samsung devices) is enabled, which can in some instances interfere with Wi-Fi.
     *
     * @return true if the "Avoid poor networks" or "Auto network switch" setting is enabled
     */
    public static boolean isPoorNetworkAvoidanceEnabled (Context ctx) {
        final int SETTING_UNKNOWN = -1;
        final int SETTING_ENABLED = 1;
        final String AVOID_POOR = "wifi_watchdog_poor_network_test_enabled";
        final String WATCHDOG_CLASS = "android.net.wifi.WifiWatchdogStateMachine";
        final String DEFAULT_ENABLED = "DEFAULT_POOR_NETWORK_AVOIDANCE_ENABLED";
        final ContentResolver cr = ctx.getContentResolver();

        int result;

        if (SDK_INT >= JELLY_BEAN_MR1) {
            //Setting was moved from Secure to Global as of JB MR1
            result = Settings.Global.getInt(cr, AVOID_POOR, SETTING_UNKNOWN);
        } else if (SDK_INT >= ICE_CREAM_SANDWICH_MR1) {
            result = Settings.Secure.getInt(cr, AVOID_POOR, SETTING_UNKNOWN);
        } else {
            //Poor network avoidance not introduced until ICS MR1
            //See android.provider.Settings.java
            return false;
        }

        //Exit here if the setting value is known
        if (result != SETTING_UNKNOWN) {
            return (result == SETTING_ENABLED);
        }

        //Setting does not exist in database, so it has never been changed.
        //It will be initialized to the default value.
        if (SDK_INT >= JELLY_BEAN_MR1) {
            //As of JB MR1, a constant was added to WifiWatchdogStateMachine to determine
            //the default behavior of the Avoid Poor Networks setting.
            try {
                //In the case of any failures here, take the safe route and assume the
                //setting is disabled to avoid disrupting the user with false information
                Class wifiWatchdog = Class.forName(WATCHDOG_CLASS);
                Field defValue = wifiWatchdog.getField(DEFAULT_ENABLED);
                if (!defValue.isAccessible()) defValue.setAccessible(true);
                return defValue.getBoolean(null);
            } catch (IllegalAccessException ex) {
                return false;
            } catch (NoSuchFieldException ex) {
                return false;
            } catch (ClassNotFoundException ex) {
                return false;
            } catch (IllegalArgumentException ex) {
                return false;
            }
        } else {
            //Prior to JB MR1, the default for the Avoid Poor Networks setting was
            //to enable it unless explicitly disabled
            return true;
        }
    }

    /** Gets a String resource from another installed application */
    public static String getExternalString(Context ctx, String namespace,
                                           String key, String defVal) {
        int resId = getExternalIdentifier(ctx, namespace, key, "string");
        if (resId != 0) {
            Resources res = getExternalResources(ctx, namespace);
            return res.getString(resId);
        } else {
            return defVal;
        }
    }

    //Broadcast notification info
    public static void sendNotificationInfo(Context context, String title, String body, int notificationId) {
        ServiceLog.v("Broadcasting message");
        Intent intent = new Intent(WifiMonitoringService.NOTIFICATION_INFO_INTENT_VALUE);
        intent.putExtra(WifiMonitoringService.EXTRA_NOTIFICATION_TITLE, title);
        intent.putExtra(WifiMonitoringService.EXTRA_NOTIFICATION_BODY, body);
        intent.putExtra(WifiMonitoringService.EXTRA_NOTIFICATION_ID, notificationId);
        // You can also include some extra data.
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
    }

    //Private stuff
    /** Gets the resources of another installed application */
    private static Resources getExternalResources(Context ctx, String namespace) {
        PackageManager pm = ctx.getPackageManager();
        try {
            return (pm == null) ? null : pm.getResourcesForApplication(namespace);
        } catch (PackageManager.NameNotFoundException ex) {
            return null;
        }
    }

    /** Gets a resource ID from another installed application */
    private static int getExternalIdentifier(Context ctx, String namespace,
                                             String key, String type) {
        Resources res = getExternalResources(ctx, namespace);
        return (res == null) ? 0 : res.getIdentifier(key, type, namespace);
    }



}