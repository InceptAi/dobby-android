package com.inceptai.wifiexpertsystem;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.inceptai.neoservice.NeoService;
import com.inceptai.neoservice.NeoUiActionsService;
import com.inceptai.wifiexpertsystem.analytics.DobbyAnalytics;
import com.inceptai.wifiexpertsystem.expert.ExpertChatService;
import com.inceptai.wifiexpertsystem.expertSystem.inferencing.DataInterpreter;
import com.inceptai.wifiexpertsystem.expertSystem.inferencing.SuggestionCreator;
import com.inceptai.wifiexpertsystem.expertSystem.messages.StructuredUserResponse;
import com.inceptai.wifiexpertsystem.heartbeat.HeartBeatManager;
import com.inceptai.wifiexpertsystem.notifications.DisplayAppNotification;
import com.inceptai.wifiexpertsystem.ui.ChatFragment;
import com.inceptai.wifiexpertsystem.ui.WifiExpertDialogFragment;
import com.inceptai.wifiexpertsystem.utils.DobbyLog;
import com.inceptai.wifiexpertsystem.utils.Utils;
import com.inceptai.wifimonitoringservice.WifiMonitoringService;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.wifi.WifiNetworkOverview;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.reactivex.Observable;

import static android.os.Build.VERSION_CODES.M;


