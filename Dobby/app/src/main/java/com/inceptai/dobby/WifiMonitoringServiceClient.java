package com.inceptai.dobby;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.WifiInfo;
import android.os.IBinder;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.dobby.analytics.DobbyAnalytics;
import com.inceptai.dobby.database.RepairDatabaseWriter;
import com.inceptai.dobby.database.RepairRecord;
import com.inceptai.dobby.utils.DobbyLog;
import com.inceptai.dobby.utils.Utils;
import com.inceptai.wifimonitoringservice.ActionRequest;
import com.inceptai.wifimonitoringservice.WifiMonitoringService;
import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.Action;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.IterateAndRepairWifiNetwork;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import javax.inject.Inject;

import static com.inceptai.dobby.ui.WifiDocActivity.REPAIR_TIMEOUT_MS;


/**
 * Created by vivek on 8/11/17.
 */
public class WifiMonitoringServiceClient {
    private WifiMonitoringService wifiMonitoringService;
    private WifiServiceConnection wifiServiceConnection;
    private Context context;
    private boolean boundToWifiService = false;
    private ListenableFuture<ActionResult> repairFuture;
    private boolean isNotificationReceiverRegistered;
    private String userId;
    private String phoneInfo;
    private Executor executor;
    private WifiMonitoringCallback wifiMonitoringCallback;

    @Inject
    RepairDatabaseWriter repairDatabaseWriter;
    @Inject
    DobbyAnalytics dobbyAnalytics;

    public interface WifiMonitoringCallback {
        void wifiMonitoringStarted();
        void wifiMonitoringStopped();
        void repairStarted(boolean started);
        void repairFinished(boolean success, WifiInfo repairedWifiInfo, String repairSummary);
    }

    public WifiMonitoringServiceClient(Context context, String userId,
                                       String phoneInfo, Executor executor,
                                       WifiMonitoringCallback wifiMonitoringCallback) {
        ((DobbyApplication) context.getApplicationContext()).getProdComponent().inject(this);
        this.context = context;
        this.userId = userId;
        this.phoneInfo = phoneInfo;
        this.executor = executor;
        this.wifiMonitoringCallback = wifiMonitoringCallback;
        wifiServiceConnection = new WifiServiceConnection();
        isNotificationReceiverRegistered = false;
        //TODO find a better place to connect
        startWifiMonitoringServiceIfEnabled();
        connect();
    }


    public void disconnect() {
        unbindWithWifiService();
    }

    public void cleanup() {
        disconnect();
        this.wifiMonitoringCallback = null;
        executor = null;
    }

    public void enableWifiService() {
        //Save wifi service started and start service
        Utils.saveWifiMonitoringEnabled(context);
        startWifiMonitoringServiceIfEnabled();
    }

    public void disableWifiService() {
        Utils.saveWifiMonitoringDisabled(context);
        stopWifiMonitoringService();
    }

    public void repairWifiNetwork(long timeOutMs) {
        repairWifiNetwork(timeOutMs, true);
    }

    public void repairWifiNetwork(long timeOutMs, boolean isLocationPermissionGranted) {
        if (repairFuture == null || repairFuture.isDone()) {
            //Start fresh repair
            dobbyAnalytics.setWifiRepairInitiated();
            if (!isLocationPermissionGranted) {
                //We can't scan hence can't connect to a network
                dobbyAnalytics.setWifiServiceUnavailableForRepair();
                processRepairResult(null, "Sorry but we are unable to perform repair without location permission -- " +
                        "we need it to scan for nearby WiFi networks that your phone can connect to. " +
                        "Please give location permission to this app and retry.");
            } else if (boundToWifiService && wifiMonitoringService != null) {
                //Listening for repair to finish on a different thread
                //Start animation for repair button
                repairFuture = wifiMonitoringService.repairWifiNetwork(REPAIR_TIMEOUT_MS);
                if (repairFuture != null) {
                    waitForRepair();
                } else {
                    processRepairResult(null);
                }
                if (wifiMonitoringCallback != null) {
                    wifiMonitoringCallback.repairStarted(true);
                }
            } else {
                //Change text for repair button
                //Notify error to WifiDocMainFragment
                DobbyLog.e("Repair failed -- Service unavailable -- boundToService " + (boundToWifiService ? Utils.TRUE_STRING : Utils.FALSE_STRING));
                if (wifiMonitoringCallback != null) {
                    wifiMonitoringCallback.repairStarted(false);
                }
                dobbyAnalytics.setWifiServiceUnavailableForRepair();
                processRepairResult(null);
            }
        }
    }

