package com.inceptai.dobby.ui;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.NetworkLayer;
import com.inceptai.dobby.R;
import com.inceptai.dobby.ai.Action;
import com.inceptai.dobby.ai.DobbyAi;
import com.inceptai.dobby.ai.suggest.LocalSummary;
import com.inceptai.dobby.analytics.DobbyAnalytics;
import com.inceptai.dobby.database.RepairDatabaseWriter;
import com.inceptai.dobby.database.RepairRecord;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.fake.FakeDataIntentReceiver;
import com.inceptai.dobby.notifications.DisplayAppNotification;
import com.inceptai.dobby.utils.DobbyLog;
import com.inceptai.dobby.utils.Utils;
import com.inceptai.wifimonitoringservice.WifiMonitoringService;
import com.inceptai.wifimonitoringservice.actionlibrary.ActionResult;
import com.inceptai.wifimonitoringservice.actionlibrary.actions.IterateAndRepairWifiNetwork;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import static com.inceptai.dobby.DobbyApplication.TAG;

public class WifiDocActivity extends AppCompatActivity implements WifiDocMainFragment.OnFragmentInteractionListener, Handler.Callback {
    public static final String PREF_FIRST_TIME_USER = "WifiTesterNewbie";
    public static final String PREF_FIRST_TOGGLE = "first_time_automatic_repair_on";
    public static final long REPAIR_TIMEOUT_MS = 30000; //10 secs for testing


    private static final int MSG_SHOW_SUGGESTIONS_UI = 1001;

    private WifiDocMainFragment mainFragment;
    private SuggestionsFragment suggestionsFragment;
    @Inject
    DobbyApplication dobbyApplication;
    @Inject
    DobbyThreadpool threadpool;
    @Inject
    DobbyAi dobbyAi;
    @Inject
    NetworkLayer networkLayer;
    @Inject
    DobbyEventBus eventBus;
    @Inject
    DobbyAnalytics dobbyAnalytics;
    @Inject
    RepairDatabaseWriter repairDatabaseWriter;

    private FakeDataIntentReceiver fakeDataIntentReceiver;
    private Handler handler;
    private NotificationInfoReceiver notificationInfoReceiver;
    private WifiServiceConnection wifiServiceConnection;
    private boolean boundToWifiService = false;
    private WifiMonitoringService wifiMonitoringService;
    private ListenableFuture<ActionResult> repairFuture;
    private boolean isNotificationReceiverRegistered;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((DobbyApplication) getApplication()).getProdComponent().inject(this);
        super.onCreate(savedInstanceState);
        /**
         * Look at this: https://stackoverflow.com/questions/19545889/app-restarts-rather-than-resumes
         * Ensure the application resumes whatever task the user was performing the last time
         * they opened the app from the launcher. It would be preferable to configure this
         * behavior in  AndroidMananifest.xml activity settings, but those settings cause drastic
         * undesirable changes to the way the app opens: singleTask closes ALL other activities
         * in the task every time and alwaysRetainTaskState doesn't cover this case, incredibly.
         *
         * The problem happens when the user first installs and opens the app from
         * the play store or sideloaded apk (not via ADB). On this first run, if the user opens
         * activity B from activity A, presses 'home' and then navigates back to the app via the
         * launcher, they'd expect to see activity B. Instead they're shown activity A.
         *
         * The best solution is to close this activity if it isn't the task root.
         *
         */

        if (!isTaskRoot()) {
            finish();
            DobbyLog.v("Finishing since this is not root task");
            return;
        }

