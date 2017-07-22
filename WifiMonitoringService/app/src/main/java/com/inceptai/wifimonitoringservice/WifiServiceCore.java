package com.inceptai.wifimonitoringservice;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ConnectivityTester;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.FutureAction;
import com.inceptai.wifimonitoringservice.monitors.PeriodicCheckMonitor;
import com.inceptai.wifimonitoringservice.monitors.ScreenStateMonitor;
import com.inceptai.wifimonitoringservice.monitors.WifiStateMonitor;
import com.inceptai.wifimonitoringservice.utils.ServiceLog;
import com.inceptai.wifimonitoringservice.utils.Utils;

import java.util.HashSet;
import java.util.Set;

import static com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ConnectivityTester.WifiConnectivityMode.CONNECTED_AND_CAPTIVE_PORTAL;
import static com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ConnectivityTester.WifiConnectivityMode.CONNECTED_AND_OFFLINE;
import static com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ConnectivityTester.WifiConnectivityMode.CONNECTED_AND_ONLINE;
import static com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ConnectivityTester.WifiConnectivityMode.CONNECTED_AND_UNKNOWN;
import static com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ConnectivityTester.WifiConnectivityMode.OFF;
import static com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ConnectivityTester.WifiConnectivityMode.ON_AND_DISCONNECTED;
import static com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ConnectivityTester.WifiConnectivityMode.UNKNOWN;

/**
 * Created by vivek on 7/10/17.
 */

