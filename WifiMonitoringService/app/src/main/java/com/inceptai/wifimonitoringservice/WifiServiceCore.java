package com.inceptai.wifimonitoringservice;

import android.content.Context;
import android.support.annotation.Nullable;

import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.ConnectivityTester;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.FutureAction;
import com.inceptai.wifimonitoringservice.monitors.PeriodicCheckMonitor;
import com.inceptai.wifimonitoringservice.monitors.ScreenStateMonitor;
import com.inceptai.wifimonitoringservice.monitors.WifiStateMonitor;
import com.inceptai.wifimonitoringservice.utils.ServiceLog;
import com.inceptai.wifimonitoringservice.utils.Utils;

/**
 * Created by vivek on 7/10/17.
 */

public class WifiServiceCore implements
        WifiStateMonitor.WifiStateCallback,
        ScreenStateMonitor.ScreenStateCallback,
        PeriodicCheckMonitor.PeriodicCheckCallback,
        ServiceActionTaker.ActionCallback {
    private final static int MAX_CONNECTIVITY_TEST_FAILURES = 3;
    private final static int WIFI_CHECK_INITIAL_CHECK_DELAY_ACTIVE_MS = 5 * 1000; // 5 sec
    private final static int WIFI_CHECK_INITIAL_CHECK_DELAY_INACTIVE_MS = 60 * 1000; // 60 sec
    private final static int WIFI_CHECK_PERIOD_SCREEN_ACTIVE_MS = 30 * 1000; //30 secs
    private final static int WIFI_CHECK_PERIOD_SCREEN_INACTIVE_MS = 5 * 60 * 1000; //5 mins
    private final static boolean ENABLE_CHECK_WHEN_SCREEN_INACTIVE = false;


    //Key components
    private WifiStateMonitor wifiStateMonitor;
    private ScreenStateMonitor screenStateMonitor;
    private ServiceActionTaker serviceActionTaker;
    private PeriodicCheckMonitor periodicCheckMonitor;
    private ServiceThreadPool serviceThreadPool;
    private Context context;
    //Key state
    private int numConsecutiveFailedConnectivityTests;
    private static WifiServiceCore WIFI_SERVICE_CORE;

    private boolean actionPending;
    private boolean isConnected;
    private boolean isEnabled;
    private long wifiCheckInitialDelayMs;
    private long wifiCheckPeriodMs;


    //TODO: Check if smart switch is incorrectly disabling wifi configuration


    private WifiServiceCore(Context context, ServiceThreadPool serviceThreadPool) {
        this.context = context;
        this.serviceThreadPool = serviceThreadPool;
        wifiStateMonitor = new WifiStateMonitor(context);
        screenStateMonitor = new ScreenStateMonitor(context);
        periodicCheckMonitor = new PeriodicCheckMonitor(context);
        serviceActionTaker = new ServiceActionTaker(context, serviceThreadPool.getExecutor(), serviceThreadPool.getScheduledExecutorServiceForActions());
        numConsecutiveFailedConnectivityTests = 0;
        actionPending = false;
        isConnected = false;
        isEnabled = false;
        wifiCheckInitialDelayMs = WIFI_CHECK_INITIAL_CHECK_DELAY_ACTIVE_MS;
        wifiCheckPeriodMs = WIFI_CHECK_PERIOD_SCREEN_ACTIVE_MS;
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

    public void startMonitoring() {
        wifiStateMonitor.registerCallback(this);
        screenStateMonitor.registerCallback(this);
        serviceActionTaker.registerCallback(this);
        ServiceLog.v("StartMonitoring ");
    }

    public void cleanup() {
        wifiStateMonitor.unregisterCallback();
        screenStateMonitor.unregisterCallback();
        periodicCheckMonitor.disableCheck();
        serviceActionTaker.unregisterCallback();
        serviceThreadPool.shutdown();
        ServiceLog.v("Cleanup ");
    }

    //Overrides for periodic check


    @Override
    public void actionStarted(String actionName) {
        ServiceLog.v("Action started  " + actionName);
        actionPending = true;
    }

    @Override
    public void actionCompleted(String actionName, ActionResult actionResult) {
        ServiceLog.v("Action finished  " + actionName);
        if (actionResult == null) {
            ServiceLog.v("Action result: null");
        } else {
            ServiceLog.v("Action result: " + actionResult.getStatusString());
        }
        actionPending = false;
    }

    @Override
    public void checkFired() {
        //Perform wifi connectivity test
        ServiceLog.v("Check fired");
        if (!actionPending && isConnected) {
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
        isEnabled = true;
    }

    @Override
    public void wifiStateDisabled() {
        //Stop the wifi check alarm
        ServiceLog.v("Wifi disabled");
        cancelChecksAndPendingActions();
        isEnabled = false;
    }

    @Override
    public void wifiStateDisconnected() {
        //Start the wifi check alarm -- first one fires after 10 secs and then every 5 mins
//        periodicCheckMonitor.enableCheck(WIFI_CHECK_INITIAL_CHECK_DELAY_ACTIVE_MS,
//                WIFI_CHECK_PERIOD_SCREEN_ACTIVE_MS, this);
        ServiceLog.v("Wifi state disconnected");
        isConnected = false;
    }

    @Override
    public void wifiStateConnected() {
        //Stop the wifi check alarm -- for now -- we will get callbacks for low snr etc. --
        // what if wifi loses connectivity in between -- we will get callback from ConnectivityManager.
        ServiceLog.v("Wifi state connected");
        isConnected = true;
        startWifiCheck();
    }

    //Problems
    @Override
    public void wifiPrimaryAPSignalLow() {
        //scan and reconnect to other stronger AP if available.
        ServiceLog.v("wifi primary AP signal low");
        if (!actionPending) {
            ServiceLog.v("ConnectToBestWifi initiated");
            serviceActionTaker.connectToBestWifi();
        }
    }

    @Override
    public void wifiStateHangingOnScanning() {
        //Router not visible -- toggle wifi and re-associate.
        ServiceLog.v("wifiStateHangingOnScanning");
        if (!actionPending) {
            ServiceLog.v("RepairConnection initiated");
            serviceActionTaker.repairConnection();
        }
    }

    @Override
    public void wifiStateHangingOnObtainingIPAddress() {
        //Disconnect and then re-associate with same router
        //TODO: Should we reset or repair ??
        ServiceLog.v("wifiStateHangingOnObtainingIPAddress");
        if (!actionPending) {
            ServiceLog.v("RepairConnection initiated");
            serviceActionTaker.resetConnection();
        }
    }

    @Override
    public void wifiStateHangingOnAuthenticating() {
        //Disconnect and  try to reconnect
        //TODO: Should we reset or repair ??
        ServiceLog.v("wifiStateHangingOnAuthenticating");
        if (!actionPending) {
            ServiceLog.v("ResetConnection initiated");
            serviceActionTaker.resetConnection();
        }
    }


    @Override
    public void wifiStateHangingOnConnecting() {
        ServiceLog.v("wifiStateHangingOnConnecting");
        if (!actionPending) {
            ServiceLog.v("ResetConnection initiated");
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
        }
        if (!actionPending) {
            ServiceLog.v("ConnectToBestWifi");
            serviceActionTaker.connectToBestWifi();
        }
    }

    @Override
    public void wifiStateErrorAuthenticating() {
        //Tell user that passphrase is wrong
        //TODO: Show a message to the user to check the passphrase for the network --
        //TODO: maybe try connecting to another network ?
        ServiceLog.v("wifiStateErrorAuthenticating");
        if (!actionPending) {
            ServiceLog.v("ConnectToBestWifi");
            serviceActionTaker.connectToBestWifi();
        }
    }

    @Override
    public void wifiStateProblematicSupplicantPattern() {
        //Toggle wifi
        ServiceLog.v("wifiStateProblematicSupplicantPattern");
        if (!actionPending) {
            ServiceLog.v("repairConnection");
            serviceActionTaker.repairConnection();
        }
    }

    @Override
    public void wifiNetworkAcquiredDataConnectivity() {
        //Perform connectivity test
        ServiceLog.v("wifiNetworkAcquiredDataConnectivity: disabling checks");
        numConsecutiveFailedConnectivityTests = 0;
        periodicCheckMonitor.disableCheck();
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
        if (!actionPending) {
            ServiceLog.v("toggleWifi");
            serviceActionTaker.toggleWifi();
        }
    }

    @Override
    public void wifiNetworkDisconnectedUnexpectedly() {
        //Reconnect with best AP or last AP
        ServiceLog.v("wifiNetworkDisconnectedUnexpectedly");
        if (!actionPending) {
            ServiceLog.v("connectToBestWifi");
            serviceActionTaker.connectToBestWifi();
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

    private boolean didActionComplete(ActionResult actionResult) {
        return actionResult != null && actionResult.getStatus() == ActionResult.ActionResultCodes.SUCCESS;
    }

    private void onConnectivityTestDone(ActionResult connectivityResult) {
        ServiceLog.v("WifiServiceCore: onConnectivityTestDone");
        if (didActionComplete(connectivityResult)) {
            if ((int)connectivityResult.getPayload() == ConnectivityTester.WifiConnectivityMode.CONNECTED_AND_ONLINE) {
                //Online
                numConsecutiveFailedConnectivityTests = 0;
                ServiceLog.v("WifiServiceCore: onConnectivityTestDone -- CONNECTED_AND_ONLINE, setting numChecks to 0");
            } else if ((int)connectivityResult.getPayload() == ConnectivityTester.WifiConnectivityMode.CONNECTED_AND_CAPTIVE_PORTAL) {
                //Captive portal
                numConsecutiveFailedConnectivityTests++;
                ServiceLog.v("WifiServiceCore: onConnectivityTestDone -- CONNECTED_AND_CAPTIVE_PORTAL, incr numChecks");
            } else {
                //Offline
                ServiceLog.v("WifiServiceCore: onConnectivityTestDone -- OFFLINE, incr numChecks");
                numConsecutiveFailedConnectivityTests++;
            }
        }
        if (numConsecutiveFailedConnectivityTests > MAX_CONNECTIVITY_TEST_FAILURES) {
            //trigger reconnect to best wifi
            ServiceLog.v("WifiServiceCore: onConnectivityTestDone numChecks > MAX checks: " +  numConsecutiveFailedConnectivityTests);
            if (!actionPending) {
                ServiceLog.v("WifiServiceCore: onConnectToBestWifi");
                serviceActionTaker.connectToBestWifi();
            }
            numConsecutiveFailedConnectivityTests = 0;
        }
    }

    private void cancelChecksAndPendingActions() {
        numConsecutiveFailedConnectivityTests = 0;
        serviceActionTaker.cancelPendingActions();
        periodicCheckMonitor.disableCheck();
    }

    private void startWifiCheck() {
        ServiceLog.v("WifiServiceCore: InStartWifiCheck");
        if (isEnabled && isConnected) {
            ServiceLog.v("WifiServiceCore: isConnected true, so starting check");
            periodicCheckMonitor.enableCheck(wifiCheckInitialDelayMs, wifiCheckPeriodMs, this);
        }
    }
}