    public void cancelWifiRepair() {
        repairFuture.cancel(true);
        if (boundToWifiService && wifiMonitoringService != null) {
            wifiMonitoringService.cancelRepairOfWifiNetwork();
        }
    }


    public Action takeAction(ActionRequest actionRequest) {
        if (boundToWifiService && wifiMonitoringService != null) {
            return wifiMonitoringService.takeAction(actionRequest);
        }
        return null;
    }

    public void cancelAction(Action action) {
        action.cancelAction();
    }



    public void resumeNotificationIfNeeded() {
        if (boundToWifiService && wifiMonitoringService != null) {
            wifiMonitoringService.resumeNotifications();
        }
    }

    public void pauseNotifications() {
        if (boundToWifiService && wifiMonitoringService != null) {
            wifiMonitoringService.pauseNotifications();
        }
    }

    //Private stuff

    private void connect() {
        bindWithWifiService();
    }

    private void startWifiMonitoringServiceIfEnabled() {
        Intent serviceStartIntent = new Intent(context, WifiMonitoringService.class);
        PendingIntent pendingIntentToLaunchOnNotificationClick = Utils.getPendingIntentForNotification(context, null);
        serviceStartIntent.putExtra(WifiMonitoringService.EXTRA_PENDING_INTENT_NAME, pendingIntentToLaunchOnNotificationClick);
        if (Utils.checkIsWifiMonitoringEnabled(context)) {
            dobbyAnalytics.setWifiServiceStarted();
            //context.startService(new Intent(context, WifiMonitoringService.class));
            context.startService(serviceStartIntent);
            if (wifiMonitoringCallback != null) {
                wifiMonitoringCallback.wifiMonitoringStarted();
            }
            if (boundToWifiService && wifiMonitoringService != null) {
                wifiMonitoringService.resumeNotifications();
            }
        }
    }

    private void stopWifiMonitoringService() {
        dobbyAnalytics.setWifiServiceStopped();
        if (boundToWifiService && wifiMonitoringService != null) {
            wifiMonitoringService.cancelNotifications();
        }
        context.stopService(new Intent(context, WifiMonitoringService.class));
        if (wifiMonitoringCallback != null) {
            wifiMonitoringCallback.wifiMonitoringStopped();
        }
    }

    private void bindWithWifiService() {
        // Bind to LocalService
        Intent intent = new Intent(context, WifiMonitoringService.class);
        try {
            if (context.bindService(intent, wifiServiceConnection, Context.BIND_AUTO_CREATE)) {
                DobbyLog.v("bindService to  wifiServiceConnection succeeded");
                dobbyAnalytics.setWifiServiceBindingSuccessful();
            } else {
                DobbyLog.v("bindService to wifiServiceConnection failed");
                dobbyAnalytics.setWifiServiceBindingFailed();
            }
        } catch (SecurityException e) {
            dobbyAnalytics.setWifiServiceBindingSecurityException();
            DobbyLog.v("App does not have permission to bind to WifiMonitoring service");
        }
    }

    private void unbindWithWifiService() {
        // Unbind from the service
        if (boundToWifiService) {
            DobbyLog.v("Unbinding Service");
            context.unbindService(wifiServiceConnection);
            boundToWifiService = false;
            wifiMonitoringService = null;
            dobbyAnalytics.setWifiServiceUnbindingCalled();
        }
    }