public class DobbyActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        ChatFragment.OnFragmentInteractionListener,
        UserInteractionManager.InteractionCallback {

    private static final String NEO_CUSTOM_INTENT = "com.inceptai.wifiexpertsystem.neo.ACTION";
    //permission codes
    private static final int PERMISSION_COARSE_LOCATION_REQUEST_CODE = 101;
    private static final int PERMISSION_OVERLAY_REQUEST_CODE = 201;

    private static final boolean RESUME_WITH_SUGGESTION_IF_AVAILABLE = false;
    private static final int SPEECH_RECOGNITION_REQUEST_CODE = 102;
    private static final long BOT_MESSAGE_DELAY_MS = 500;

    private static final boolean SHOW_CONTACT_HUMAN_BUTTON = true;
    private static final boolean ENABLE_WIFI_MONITORING_SERVICE = false;
    private static final boolean ENABLE_OVERLAY_PERMISSION = true;

    private UserInteractionManager userInteractionManager;
    @Inject
    DobbyAnalytics dobbyAnalytics;
    @Inject
    HeartBeatManager heartBeatManager;
    @Inject DobbyThreadPool dobbyThreadPool;
    private ChatFragment chatFragment;
    private boolean isTaskRoot = true;
    private NotificationInfoReceiver notificationInfoReceiver;
    private NeoCustomIntentReceiver neoCustomIntentReceiver;
    private boolean askedForOverlayPermission;
    private boolean needOverlayPermission;
    private boolean askedForAccessibilityPermission;

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
            isTaskRoot = false;
            finish();
            DobbyLog.v("Finishing since this is not root task");
            return;
        }
        askedForAccessibilityPermission = false;
        needOverlayPermission = false;
        userInteractionManager = new UserInteractionManager(getApplicationContext(), this, SHOW_CONTACT_HUMAN_BUTTON);
        heartBeatManager.setDailyHeartBeat();
        notificationInfoReceiver = new NotificationInfoReceiver();

        setContentView(R.layout.activity_wifi_expert_app);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        if (Build.VERSION.SDK_INT >= M) {
            drawer.addDrawerListener(toggle);
        } else {
            drawer.setDrawerListener(toggle);
        }
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        setupChatFragment();
        registerNeoCustomIntentReceiver();
        if (ENABLE_WIFI_MONITORING_SERVICE) {
            startWifiMonitoringService();
        }
    }



    private ChatFragment getChatFragmentFromTag() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager != null) {
            Fragment existingFragment = fragmentManager.findFragmentByTag(ChatFragment.FRAGMENT_TAG);
            if (existingFragment != null) {
                return (ChatFragment)existingFragment;
            }
        }
        return null;
    }

    private void setupChatFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.placeholder_fl,
                new ChatFragment(), ChatFragment.FRAGMENT_TAG);
        //fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }


    @Override
    public void onBackPressed(){
        moveTaskToBack(true);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /*
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    */

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        if (id == R.id.about_wifi_expert) {
            showAboutAndPrivacyPolicy();
        } else if (id == R.id.feedback_wifi_expert) {
            showFeedbackForm();
        }
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        item.setChecked(false);
        return true;
    }

    //Interaction manager callback
    @Override
    public void observeBandwidth(Observable bandwidthObservable) {
        DobbyLog.v("DobbyActivity: In observerBandwidth callback");
        if (chatFragment != null) {
            DobbyLog.v("DobbyActivity: Sending observerBandwidth to CF");
            chatFragment.observeBandwidthNonUi(bandwidthObservable);
        }
    }

    @Override
    public void showBotResponse(String text) {
        if (chatFragment != null) {
            chatFragment.showBotResponse(text);
        }
    }

    @Override
    public void showUserResponse(String text) {
        if (chatFragment != null) {
            chatFragment.showUserResponse(text);
        }

    }

    @Override
    public void showExpertResponse(String text) {
        if (chatFragment != null) {
            chatFragment.showExpertChatMessage(text);
        }
    }

    @Override
    public void showStatusUpdate(String text) {
        if (chatFragment != null) {
            chatFragment.showStatus(text);
        }
    }

    @Override
    public void updateExpertIndicator(String text) {
        if (chatFragment != null) {
            chatFragment.showExpertIndicatorWithText(text);
        }
    }

    @Override
    public void hideExpertIndicator() {
        if (chatFragment != null) {
            chatFragment.hideExpertIndicator();
        }
    }

    @Override
    public void cancelTestsResponse() {
        if (chatFragment != null) {
            chatFragment.cancelTests();
        }
    }

    @Override
    public void showBandwidthViewCard(double downloadMbps, double uploadMbps) {
        if (chatFragment != null) {
            chatFragment.addBandwidthResultsCardView(downloadMbps, uploadMbps);
        }
    }

    @Override
    public void showNetworkInfoViewCard(WifiNetworkOverview wifiNetworkOverview) {
        if (chatFragment != null) {
            chatFragment.addOverallNetworkResultsCardView(wifiNetworkOverview);
        }
    }

    @Override
    public void showDetailedSuggestions(SuggestionCreator.Suggestion suggestion) {
        DobbyLog.v("In showDetailedSuggestions of DobbyActivity");
        if (chatFragment != null) {
            chatFragment.showDetailedSuggestionsView(suggestion);
        }
    }


    @Override
    public void showUserActionOptions(List<StructuredUserResponse> structuredUserResponses) {
        DobbyLog.v("In showUserActionOptions of DobbyActivity: responseTypes: ");
        if (chatFragment != null) {
            chatFragment.showUserActionOptions(structuredUserResponses);
        }
    }


    @Override
    public void showFillerTypingMessage(String text) {
        if (chatFragment != null) {
            chatFragment.showStatus(text, 0);
        }
    }

    @Override
    public void showPingInfoViewCard(DataInterpreter.PingGrade pingGrade) {
        if (chatFragment != null) {
            chatFragment.addPingResultsCardView(pingGrade);
        }
    }

    //From chatFragment onInteractionListener
    @Override
    public void onUserQuery(String text, boolean isStructuredResponse, @StructuredUserResponse.ResponseType int responseType, int responseId) {
        userInteractionManager.onUserQuery(text, isStructuredResponse, responseType, responseId);
    }



    public DobbyActivity() {
        super();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    private void processIntent(Intent intent) {
        String notifSource = intent.getStringExtra(ExpertChatService.INTENT_NOTIF_SOURCE);
        if (notifSource != null) {
            userInteractionManager.notificationConsumed();
        }
    }

    @TargetApi(M)
    public void showLocationPermissionRequest() {
        if (Build.VERSION.SDK_INT >= M) {
            // Assume thisActivity is the current activity
            int permissionCheck = this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                WifiExpertDialogFragment fragment = WifiExpertDialogFragment.forLocationPermission(this);
                fragment.show(this.getSupportFragmentManager(), "Request Location Permission.");
            }
        }
    }


    @TargetApi(M)
    public void showLocationAndOverdrawPermissionRequest() {
        if (Build.VERSION.SDK_INT >= M) {
            // Assume thisActivity is the current activity
            boolean locationPermissionGranted = this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
            boolean overdrawPermissionGranted = Settings.canDrawOverlays(this);
            if (!locationPermissionGranted && !overdrawPermissionGranted) {
                WifiExpertDialogFragment fragment = WifiExpertDialogFragment.forLocationAndOverdrawPermission(this);
                fragment.show(this.getSupportFragmentManager(), "Request Location and Draw Permission.");
            }
        }
    }

    public void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_COARSE_LOCATION_REQUEST_CODE);
    }

    public void requestLocationAndOverdrawPermission() {
        needOverlayPermission = true;
        requestLocationPermission();
    }


    @Override
    public void requestAccessibilityPermission() {
        showAccessibilityPermissionDialog();
    }

    private void showAccessibilityPermissionDialog() {
        WifiExpertDialogFragment fragment = WifiExpertDialogFragment.forAccessibilityPermission(this);
        fragment.show(this.getSupportFragmentManager(), "Accessibility Permission.");
        new AccessibilityPermissionChecker(dobbyThreadPool.getScheduledExecutorService()).startChecking();
    }

    public void takeUserToAccessibilitySetting() {
        NeoService.showAccessibilitySettings(this);
    }
    //Neo stuff
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        DobbyLog.i("onActivityResult:" + requestCode + " package name: " + getPackageName());
        if (requestCode == PERMISSION_OVERLAY_REQUEST_CODE && isAndroidMOrLater()) {
            askedForOverlayPermission = false;
            if (Settings.canDrawOverlays(this)) {
                //Callback that permission granted
                userInteractionManager.overlayPermissionStatus(true);
                //navigate back to current activity
            } else {
                //Callback that permission denied
                //Toast.makeText(this, "ACTION_MANAGE_OVERLAY_PERMISSION Permission Denied", Toast.LENGTH_SHORT).show();
                userInteractionManager.overlayPermissionStatus(false);
            }
        } else if (requestCode == SPEECH_RECOGNITION_REQUEST_CODE) {
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> res = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String inSpeech = res.get(0);
                if (chatFragment != null) {
                    chatFragment.addSpokenText(inSpeech);
                }
                onUserQuery(inSpeech, false, StructuredUserResponse.ResponseType.UNKNOWN, 0);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @TargetApi(M)
    private void askForOverlayPermission() {
        askedForOverlayPermission = true;
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, PERMISSION_OVERLAY_REQUEST_CODE);
        new OverlayPermissionChecker(dobbyThreadPool.getScheduledExecutorService()).startChecking();
    }

    private boolean isAndroidMOrLater() {
        return Build.VERSION.SDK_INT >= M;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_COARSE_LOCATION_REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    DobbyLog.i("Coarse location permission granted.");
                } else {
                    Utils.buildSimpleDialog(this, "Functionality limited",
                            "Since location access has not been granted, this app will not be able to analyze your wifi network.");
                }
                if (needOverlayPermission) {
                    askForOverlayPermission();
                }
                return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        DobbyLog.v("DobbyActivity: onResume");
        userInteractionManager.onUserEnteredChat();
        unRegisterNotificationInfoReceiver();
        processIntent(getIntent());
    }


    @Override
    protected void onDestroy() {
        DobbyLog.v("DobbyActivity: onDestroy");
        super.onDestroy();
        if (isTaskRoot) {
            userInteractionManager.cleanup();
            removeNeoCustomIntentReceiver();
        }
    }

    @Override
    protected void onStop() {
        DobbyLog.v("DobbyActivity: onStop");
        super.onStop();
        userInteractionManager.onUserExitChat();
    }

    @Override
    protected void onPause() {
        super.onPause();
        registerNotificationInfoReceiver();
    }

    @Override
    protected void onStart() {
        super.onStart();
        DobbyLog.v("DobbyActivity: onStart");
    }

    @Override
    public void onMicPressed() {
        listen();
    }

    @Override
    public void onRecyclerViewReady() {
        //Get the welcome message
        DobbyLog.v("DobbyActivity:onRecyclerViewReady");
        if (!NeoUiActionsService.isAccessibilityPermissionGranted(DobbyActivity.this) &&
                !askedForAccessibilityPermission) {
            askedForAccessibilityPermission = true;
            showAccessibilityPermissionDialog();
        }
        //showLocationPermissionRequest();
        //showLocationAndOverdrawPermissionRequest();
    }

    @Override
    public void onFirstTimeResumed() {
        DobbyLog.v("DobbyActivity:onFirstTimeResumed");
        userInteractionManager.resumeChatWithWifiCheck();
    }

    @Override
    public void onFragmentGone() {
        DobbyLog.v("DobbyActivity:onFragmentGone Setting chatFragment to null");
        //Setting chat fragment to null
        chatFragment = null;
    }

    @Override
    public void onFragmentReady() {
        DobbyLog.v("DobbyActivity:onFragmentReady Setting chatFragment based on tag");
        //Setting chat fragment here
        chatFragment = getChatFragmentFromTag();
        if (chatFragment != null) {
            chatFragment.setBotMessageDelay(BOT_MESSAGE_DELAY_MS);
        }
        //Don't do resume stuff for now
    }


    private void startWifiMonitoringService() {
        Intent serviceStartIntent = new Intent(this, WifiMonitoringService.class);
        //serviceStartIntent.putExtra(NotificationInfoKeys., NOTIFICATION_INFO_INTENT_VALUE);
        startService(new Intent(this, WifiMonitoringService.class));
    }

    private void showAboutAndPrivacyPolicy() {
        WifiExpertDialogFragment fragment = WifiExpertDialogFragment.forAboutAndPrivacyPolicy();
        fragment.show(getSupportFragmentManager(), "About");
        dobbyAnalytics.aboutShown();
    }

    private void showFeedbackForm() {
        WifiExpertDialogFragment fragment = WifiExpertDialogFragment.forFeedback(R.layout.activity_wifi_expert_app);
        fragment.show(getSupportFragmentManager(), "Feedback");
        dobbyAnalytics.feedbackFormShown();
    }

    private Fragment getFragmentByTag(String tag) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        return fragmentManager.findFragmentByTag(tag);
    }

    private void listen() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something");

        try {
            startActivityForResult(i, SPEECH_RECOGNITION_REQUEST_CODE);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(DobbyActivity.this, "Your device doesn't support Speech Recognition", Toast.LENGTH_SHORT).show();
        }
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
            dobbyThreadPool.getExecutor().execute(new DisplayAppNotification(context, notificationTitle, notificationBody, notificationId));
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

    private class NeoCustomIntentReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            DobbyActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    userInteractionManager.toggleNeoService();
                }
            });
        }
    }

    private void registerNeoCustomIntentReceiver() {
        neoCustomIntentReceiver = new NeoCustomIntentReceiver();
        IntentFilter intentFilter = new IntentFilter(NEO_CUSTOM_INTENT);
        registerReceiver(neoCustomIntentReceiver, intentFilter);
    }

    private void removeNeoCustomIntentReceiver() {
        if (neoCustomIntentReceiver != null) {
            unregisterReceiver(neoCustomIntentReceiver);
            neoCustomIntentReceiver = null;
        }
    }

    @TargetApi(M)
    private boolean checkOverlayPermissionAndLaunchMainActivity() {
        if (Settings.canDrawOverlays(DobbyActivity.this)) {
            //You have the permission, re-launch DobbyActivity
            Utils.launchWifiExpertMainActivity(this.getApplicationContext());
            return true;
        }
        return false;
    }

    private class OverlayPermissionChecker {
        private static final int CHECKING_INTERVAL_MS = 1000;
        private static final int MAX_ATTEMPTS = 5;

        private ScheduledExecutorService scheduledExecutorService;
        private int numAttempts;

        public OverlayPermissionChecker(ScheduledExecutorService scheduledExecutorService) {
            this.scheduledExecutorService = scheduledExecutorService;
            numAttempts = 0;
        }

        void startChecking() {
            postOverdrawCheck();
        }

        private void postOverdrawCheck() {
            scheduledExecutorService.schedule(new Runnable() {
                @Override
                public void run() {
                    numAttempts++;
                    if (!DobbyActivity.this.checkOverlayPermissionAndLaunchMainActivity() && numAttempts < MAX_ATTEMPTS) {
                        postOverdrawCheck();
                    }
                }
            }, CHECKING_INTERVAL_MS, TimeUnit.MILLISECONDS);
        }
    }

    @TargetApi(M)
    private boolean checkAccessibilityPermissionAndLaunchMainActivity() {
        if (NeoUiActionsService.isAccessibilityPermissionGranted(DobbyActivity.this)) {
            //You have the permission, re-launch MainActivity
            userInteractionManager.onAccessibilityPermissionGranted(false);
            Utils.launchWifiExpertMainActivity(this.getApplicationContext());
            return true;
        }
        return false;
    }

    private class AccessibilityPermissionChecker {
        private static final int CHECKING_INTERVAL_MS = 1000;
        private static final int MAX_ATTEMPTS = 20;

        private ScheduledExecutorService scheduledExecutorService;
        private int numAttempts;

        public AccessibilityPermissionChecker(ScheduledExecutorService scheduledExecutorService) {
            this.scheduledExecutorService = scheduledExecutorService;
            numAttempts = 0;
        }

        void startChecking() {
            postOverdrawCheck();
        }

        private void postOverdrawCheck() {
            scheduledExecutorService.schedule(new Runnable() {
                @Override
                public void run() {
                    numAttempts++;
                    if (!DobbyActivity.this.checkAccessibilityPermissionAndLaunchMainActivity() && numAttempts < MAX_ATTEMPTS) {
                        postOverdrawCheck();
                    }
                }
            }, CHECKING_INTERVAL_MS, TimeUnit.MILLISECONDS);
        }
    }

}