public class WifiServiceCore implements
        WifiStateMonitor.WifiStateCallback,
        ScreenStateMonitor.ScreenStateCallback,
        PeriodicCheckMonitor.PeriodicCheckCallback,
        ServiceActionTaker.ActionCallback {
    private final static int MAX_CONNECTIVITY_TEST_FAILURES = 2;
    private final static int WIFI_CHECK_INITIAL_CHECK_DELAY_ACTIVE_MS = 10 * 1000; // 5 sec
    private final static int WIFI_CHECK_INITIAL_CHECK_DELAY_INACTIVE_MS = 60 * 1000; // 60 sec
    private final static int WIFI_CHECK_PERIOD_SCREEN_ACTIVE_MS = 60 * 1000; //30 secs
    private final static int WIFI_CHECK_PERIOD_SCREEN_INACTIVE_MS = 5 * 60 * 1000; //5 mins
    private final static boolean ENABLE_CHECK_WHEN_SCREEN_INACTIVE = false;
    private final static boolean ENABLE_AGGRESSIVE_RECONNECT_WHEN_CONNECTION_DROPS = false;
    private final static long ACTION_PENDING_TIMEOUT = 60 * 1000; //60 secs



    //Key components
    private WifiStateMonitor wifiStateMonitor;
    private ScreenStateMonitor screenStateMonitor;
    private ServiceActionTaker serviceActionTaker;
    private PeriodicCheckMonitor periodicCheckMonitor;
    private ServiceThreadPool serviceThreadPool;
    private Context context;
    private Set<String> listOfOfflineRouterIDs;
    private String notificationIntentToBroadcast;
    private String lastActionTakenDescription;
    private long lastActionTimestampMs;
    private String lastWifiEventDescription;
    private long lastWifiEventTimestampMs;
    //Key state
    private int numConsecutiveFailedConnectivityTests;
    private static WifiServiceCore WIFI_SERVICE_CORE;

    private long wifiCheckInitialDelayMs;
    private long wifiCheckPeriodMs;
    private boolean notifiedOfConnectivityPass;
    @ConnectivityTester.WifiConnectivityMode
    private int wifiConnectivityMode;
    private long actionPendingTimestampMs;

    //TODO: Move to repair if reset doesn't work for n number of times
    private WifiServiceCore(Context context, ServiceThreadPool serviceThreadPool) {
        this.context = context;
        this.serviceThreadPool = serviceThreadPool;
        wifiStateMonitor = new WifiStateMonitor(context);
        screenStateMonitor = new ScreenStateMonitor(context);
        periodicCheckMonitor = new PeriodicCheckMonitor(context);
        serviceActionTaker = new ServiceActionTaker(context, serviceThreadPool.getExecutor(), serviceThreadPool.getScheduledExecutorServiceForActions());
        numConsecutiveFailedConnectivityTests = 0;
        wifiCheckInitialDelayMs = WIFI_CHECK_INITIAL_CHECK_DELAY_ACTIVE_MS;
        wifiCheckPeriodMs = WIFI_CHECK_PERIOD_SCREEN_ACTIVE_MS;
        listOfOfflineRouterIDs = new HashSet<>();
        notificationIntentToBroadcast = WifiMonitoringService.NOTIFICATION_INFO_INTENT_VALUE;
        lastActionTakenDescription = Utils.EMPTY_STRING;
        lastActionTimestampMs = 0;
        lastWifiEventDescription = Utils.EMPTY_STRING;
        lastWifiEventTimestampMs = 0;
        actionPendingTimestampMs = 0;
    }

    /**
     * Factory constructor to create an instance
     *
     * @param context Application context.
     * @return Instance of WifiAnalyzer or null on error.
     */
    @Nullable
    public static WifiServiceCore create(Context context, ServiceThreadPool serviceThreadPool) {
        if (context == null) {
            return null;
        }
        if (WIFI_SERVICE_CORE == null) {
            return new WifiServiceCore(context, serviceThreadPool);
        }
        return WIFI_SERVICE_CORE;
    }

    public void setNotificationIntent(String notificationIntent) {
        if (notificationIntent != null) {
            notificationIntentToBroadcast = notificationIntent;
        }
    }

    void startMonitoring() {
        wifiStateMonitor.registerCallback(this);
        screenStateMonitor.registerCallback(this);
        serviceActionTaker.registerCallback(this);
        ServiceLog.v("StartMonitoring ");
    }

    void cleanup() {
        wifiStateMonitor.cleanup();
        screenStateMonitor.cleanup();
        periodicCheckMonitor.cleanup();
        serviceActionTaker.cleanup();
        serviceThreadPool.shutdown();
        ServiceLog.v("Cleanup ");
    }

    ListenableFuture<ActionResult> forceRepairWifiNetwork() {
        cancelChecksAndPendingActions();
        return serviceActionTaker.iterateAndRepairConnection();
    }

    void sendStatusUpdateNotification() {
        sendWifiStatusNotification();
    }

    //Overrides for periodic check
    @Override
    public void actionStarted(String actionName) {
        ServiceLog.v("Action started  " + actionName);
        markActionPending();
    }

    @Override
    public void actionCompleted(String actionName, ActionResult actionResult) {
        ServiceLog.v("Action finished  " + actionName);
        if (actionResult == null) {
            ServiceLog.v("Action result: null");
        } else {
            ServiceLog.v("Action result: " + actionResult.getStatusString());
        }
        markActionDone();
        if (ActionResult.isSuccessful(actionResult)) {
            lastActionTimestampMs = System.currentTimeMillis();
            lastActionTakenDescription = actionName;
            sendWifiStatusNotification();
        }
    }

    @Override
    public void checkFired() {
        //Perform wifi connectivity test
        ServiceLog.v("Check fired actionPending " + (isActionPending() ? "True" : "False"));
        if (!isActionPending() && ConnectivityTester.isConnected(wifiConnectivityMode)) {
            ServiceLog.v("Initiating connectivity test");
            performConnectivityTest();
        }
    }

    //Overrides for screen state check
    @Override
    public void onScreenStateOn() {
        //Start the wifi check alarm -- first one fires after 1 min and then every 5 mins
        ServiceLog.v("Screen state on");
        wifiCheckInitialDelayMs = WIFI_CHECK_INITIAL_CHECK_DELAY_ACTIVE_MS;
        wifiCheckPeriodMs = WIFI_CHECK_PERIOD_SCREEN_ACTIVE_MS;
        startWifiCheck();
    }

    @Override
    public void onScreenStateOff() {
        //Disable wifi check for now in screen off mode
        ServiceLog.v("Screen state off");
        wifiCheckInitialDelayMs = WIFI_CHECK_INITIAL_CHECK_DELAY_INACTIVE_MS;
        wifiCheckPeriodMs = WIFI_CHECK_PERIOD_SCREEN_INACTIVE_MS;
        if (!ENABLE_CHECK_WHEN_SCREEN_INACTIVE) {
            periodicCheckMonitor.disableCheck();
        }
    }

    //Overrides for wifi state
    @Override
    public void wifiStateEnabled() {
        ServiceLog.v("Wifi enabled");
        //Start the wifi check alarm -- first one fires after 1 min and then every 5 mins
        updateWifiConnectivityMode(ConnectivityTester.WifiConnectivityMode.ON_AND_DISCONNECTED);
    }

    @Override
    public void wifiStateDisabled() {
        //Stop the wifi check alarm
        ServiceLog.v("Wifi disabled");
        cancelChecksAndPendingActions();
        updateWifiConnectivityMode(ConnectivityTester.WifiConnectivityMode.OFF);
    }

    @Override
    public void wifiStateDisconnected() {
        //Start the wifi check alarm -- first one fires after 10 secs and then every 5 mins
//        periodicCheckMonitor.enableCheck(WIFI_CHECK_INITIAL_CHECK_DELAY_ACTIVE_MS,
//                WIFI_CHECK_PERIOD_SCREEN_ACTIVE_MS, this);
        ServiceLog.v("Wifi state disconnected");
        updateWifiConnectivityMode(ConnectivityTester.WifiConnectivityMode.ON_AND_DISCONNECTED);
        notifiedOfConnectivityPass = false;
        startWifiCheck();
    }

    @Override
    public void wifiStateConnected() {
        //Stop the wifi check alarm -- for now -- we will get callbacks for low snr etc. --
        // what if wifi loses connectivity in between -- we will get callback from ConnectivityManager.
        ServiceLog.v("Wifi state connected");
        updateWifiConnectivityMode(ConnectivityTester.WifiConnectivityMode.CONNECTED_AND_UNKNOWN);
        startWifiCheck();
    }

    @Override
    public void wifiStatePrimaryAPSignalLevelChanged() {
        if (ConnectivityTester.isConnected(wifiConnectivityMode)) {
            ServiceLog.v("Sending wifi status as signal level changed");
            sendWifiStatusNotification();
        }
    }

    //Problems
    @Override
    public void wifiPrimaryAPSignalLow() {
        //scan and reconnect to other stronger AP if available.
        ServiceLog.v("wifi primary AP signal low");
        ServiceLog.v("Sending wifi status as signal level low");
        if (ConnectivityTester.isConnected(wifiConnectivityMode)) {
            sendWifiStatusNotification();
        }
        if (!isActionPending()) {
            ServiceLog.v("ConnectToBestWifi initiated");
            sendNotificationOfServiceActionStarted(context.getString(R.string.connect_to_best_wifi),
                    context.getString(R.string.wifi_signal_poor));
            serviceActionTaker.connectToBestWifi(listOfOfflineRouterIDs);
        }
    }

    @Override
    public void wifiStateHangingOnScanning() {
        //Router not visible -- toggle wifi and re-associate.
        ServiceLog.v("wifiStateHangingOnScanning");
        if (!isActionPending()) {
            ServiceLog.v("RepairConnection initiated");
            sendNotificationOfServiceActionStarted(context.getString(R.string.repair_wifi_network),
                    context.getString(R.string.wifi_stuck_scanning));
            serviceActionTaker.repairConnection();
        }
    }

    @Override
    public void wifiStateHangingOnObtainingIPAddress() {
        //Disconnect and then re-associate with same router
        //TODO: Should we reset or repair ??
        ServiceLog.v("wifiStateHangingOnObtainingIPAddress");
        if (!isActionPending()) {
            ServiceLog.v("RepairConnection initiated");
            sendNotificationOfServiceActionStarted(context.getString(R.string.reset_connection_to_current_wifi),
                    context.getString(R.string.wifi_stuck_ip));
            serviceActionTaker.resetConnection();
        }
    }

    @Override
    public void wifiStateHangingOnAuthenticating() {
        //Disconnect and  try to reconnect
        //TODO: Should we reset or repair ??
        ServiceLog.v("wifiStateHangingOnAuthenticating");
        if (!isActionPending()) {
            ServiceLog.v("ResetConnection initiated");
            sendNotificationOfServiceActionStarted(context.getString(R.string.reset_connection_to_current_wifi),
                    context.getString(R.string.wifi_stuck_authenticating));
            serviceActionTaker.resetConnection();
        }
    }


    @Override
    public void wifiStateHangingOnConnecting() {
        ServiceLog.v("wifiStateHangingOnConnecting");
        if (!isActionPending()) {
            ServiceLog.v("ResetConnection initiated");
            sendNotificationOfServiceActionStarted(context.getString(R.string.reset_connection_to_current_wifi),
                    context.getString(R.string.wifi_stuck_connecting));
            serviceActionTaker.resetConnection();
        }
    }

    @Override
    public void wifiStateFrequentDropOff() {
        //If ping ponging -- then increase the priority of one network significantly
        //If just one wifi and signal is strong -- check smart network switch
        //If signal is on the fringe, demote this network and connect to a better network if possible.
        ServiceLog.v("wifiStateFrequentDropOff");
        if (Utils.isPoorNetworkAvoidanceEnabled(context)) {
            //TODO: Show a message to the user to disable Smart Network Switch -- could be causing issues
            sendNotificationOfUserActionNeeded(context.getString(R.string.wifi_turn_off_smart_switch), context.getString(R.string.wifi_frequent_dropoff));
        }
        if (!isActionPending() && !ConnectivityTester.isConnected(wifiConnectivityMode)) {
            ServiceLog.v("ConnectToBestWifi");
            sendNotificationOfServiceActionStarted(context.getString(R.string.connect_to_best_wifi),
                    context.getString(R.string.wifi_frequent_dropoff));
            serviceActionTaker.connectToBestWifi(listOfOfflineRouterIDs);
        }
    }

    @Override
    public void wifiStateErrorAuthenticating() {
        //Tell user that passphrase is wrong
        //TODO: Show a message to the user to check the passphrase for the network --
        //TODO: maybe try connecting to another network ?
        sendNotificationOfUserActionNeeded(context.getString(R.string.wifi_check_password), context.getString(R.string.wifi_authentication_error));
        ServiceLog.v("wifiStateErrorAuthenticating");
        if (!isActionPending()) {
            ServiceLog.v("ConnectToBestWifi");
            sendNotificationOfServiceActionStarted(context.getString(R.string.connect_to_best_wifi),
                    context.getString(R.string.wifi_authentication_error));
            serviceActionTaker.connectToBestWifi(listOfOfflineRouterIDs);
        }
    }

    @Override
    public void wifiStateProblematicSupplicantPattern() {
        //Toggle wifi
        ServiceLog.v("wifiStateProblematicSupplicantPattern");
        if (!isActionPending()) {
            ServiceLog.v("repairConnection");
            sendNotificationOfServiceActionStarted(context.getString(R.string.repair_wifi_network),
                    context.getString(R.string.wifi_bad_state));
            serviceActionTaker.repairConnection();
        }
    }

    @Override
    public void wifiNetworkAcquiredDataConnectivity() {
        //Perform connectivity test
        ServiceLog.v("wifiNetworkAcquiredDataConnectivity");
        numConsecutiveFailedConnectivityTests = 0;
        startWifiCheck(); // Keep checking to make sure we have connectivity
        //periodicCheckMonitor.disableCheck();
    }

    @Override
    public void wifiNetworkLostDataConnectivity() {
        //Check wifi signal -- if strong signal to a network which was validated to have Internet,
        // then trigger reconnect -- aggressive mode
        ServiceLog.v("wifiNetworkLostDataConnectivity: starting check");
        startWifiCheck();
    }

    @Override
    public void wifiNetworkInvalidOrInactiveOrDormant() {
        //Toggle wifi
        ServiceLog.v("wifiNetworkInvalidOrInactiveOrDormant");
        if (!isActionPending()) {
            ServiceLog.v("toggleWifi");
            sendNotificationOfServiceActionStarted(context.getString(R.string.toggle_wifi_off_and_on),
                    context.getString(R.string.wifi_inactive_state));
            serviceActionTaker.toggleWifi();
        }
    }

    @Override
    public void wifiNetworkDisconnectedUnexpectedly() {
        //Reconnect with best AP or last AP
        //TODO: this is very agressive -- what if the user wants to switch networks for some reacon -- drop it for now
        if (ENABLE_AGGRESSIVE_RECONNECT_WHEN_CONNECTION_DROPS) {
            ServiceLog.v("wifiNetworkDisconnectedUnexpectedly");
            if (!isActionPending()) {
                ServiceLog.v("connectToBestWifi");
                sendNotificationOfServiceActionStarted(context.getString(R.string.connect_to_best_wifi),
                        context.getString(R.string.wifi_disconnected_prematurely));
                serviceActionTaker.connectToBestWifi(listOfOfflineRouterIDs);
            }
        }
    }

    //private
    private void performConnectivityTest() {
        final FutureAction connectivityAction = serviceActionTaker.performConnectivityTest();
        connectivityAction.getFuture().addListener(new Runnable() {
            @Override
            public void run() {
                ActionResult result = null;
                try {
                    result = connectivityAction.getFuture().get();
                    ServiceLog.v("WifiServiceCore: Got the result for  " + connectivityAction.getName() + " result " + result.getStatusString());
                }catch (Exception e) {
                    e.printStackTrace(System.out);
                    ServiceLog.w("WifiServiceCore: Exception getting wifi results: " + e.toString());
                    //Informing inference engine of the error.
                } finally {
                    onConnectivityTestDone(result);
                }
            }
        }, serviceThreadPool.getExecutor());
    }


    private void onConnectivityTestDone(ActionResult connectivityResult) {
        @ConnectivityTester.WifiConnectivityMode int lastConnectivityMode = ConnectivityTester.WifiConnectivityMode.UNKNOWN;
        ServiceLog.v("WifiServiceCore: onConnectivityTestDone");
        if (didActionComplete(connectivityResult)) {
            if ((int)connectivityResult.getPayload() == ConnectivityTester.WifiConnectivityMode.CONNECTED_AND_ONLINE) {
                //Online
                ServiceLog.v("WifiServiceCore: onConnectivityTestDone -- CONNECTED_AND_ONLINE, setting numChecks to 0");
                numConsecutiveFailedConnectivityTests = 0;
                listOfOfflineRouterIDs.clear();
                lastConnectivityMode = CONNECTED_AND_ONLINE;
                updateWifiConnectivityMode(ConnectivityTester.WifiConnectivityMode.CONNECTED_AND_ONLINE);
                //Send message to user saying it passed
            } else if ((int)connectivityResult.getPayload() == ConnectivityTester.WifiConnectivityMode.CONNECTED_AND_CAPTIVE_PORTAL) {
                //Captive portal
                numConsecutiveFailedConnectivityTests++;
                lastConnectivityMode = ConnectivityTester.WifiConnectivityMode.CONNECTED_AND_CAPTIVE_PORTAL;
                ServiceLog.v("WifiServiceCore: onConnectivityTestDone -- CONNECTED_AND_CAPTIVE_PORTAL, incr numChecks");
            } else {
                //Offline
                lastConnectivityMode = ConnectivityTester.WifiConnectivityMode.CONNECTED_AND_OFFLINE;
                numConsecutiveFailedConnectivityTests++;
                ServiceLog.v("WifiServiceCore: onConnectivityTestDone -- OFFLINE,  numChecks: " + numConsecutiveFailedConnectivityTests);
            }
        }
        if (numConsecutiveFailedConnectivityTests >= MAX_CONNECTIVITY_TEST_FAILURES) {
            //trigger reconnect to best wifi
            ServiceLog.v("WifiServiceCore: onConnectivityTestDone numChecks > MAX checks: " +  numConsecutiveFailedConnectivityTests);
            listOfOfflineRouterIDs.add(wifiStateMonitor.primaryRouterID());
            numConsecutiveFailedConnectivityTests = 0;
            updateWifiConnectivityMode(lastConnectivityMode);
            if (!isActionPending()) {
                ServiceLog.v("WifiServiceCore: onConnectToBestWifi: list offline routers " + listOfOfflineRouterIDs.toString());
                sendNotificationOfServiceActionStarted("Connect to best Wifi", "Max connectivity tests failed");
                serviceActionTaker.connectToBestWifi(listOfOfflineRouterIDs);
            }
        } else if (numConsecutiveFailedConnectivityTests > 0) {
            //Reschedule connectivity test
            if (!isActionPending() && ConnectivityTester.isOnline(wifiConnectivityMode)) {
                ServiceLog.v("Initiating connectivity test");
                performConnectivityTest();
            }
        }
    }

    private void updateWifiConnectivityMode(@ConnectivityTester.WifiConnectivityMode int newMode) {
        ServiceLog.v("In updateWifiConnectivityMode with mode " + ConnectivityTester.connectivityModeToString(newMode));
        if (newMode != wifiConnectivityMode) {
            ServiceLog.v("Updating connectivity mode from : " +  ConnectivityTester.connectivityModeToString(wifiConnectivityMode) + " to " + ConnectivityTester.connectivityModeToString(newMode));
            wifiConnectivityMode = newMode;
            lastWifiEventTimestampMs = System.currentTimeMillis();
            lastWifiEventDescription = ConnectivityTester.connectivityModeDescription(wifiConnectivityMode);
            sendWifiStatusNotification();
        }
    }

    private void cancelChecksAndPendingActions() {
        numConsecutiveFailedConnectivityTests = 0;
        serviceActionTaker.cancelPendingActions();
        periodicCheckMonitor.disableCheck();
    }

    private void startWifiCheck() {
        ServiceLog.v("WifiServiceCore: InStartWifiCheck");
        if (ConnectivityTester.isConnected(wifiConnectivityMode)) {
            ServiceLog.v("WifiServiceCore: isConnected true, so starting check");
            checkFired();
            periodicCheckMonitor.enableCheck(wifiCheckInitialDelayMs, wifiCheckPeriodMs, this);
        }
    }


    //Notification functions
    private void sendWifiStatusNotification() {
        @ConnectivityTester.WifiConnectivityMode int modeToNotify = wifiConnectivityMode;
        ServiceLog.v("Sending notification of wifi status with mode " + ConnectivityTester.connectivityModeDescription(modeToNotify));
        String title = Utils.EMPTY_STRING;
        String primaryRouterSSID = Utils.limitSSID(wifiStateMonitor.primaryRouterSSID());
        String primaryRouterSignalQuality = wifiStateMonitor.primaryRouterSignalQuality();
        String body = Utils.EMPTY_STRING;
        if (lastActionTimestampMs > 0) {
            body = lastActionTakenDescription + " at " + Utils.convertMillisecondsToTimeForNotification(lastActionTimestampMs);
        } else if (lastWifiEventTimestampMs > 0 && !lastWifiEventDescription.equals(Utils.EMPTY_STRING)) {
            body = lastWifiEventDescription + " at " + Utils.convertMillisecondsToTimeForNotification(lastWifiEventTimestampMs);
        }
        switch (modeToNotify) {
            case CONNECTED_AND_ONLINE:
                title = primaryRouterSSID + "/ Online / " + primaryRouterSignalQuality;
                break;
            case CONNECTED_AND_CAPTIVE_PORTAL:
                title = primaryRouterSSID + "/ Sign In Required";
                break;
            case CONNECTED_AND_OFFLINE:
            case CONNECTED_AND_UNKNOWN:
                title = primaryRouterSSID + "/ No Internet";
                break;
            case ON_AND_DISCONNECTED:
                title = "WiFi Disconnected";
                break;
            case OFF:
                title = "WiFi Off";
                break;
            case UNKNOWN:
            default:
                break;
        }
        Utils.sendNotificationInfo(context, title, body, WifiMonitoringService.WIFI_STATUS_NOTIFICATION_ID);
    }

    private void sendNotificationOfUserActionNeeded(String actionNeeded, String reason) {
        Utils.sendNotificationInfo(context, actionNeeded, reason, WifiMonitoringService.WIFI_ISSUE_NOTIFICATION_ID);
    }

    private boolean didActionComplete(ActionResult actionResult) {
        return actionResult != null && actionResult.getStatus() == ActionResult.ActionResultCodes.SUCCESS;
    }

    private void sendNotificationOfServiceActionStarted(String actionName, String reason) {
        ServiceLog.v("Starting action: " + actionName + " because: " + reason);
        //Log this in a screen and show to user -- no op for now
    }

    private void markActionPending() {
        actionPendingTimestampMs = System.currentTimeMillis();
    }

    private void markActionDone() {
        actionPendingTimestampMs = 0;
    }

    private boolean isActionPending() {
        return (System.currentTimeMillis() - actionPendingTimestampMs < ACTION_PENDING_TIMEOUT);
    }



}
