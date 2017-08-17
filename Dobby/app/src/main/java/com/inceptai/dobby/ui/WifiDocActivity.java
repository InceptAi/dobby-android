package com.inceptai.dobby.ui;

import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.NetworkLayer;
import com.inceptai.dobby.R;
import com.inceptai.dobby.RemoteConfig;
import com.inceptai.dobby.WifiMonitoringServiceClient;
import com.inceptai.dobby.ai.Action;
import com.inceptai.dobby.ai.DobbyAi;
import com.inceptai.dobby.ai.suggest.LocalSummary;
import com.inceptai.dobby.database.RepairDatabaseWriter;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.fake.FakeDataIntentReceiver;
import com.inceptai.dobby.feedback.RatingsManager;
import com.inceptai.dobby.utils.DobbyLog;
import com.inceptai.dobby.utils.Utils;

import javax.inject.Inject;

import static com.inceptai.dobby.DobbyApplication.TAG;

public class WifiDocActivity extends AppCompatActivity implements
        WifiDocMainFragment.OnFragmentInteractionListener,
        Handler.Callback,
        WifiMonitoringServiceClient.WifiMonitoringCallback {
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
    RepairDatabaseWriter repairDatabaseWriter;
    @Inject
    RemoteConfig remoteConfig;

    private FakeDataIntentReceiver fakeDataIntentReceiver;
    private Handler handler;
    private WifiMonitoringServiceClient wifiMonitoringServiceClient;
    private RatingsManager ratingsManager;
    private boolean isLocationPermissionGranted;

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

        if (!isTaskRoot()
                && getIntent().hasCategory(Intent.CATEGORY_LAUNCHER)
                && getIntent().getAction() != null
                && getIntent().getAction().equals(Intent.ACTION_MAIN)) {
                finish();
            DobbyLog.v("Finishing since this is not root task");
            return;
        }

        handler = new Handler();
        setContentView(R.layout.activity_wifi_doc);
        setupMainFragment();
        networkLayer.fetchLastKnownLocation();
        isLocationPermissionGranted = true;
        wifiMonitoringServiceClient = new WifiMonitoringServiceClient(
                this,
                dobbyApplication.getUserUuid(),
                dobbyApplication.getPhoneInfo(),
                threadpool.getExecutor(),
                this /* callback */);
        ratingsManager = new RatingsManager(this, remoteConfig, threadpool.getExecutor());
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


    public boolean isLocationPermissionGranted() {
        return isLocationPermissionGranted;
    }

    public void setLocationPermissionGranted(boolean locationPermissionGranted) {
        isLocationPermissionGranted = locationPermissionGranted;
    }

    @Override
    public void onUserSaysLaterToGivingRating() {
        ratingsManager.saveRatingPreference(RatingsManager.LATER_PREF);
    }

    @Override
    public void onUserSaysYesToGivingRating() {
        ratingsManager.launchAppStorePageForRatingTheApp();
    }

    @Override
    public void wifiMonitoringStarted() {
        //No-op
    }

    @Override
    public void wifiMonitoringStopped() {
        //No-op
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
        wifiMonitoringServiceClient.pauseNotifications();
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
        wifiMonitoringServiceClient.resumeNotificationIfNeeded();
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
    public void onLocationPermissionDenied() {
        setLocationPermissionGranted(false);
    }

    @Override
    public void onLocationPermissionGranted() {
        //Trigger a wifiScan when permission is granted
        setLocationPermissionGranted(true);
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
        if (wifiMonitoringServiceClient != null) {
            wifiMonitoringServiceClient.disconnect();
        }
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
        wifiMonitoringServiceClient.repairWifiNetwork(0, isLocationPermissionGranted());
    }

    @Override
    public void onWifiMonitoringServiceDisabled() {
        wifiMonitoringServiceClient.disableWifiService();
    }

    @Override
    public void onWifiMonitoringServiceEnabled() {
        wifiMonitoringServiceClient.enableWifiService();
    }

    @Override
    public void onWifiRepairCancelled() {
        DobbyLog.v("WifiDocMainActivity: onWifiRepairCancelled");
        wifiMonitoringServiceClient.cancelWifiRepair();
    }

    @Override
    public void repairStarted(boolean started) {
        if (started) {
            //repair successfully started
        } else {
            //unable to start repair
        }
    }

    @Override
    public void repairFinished(boolean success, WifiInfo repairedWifiInfo, String repairSummary) {
        //Send this information to main fragment
        int textId = R.string.repair_wifi_success;
        if (!success) {
            textId = R.string.repair_wifi_failure;
        }
        if (mainFragment != null) {
            mainFragment.handleRepairFinished(repairedWifiInfo, textId, repairSummary, shouldAskForRating() && success);
        }
    }

    private boolean shouldAskForRating() {
        return ratingsManager.shouldBeAllowedToAskForRating();
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

}
