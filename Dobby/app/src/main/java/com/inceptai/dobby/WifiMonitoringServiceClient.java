package com.inceptai.dobby;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.WifiInfo;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.dobby.analytics.DobbyAnalytics;
import com.inceptai.dobby.database.RepairDatabaseWriter;
import com.inceptai.dobby.database.RepairRecord;
import com.inceptai.dobby.notifications.DisplayAppNotification;
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
import static com.inceptai.wifimonitoringservice.WifiMonitoringService.NOTIFICATION_INFO_INTENT_VALUE;


/**
 * Created by vivek on 8/11/17.
 */
public class WifiMonitoringServiceClient {
    private WifiMonitoringService wifiMonitoringService;
    private WifiServiceConnection wifiServiceConnection;
    private NotificationInfoReceiver notificationInfoReceiver;
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
        notificationInfoReceiver = new NotificationInfoReceiver();
        wifiServiceConnection = new WifiServiceConnection();
        isNotificationReceiverRegistered = false;
        //TODO find a better place to connect
        startWifiMonitoringServiceIfEnabled();
        connect();
    }

    public void connect() {
        bindWithWifiService();
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
        if (repairFuture == null || repairFuture.isDone()) {
            //Start fresh repair
            dobbyAnalytics.setWifiRepairInitiated();
            if (boundToWifiService && wifiMonitoringService != null) {
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
        if (Utils.checkIsWifiMonitoringEnabled(context)) {
            registerNotificationInfoReceiver();
            initiateStatusNotification();
        }
    }

    public void pauseNotifications() {
        unRegisterNotificationInfoReceiver();
    }
    //Private stuff

    private void startWifiMonitoringServiceIfEnabled() {
        Intent serviceStartIntent = new Intent(context, WifiMonitoringService.class);
        PendingIntent pendingIntentToLaunchOnNotificationClick = Utils.getPendingIntentForNotification(context, null);
        serviceStartIntent.putExtra(WifiMonitoringService.EXTRA_PENDING_INTENT_NAME, pendingIntentToLaunchOnNotificationClick);
        if (Utils.checkIsWifiMonitoringEnabled(context)) {
            dobbyAnalytics.setWifiServiceStarted();
            //context.startService(new Intent(context, WifiMonitoringService.class));
            context.startService(serviceStartIntent);
        }
    }

    private void stopWifiMonitoringService() {
        //Intent serviceStartIntent = new Intent(this, WifiMonitoringService.class);
        //serviceStartIntent.putExtra(NotificationInfoKeys., NOTIFICATION_INFO_INTENT_VALUE);
        dobbyAnalytics.setWifiServiceStopped();
        context.stopService(new Intent(context, WifiMonitoringService.class));
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

    private void processRepairResult(IterateAndRepairWifiNetwork.RepairResult repairResult) {
        int textId = R.string.repair_wifi_failure;
        WifiInfo repairedWifiInfo = null;
        boolean repairSuccessful = false;
        boolean toggleSuccessful = true;
        String repairSummary = Utils.EMPTY_STRING;
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


    //Notification stuff
    // Our handler for received Intents. This will be called whenever an Intent
    // with an action named "custom-event-name" is broadcasted.
    private class NotificationInfoReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String notificationTitle = intent.getStringExtra(WifiMonitoringService.EXTRA_NOTIFICATION_TITLE);
            String notificationBody = intent.getStringExtra(WifiMonitoringService.EXTRA_NOTIFICATION_BODY);
            int notificationId = intent.getIntExtra(WifiMonitoringService.EXTRA_NOTIFICATION_ID, 0);
            dobbyAnalytics.setWifiServiceNotificationShown();
            executor.execute(new DisplayAppNotification(context, notificationTitle, notificationBody, notificationId));
        }
    }

    private void registerNotificationInfoReceiver() {
        IntentFilter intentFilter = new IntentFilter(NOTIFICATION_INFO_INTENT_VALUE);
        LocalBroadcastManager.getInstance(context).registerReceiver(
                notificationInfoReceiver, intentFilter);
        isNotificationReceiverRegistered = true;
    }

    private void unRegisterNotificationInfoReceiver() {
        if (isNotificationReceiverRegistered) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(
                    notificationInfoReceiver);
            isNotificationReceiverRegistered = false;
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