    private void waitForRepair() {
        repairFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    processRepairResult((IterateAndRepairWifiNetwork.RepairResult)repairFuture.get());
                } catch (InterruptedException | ExecutionException | CancellationException e) {
                    //Notify error to WifiDocMainFragment
                    DobbyLog.e("Interrupted  while repairing");
                }
            }
        }, executor);
    }

    private void processRepairResult(IterateAndRepairWifiNetwork.RepairResult repairResult, String repairSummary) {
        int textId = R.string.repair_wifi_failure;
        WifiInfo repairedWifiInfo = null;
        boolean repairSuccessful = false;
        boolean toggleSuccessful = true;
        dobbyAnalytics.setWifiRepairFinished();
        if (ActionResult.isSuccessful(repairResult)) {
            textId = R.string.repair_wifi_success;
            repairSuccessful = true;
            repairedWifiInfo = (WifiInfo) repairResult.getPayload();
            dobbyAnalytics.setWifiRepairSuccessful();
        } else if (ActionResult.failedToComplete(repairResult)){
            repairedWifiInfo = (WifiInfo) repairResult.getPayload();
            if (repairedWifiInfo == null) {
                toggleSuccessful = false;
            }
            dobbyAnalytics.setWifiRepairFailed(repairResult.getStatus());
        } else if (repairResult != null){
            dobbyAnalytics.setWifiRepairFailed(repairResult.getStatus());
        } else {
            dobbyAnalytics.setWifiRepairFailed(ActionResult.ActionResultCodes.UNKNOWN);
        }

        if (repairResult != null) {
            repairSummary = repairResult.getStatusString();
        }

        if (wifiMonitoringCallback != null) {
            wifiMonitoringCallback.repairFinished(repairSuccessful, repairedWifiInfo, repairSummary);
        }
        writeRepairRecord(createRepairRecord(repairResult));
        addWifiFailureReasonToAnalytics(repairResult);
    }


    private void processRepairResult(IterateAndRepairWifiNetwork.RepairResult repairResult) {
        processRepairResult(repairResult, Utils.EMPTY_STRING);
    }

    private void addWifiFailureReasonToAnalytics(IterateAndRepairWifiNetwork.RepairResult repairResult) {
        if (repairResult == null) {
            dobbyAnalytics.setWifiRepairUnknownFailure();
            return;
        }
        switch (repairResult.getRepairFailureReason()) {
            case IterateAndRepairWifiNetwork.RepairResult.NO_NEARBY_CONFIGURED_NETWORKS:
                dobbyAnalytics.setWifiRepairNoNearbyConfiguredNetworks();
                break;
            case IterateAndRepairWifiNetwork.RepairResult.NO_NETWORK_WITH_ONLINE_CONNECTIVITY_MODE:
                dobbyAnalytics.setWifiRepairNoNetworkWithOnlineConnectivityMode();
                break;
            case IterateAndRepairWifiNetwork.RepairResult.UNABLE_TO_CONNECT_TO_ANY_NETWORK:
                dobbyAnalytics.setWifiRepairUnableToConnectToAnyNetwork();
                break;
            case IterateAndRepairWifiNetwork.RepairResult.UNABLE_TO_TOGGLE_WIFI:
                dobbyAnalytics.setWifiRepairUnableToToggleWifi();
                break;
            case IterateAndRepairWifiNetwork.RepairResult.TIMED_OUT:
                dobbyAnalytics.setWifiRepairTimedOut();
                break;
            case IterateAndRepairWifiNetwork.RepairResult.UNKNOWN:
            default:
                dobbyAnalytics.setWifiRepairUnknownFailure();
                break;
        }
    }

    private RepairRecord createRepairRecord(IterateAndRepairWifiNetwork.RepairResult repairResult) {
        RepairRecord repairRecord = new RepairRecord();
        repairRecord.uid = userId;
        repairRecord.phoneInfo = phoneInfo;
        repairRecord.appVersion = DobbyApplication.getAppVersion();
        if (repairResult != null) {
            repairRecord.repairStatusString = ActionResult.actionResultCodeToString(repairResult.getStatus());
            repairRecord.repairStatusMessage = repairResult.getStatusString();
            repairRecord.failureReason = repairResult.getRepairFailureReason();
        } else {
            repairRecord.repairStatusString = ActionResult.actionResultCodeToString(ActionResult.ActionResultCodes.UNKNOWN);
            repairRecord.repairStatusString = Utils.EMPTY_STRING;
            repairRecord.failureReason = Utils.EMPTY_STRING;
        }
        repairRecord.timestamp = System.currentTimeMillis();
        return repairRecord;
    }


    private void writeRepairRecord(final RepairRecord repairRecord) {
        repairDatabaseWriter.writeRepairToDatabase(repairRecord);
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private class WifiServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            WifiMonitoringService.WifiServiceBinder binder = (WifiMonitoringService.WifiServiceBinder) service;
            DobbyLog.v("In onServiceConnected for bind service");
            wifiMonitoringService = binder.getService();
            boundToWifiService = true;
            dobbyAnalytics.setWifiServiceConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            DobbyLog.v("WifiDocActivity: onServiceDisconnected");
            boundToWifiService = false;
            dobbyAnalytics.setWifiServiceDisconnected();
        }
    }

    private void initiateStatusNotification() {
        if (Utils.checkIsWifiMonitoringEnabled(context) && boundToWifiService && wifiMonitoringService != null) {
            wifiMonitoringService.sendStatusUpdateNotification();
        } else {
            //Change text for repair button
            //Notify error to WifiDocMainFragment
            DobbyLog.e("Sending status request failed -- Service unavailable  boundToService " + (boundToWifiService ? Utils.TRUE_STRING : Utils.FALSE_STRING));
        }

    }


}
