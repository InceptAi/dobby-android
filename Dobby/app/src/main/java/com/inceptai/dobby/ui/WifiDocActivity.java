package com.inceptai.dobby.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;

import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.NetworkLayer;
import com.inceptai.dobby.R;
import com.inceptai.dobby.ai.Action;
import com.inceptai.dobby.ai.DobbyAi;
import com.inceptai.dobby.ai.suggest.LocalSummary;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.fake.FakeDataIntentReceiver;
import com.inceptai.dobby.notifications.DisplayAppNotification;
import com.inceptai.dobby.utils.DobbyLog;
import com.inceptai.dobby.utils.Utils;
import com.inceptai.wifimonitoringservice.WifiMonitoringService;

import javax.inject.Inject;

import static com.inceptai.dobby.DobbyApplication.TAG;

public class WifiDocActivity extends AppCompatActivity implements WifiDocMainFragment.OnFragmentInteractionListener, Handler.Callback {
    public static final String PREF_FIRST_TIME_USER = "WifiTesterNewbie";

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

    private FakeDataIntentReceiver fakeDataIntentReceiver;
    private Handler handler;
    private NotificationInfoReceiver notificationInfoReceiver;


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

//        if (!isTaskRoot()
//                && getIntent().hasCategory(Intent.CATEGORY_LAUNCHER)
//                && getIntent().getAction() != null
//                && getIntent().getAction().equals(Intent.ACTION_MAIN)) {
//            finish();
//            DobbyLog.v("Finishing since this is not root task");
//            return;
//        }

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
    }

    public void setupMainFragment() {
        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment existingFragment = fragmentManager.findFragmentByTag(WifiDocMainFragment.TAG);
        if (existingFragment == null) {
            try {
                existingFragment = (Fragment) WifiDocMainFragment.newInstance(Utils.EMPTY_STRING);
            } catch (Exception e) {
                DobbyLog.e("Unable to create WifiDocMainFragment");
            }
        }
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.wifi_doc_placeholder_fl, existingFragment, TAG);
        // fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
        mainFragment = (WifiDocMainFragment) existingFragment;
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
        registerNotificationInfoReceiver();
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
    protected void onStop() {
        super.onStop();
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

    private void startWifiMonitoringService() {
        Intent serviceStartIntent = new Intent(this, WifiMonitoringService.class);
        //serviceStartIntent.putExtra(NotificationInfoKeys., NOTIFICATION_INFO_INTENT_VALUE);
        startService(new Intent(this, WifiMonitoringService.class));
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
            threadpool.getExecutor().execute(new DisplayAppNotification(context, notificationTitle, notificationBody, notificationId));
        }
    }

    private void registerNotificationInfoReceiver() {
        IntentFilter intentFilter = new IntentFilter(WifiMonitoringService.NOTIFICATION_INFO_INTENT_VALUE);
        LocalBroadcastManager.getInstance(this).registerReceiver(
                notificationInfoReceiver, intentFilter);
    }

    private void unRegisterNotificationInfoReceiver() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(
                notificationInfoReceiver);
    }
}