        handler = new Handler();
        setContentView(R.layout.activity_wifi_doc);
        setupMainFragment();
        networkLayer.fetchLastKnownLocation();
        startWifiMonitoringService();
        notificationInfoReceiver = new NotificationInfoReceiver();
        wifiServiceConnection = new WifiServiceConnection();
        bindWithWifiService();
        isNotificationReceiverRegistered = false;
    }

    public void setupMainFragment() {
        DobbyLog.v("WifiDocActivity setupMainFragment");
        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment existingFragment = fragmentManager.findFragmentByTag(WifiDocMainFragment.WIFI_DOC_MAIN_FRAGMENT);
        if (existingFragment == null) {
            try {
                DobbyLog.v("WifiDocActivity setupMainFragment existing fragment is there");
                existingFragment = (Fragment) WifiDocMainFragment.newInstance(Utils.EMPTY_STRING);
            } catch (Exception e) {
                DobbyLog.e("Unable to create WifiDocMainFragment");
            }
        }
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.wifi_doc_placeholder_fl, existingFragment, WifiDocMainFragment.WIFI_DOC_MAIN_FRAGMENT);
        // fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
        mainFragment = (WifiDocMainFragment) existingFragment;
        DobbyLog.v("WifiDocActivity setupMainFragment returning existing fragment");
    }

    public void setupSuggestionsFragment(LocalSummary localSummary) {
        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        suggestionsFragment = (SuggestionsFragment) fragmentManager.findFragmentByTag(SuggestionsFragment.TAG);
        if (suggestionsFragment == null) {
            try {
                suggestionsFragment = SuggestionsFragment.newInstance(Utils.EMPTY_STRING);
                suggestionsFragment.setSuggestions(localSummary);
            } catch (Exception e) {
                DobbyLog.e("Unable to create SuggestionsFragment");
            }
        }
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.wifi_doc_placeholder_fl, suggestionsFragment, TAG);
        // We add the suggestions fragment to the back stack so that the user can go back to
        // the main fragment.
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }



    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    protected void onResume() {
        DobbyLog.i("onResume()");
        super.onResume();
        if (fakeDataIntentReceiver == null) {
            fakeDataIntentReceiver = new FakeDataIntentReceiver(this);
        }
        Intent intent = registerReceiver(fakeDataIntentReceiver, new IntentFilter(FakeDataIntentReceiver.FAKE_DATA_INTENT));
        if (intent != null) {
            DobbyLog.e("intent: " + intent.getComponent());
        }
        unRegisterNotificationInfoReceiver();
    }

    @Override
    public void onBackPressed(){
        moveTaskToBack(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fakeDataIntentReceiver != null) {
            try {
                unregisterReceiver(fakeDataIntentReceiver);
            } catch (IllegalArgumentException e) {
                DobbyLog.i("Ignoring IllegalArgumentException for fake intent unregister.");
            }
        }
        if (Utils.checkIsWifiMonitoringEnabled(this)) {
            registerNotificationInfoReceiver();
            initiateStatusNotification();
        }
    }

    @Override
    public void onMainButtonClick() {
        threadpool.submit(new Runnable() {
            @Override
            public void run() {
                DobbyLog.i("onMainButtonClick.");
                dobbyAi.takeAction(new Action(Utils.EMPTY_STRING,
                        Action.ActionType.ACTION_TYPE_DIAGNOSE_SLOW_INTERNET));
            }
        });
    }

    @Override
    public void onLocationPermissionGranted() {
        //Trigger a wifiScan when permission is granted
        networkLayer.wifiScan();
    }

    public DobbyEventBus getEventBus() {
        return eventBus;
    }


    @Override
    protected void onStart() {
        super.onStart();
    }


    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        unbindWithWifiService();
        super.onDestroy();
    }

    @Override
    public void cancelTests() {
        DobbyLog.v("WifiDocActivity start with bw cancellation");
        networkLayer.cancelBandwidthTests();
        DobbyLog.v("WifiDocActivity end with bw cancellation");
    }

    public void showFakeSuggestionsUi(LocalSummary localSummary) {
        setupSuggestionsFragment(localSummary);
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case MSG_SHOW_SUGGESTIONS_UI:
                setupSuggestionsFragment((LocalSummary) msg.obj);
                break;
            default:
                return false;
        }
        return true;
    }

    @Override
    public void onWifiRepairInitiated() {
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
            } else {
                //Change text for repair button
                //Notify error to WifiDocMainFragment
                DobbyLog.e("Repair failed -- Service unavailable -- boundToService " + (boundToWifiService ? Utils.TRUE_STRING : Utils.FALSE_STRING));
                dobbyAnalytics.setWifiServiceUnavailableForRepair();
                processRepairResult(null);
            }
        }
    }

    @Override
    public void onWifiMonitoringServiceDisabled() {
        Utils.saveWifiMonitoringDisabled(this);
        stopWifiMonitoringService();
    }

    @Override
    public void onWifiMonitoringServiceEnabled() {
        Utils.saveWifiMonitoringEnabled(this);
        startWifiMonitoringService();
    }

    @Override
    public void onWifiRepairCancelled() {
        DobbyLog.v("WifiDocMainActivity: onWifiRepairCancelled");
        if (repairFuture != null && !repairFuture.isDone()) {
            dobbyAnalytics.setWifiRepairCancelled();
            DobbyLog.v("WifiDocMainActivity: Calling cancel on repair future");
            repairFuture.cancel(true);
            if (boundToWifiService && wifiMonitoringService != null) {
                wifiMonitoringService.cancelRepairOfWifiNetwork();
            } else {
                DobbyLog.e("Cancel Repair failed as service is unavailable");
            }
        }
    }

    private WifiDocMainFragment getMainFragmentFromTag() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager != null) {
            Fragment existingFragment = fragmentManager.findFragmentByTag(WifiDocMainFragment.WIFI_DOC_MAIN_FRAGMENT);
            if (existingFragment != null) {
                DobbyLog.v("In WifiDocActivity: getMainFragmentFromTag returning NON NULL fragment");
                return (WifiDocMainFragment) existingFragment;
            }
        }
        return null;
    }


    @Override
    public void onFragmentReady() {
        DobbyLog.v("In WifiDocActivity: onFragmentReady");
        mainFragment = getMainFragmentFromTag();
    }

    @Override
    public void onFragmentGone() {
        DobbyLog.v("In WifiDocActivity: onFragmentGone");
        mainFragment = null;
    }

    //Private stuff
    private void startRepair() {

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
        }, threadpool.getExecutor());
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

        if (mainFragment != null) {
            mainFragment.handleRepairFinished(repairedWifiInfo, textId, repairSummary);
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
        repairRecord.uid = dobbyApplication.getUserUuid();
        repairRecord.phoneInfo = dobbyApplication.getPhoneInfo();
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

    private void startWifiMonitoringService() {
        //Intent serviceStartIntent = new Intent(this, WifiMonitoringService.class);
        //serviceStartIntent.putExtra(NotificationInfoKeys., NOTIFICATION_INFO_INTENT_VALUE);
        if (Utils.checkIsWifiMonitoringEnabled(this)) {
            dobbyAnalytics.setWifiServiceStarted();
            startService(new Intent(this, WifiMonitoringService.class));
        }
    }

    private void stopWifiMonitoringService() {
        //Intent serviceStartIntent = new Intent(this, WifiMonitoringService.class);
        //serviceStartIntent.putExtra(NotificationInfoKeys., NOTIFICATION_INFO_INTENT_VALUE);
        dobbyAnalytics.setWifiServiceStopped();
        stopService(new Intent(this, WifiMonitoringService.class));
    }

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
            threadpool.getExecutor().execute(new DisplayAppNotification(context, notificationTitle, notificationBody, notificationId));
        }
    }

    private void registerNotificationInfoReceiver() {
            IntentFilter intentFilter = new IntentFilter(WifiMonitoringService.NOTIFICATION_INFO_INTENT_VALUE);
            LocalBroadcastManager.getInstance(this).registerReceiver(
                    notificationInfoReceiver, intentFilter);
            isNotificationReceiverRegistered = true;
    }

    private void unRegisterNotificationInfoReceiver() {
        if (isNotificationReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(
                    notificationInfoReceiver);
            isNotificationReceiverRegistered = false;
        }
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

    private void bindWithWifiService() {
        // Bind to LocalService
        Intent intent = new Intent(this, WifiMonitoringService.class);
        try {
            if (bindService(intent, wifiServiceConnection, Context.BIND_AUTO_CREATE)) {
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
            unbindService(wifiServiceConnection);
            boundToWifiService = false;
            wifiMonitoringService = null;
            dobbyAnalytics.setWifiServiceUnbindingCalled();
        }
    }

    public void initiateStatusNotification() {
        if (Utils.checkIsWifiMonitoringEnabled(this) && boundToWifiService && wifiMonitoringService != null) {
            wifiMonitoringService.sendStatusUpdateNotification();
        } else {
            //Change text for repair button
            //Notify error to WifiDocMainFragment
            DobbyLog.e("Sending status request failed -- Service unavailable  boundToService " + (boundToWifiService ? Utils.TRUE_STRING : Utils.FALSE_STRING));
        }

    }
}
