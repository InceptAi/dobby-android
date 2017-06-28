package com.inceptai.dobby;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.dobby.ai.DataInterpreter;
import com.inceptai.dobby.ai.DobbyAi;
import com.inceptai.dobby.ai.RtDataSource;
import com.inceptai.dobby.ai.SuggestionCreator;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.expert.ExpertChat;
import com.inceptai.dobby.expert.ExpertChatService;
import com.inceptai.dobby.heartbeat.HeartBeatManager;
import com.inceptai.dobby.speedtest.BandwidthObserver;
import com.inceptai.dobby.ui.ChatFragment;
import com.inceptai.dobby.ui.DebugFragment;
import com.inceptai.dobby.ui.FakeDataFragment;
import com.inceptai.dobby.ui.WifiDocDialogFragment;
import com.inceptai.dobby.ui.WifiFragment;
import com.inceptai.dobby.utils.DobbyLog;
import com.inceptai.dobby.utils.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static com.inceptai.dobby.utils.Utils.EMPTY_STRING;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        DobbyAi.ResponseCallback,
        Handler.Callback,
        ChatFragment.OnFragmentInteractionListener,
        ExpertChatService.ChatCallback {

    private static final int PERMISSION_COARSE_LOCATION_REQUEST_CODE = 101;
    private static final int SPEECH_RECOGNITION_REQUEST_CODE = 102;
    private static final boolean ENABLE_HISTORY = true;

    private static final String PREF_FIRST_CHAT = "first_dobby_expert_chat";
    private static final String PREF_CHAT_IN_EXPERT_MODE = "dobby_in_expert_mode";
    private static final String EXPERT_MODE_INITIATED_TIMESTAMP = "expert_mode_start_ts";
    private static final long MAX_TIME_ELAPSED_FOR_RESUMING_EXPERT_MODE_MS = AlarmManager.INTERVAL_DAY;
    private static final boolean ENABLE_AGENT_TYPING_FILLER = true;



    @Inject DobbyApplication dobbyApplication;
    @Inject DobbyThreadpool threadpool;
    @Inject DobbyAi dobbyAi;
    @Inject NetworkLayer networkLayer;
    @Inject DobbyEventBus eventBus;
    @Inject DobbyAnalytics dobbyAnalytics;
    @Inject HeartBeatManager heartBeatManager;
    @Inject ExpertChatService expertChatService;


    private Handler handler;
    private ChatFragment chatFragment;
    private boolean expertIsPresent = false;
    private long currentEtaSeconds;
    private long sessionStartedTimestamp;
    private Set<String> expertChatIdsDisplayed;
    private boolean isTaskRoot = true;

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

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("Foo bar");

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            drawer.addDrawerListener(toggle);
        } else {
            drawer.setDrawerListener(toggle);
        }
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        expertChatIdsDisplayed = new HashSet<>();
        currentEtaSeconds = ExpertChatService.ETA_OFFLINE;
        expertIsPresent = false;
        sessionStartedTimestamp = System.currentTimeMillis();

        dobbyAi.setResponseCallback(this);
        dobbyAi.initChatToBotState(); //Resets booleans indicating which mode of expert are we in

        handler = new Handler(this);
        setupChatFragment();
        heartBeatManager.setDailyHeartBeat();

        //Don't do resume stuff for now
        if (checkSharedPrefForExpertModeResume()) {
            dobbyAi.contactExpert();
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

    private Fragment setupFragment(Class fragmentClass, String tag) {

        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment existingFragment = fragmentManager.findFragmentByTag(tag);
        if (existingFragment == null) {
            try {
                existingFragment = (Fragment) fragmentClass.newInstance();
            } catch (Exception e) {
                DobbyLog.e("Unable to create fragment: " + fragmentClass.getCanonicalName());
                return null;
            }
        }
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.placeholder_fl,
                existingFragment, tag);
        //fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
        return existingFragment;
    }

    private void setupChatFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.placeholder_fl,
                new ChatFragment(), ChatFragment.FRAGMENT_TAG);
        //fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void fetchChatMessages() {
        DobbyLog.v("MainActivity: start fetching");
        expertChatService.setCallback(this);
        if (!expertChatService.isListenerConnected()) {
            DobbyLog.v("MainActivity: listener connected, fetching messages");
            expertChatService.fetchChatMessages();
        }
        DobbyLog.v("MainActivity: end fetching");
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

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_wifi_scan) {
            ListenableFuture<List<ScanResult>> scanFuture = networkLayer.wifiScan();
            Toast.makeText(this, "Starting wifi scan...", Toast.LENGTH_SHORT).show();
            WifiFragment fragment = (WifiFragment) setupFragment(WifiFragment.class, WifiFragment.FRAGMENT_TAG);
            fragment.setWifiScanFuture(scanFuture, threadpool.getExecutor());

        } else if (id == R.id.nav_debug) {
            DebugFragment fragment = (DebugFragment) setupFragment(DebugFragment.class, DebugFragment.FRAGMENT_TAG);

        } else if (id == R.id.nav_fake_data) {
            FakeDataFragment fragment = (FakeDataFragment) setupFragment(FakeDataFragment.class, FakeDataFragment.FRAGMENT_TAG);

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        } else if (id == R.id.about_wifi_expert) {
            showAboutAndPrivacyPolicy();
        } else if (id == R.id.feedback_wifi_expert) {
            showFeedbackForm();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        item.setChecked(false);
        return true;
    }

    // From DobbyAi.ResponseCallback interface.
    @Override
    public void showBotResponseToUser(String text) {
        DobbyLog.v("MainActivity:showBotResponseToUser text: " + text);
        pushBotChatMessage(text);
    }

    @Override
    public void showStatus(String text) {
        if (ENABLE_AGENT_TYPING_FILLER && chatFragment != null) {
//            SpannableString spanString = new SpannableString(text);
//            spanString.setSpan(new StyleSpan(Typeface.ITALIC), 0, spanString.length(), 0);
//            chatFragment.showStatus(spanString.toString());
            DobbyLog.v("MainActivity:showStatus text: " + text);
            chatFragment.showStatus(text);
        }
    }

    @Override
    public void showBandwidthViewCard(DataInterpreter.BandwidthGrade bandwidthGrade) {
        if (chatFragment != null) {
            chatFragment.addBandwidthResultsCardView(bandwidthGrade);
        }
    }

    @Override
    public void showNetworkInfoViewCard(DataInterpreter.WifiGrade wifiGrade, String isp, String ip) {
        if (chatFragment != null) {
            chatFragment.addOverallNetworkResultsCardView(wifiGrade, isp, ip);
        }
        pushBotChatMessage(wifiGrade.userReadableInterpretation());
    }

    @Override
    public void showUserActionOptions(List<Integer> userResponseTypes) {
        DobbyLog.v("In showUserActionOptions of MainActivity: responseTypes: " + userResponseTypes);
        if (chatFragment != null) {
            chatFragment.showUserActionOptions(userResponseTypes);
        }
    }

    @Override
    public void showDetailedSuggestions(SuggestionCreator.Suggestion suggestion) {
        DobbyLog.v("In showDetailedSuggestions of MainActivity");
        if (chatFragment != null) {
            chatFragment.showDetailedSuggestionsView(suggestion);
        }
    }

    @Override
    public void onUserQuery(String text, boolean isButtonActionText) {
        dobbyAi.sendQuery(text, isButtonActionText);
    }

    // From DobbyAi.ResponseCallback interface.
    @Override
    public void showRtGraph(RtDataSource<Float, Integer> rtDataSource) {
        // chatFragment.showRtGraph(rtDataSource);
    }

    @Override
    public void cancelTests() {
        if (chatFragment != null) {
            chatFragment.cancelTests();
        }
    }


    @Override
    public void onUserMessageAvailable(String text, boolean sendMessageToExpert) {
        DobbyLog.v("Pushing out user message " + text + " send to expert " + (sendMessageToExpert ? Utils.TRUE_STRING : Utils.FALSE_STRING));
        pushUserChatMessage(text, sendMessageToExpert);
    }

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }

    @Override
    public void contactExpertAndGetETA() {
        //Contact expert and show ETA to user
        //Showing ETA to user
        updateEta(currentEtaSeconds, expertIsPresent, true);
        sendInitialMessageToExpert();
        //Show a message showing trying to get expert
        updateExpertIndicator();
    }

    @Override
    public void switchedToExpertIsListeningMode() {
        //Show the message saying you are talking to expert
        updateExpertIndicator();
    }

    public MainActivity() {
        super();
    }

    private void sendInitialMessageToExpert() {
        //Contacting expert
        ExpertChat expertChat = new ExpertChat("Expert help needed here", ExpertChat.MSG_TYPE_META_SEND_MESSAGE_TO_EXPERT_FOR_HELP);
        expertChatService.pushUserChatMessage(expertChat, true);
    }

    @Override
    public void switchedToExpertMode() {
        saveSwitchedToExpertMode();
        updateExpertIndicator();
        if (expertChatService != null) {
            expertChatService.checkIn(this);
        }
    }

    @Override
    public void switchedToBotMode() {
        saveSwitchedToBotMode();
        updateExpertIndicator();
    }

    @Override
    public void userAskedForExpert() {
        updateExpertIndicator();
    }

    //Handle ExpertChatServiceCallback
    @Override
    public void onMessageAvailable(ExpertChat expertChat) {
        //Check the timestamp | if this is the history, then display all else just display the expert chat message
        //TODO finish the function
        if (!ENABLE_HISTORY) {
            if (expertChat.getUtcTimestampMs() < sessionStartedTimestamp) {
                //This message is in past and history is disabled
                return;
            }
            if (expertChat.getMessageType() != ExpertChat.MSG_TYPE_EXPERT_TEXT) {
                //History is disabled and message type is not expert type
                return;
            }
        }

        //Check if hashset contains this id
        if (!expertChat.getId().equals(Utils.EMPTY_STRING) && expertChatIdsDisplayed.contains(expertChat.getId())) {
            //We have already displayed this chat message
            DobbyLog.v("Ignoring since empty text " + expertChat.getText());
            return;
        }

        expertChatIdsDisplayed.add(expertChat.getId());
        String messageReceived = expertChat.getText();
        DobbyLog.v("MainActivity:FirebaseMessage recvd from firebase: " + messageReceived);

        if (chatFragment == null) {
            DobbyLog.v("chat fragment is null, so not displaying mesg");
            return;
        }

        switch (expertChat.getMessageType()) {
            case ExpertChat.MSG_TYPE_EXPERT_TEXT:
                chatFragment.showExpertChatMessage(messageReceived);
                DobbyLog.v("MainActivity:FirebaseMessage added mesg to ExpertChat");
                saveExpertChatStarted();
                break;
            case ExpertChat.MSG_TYPE_BOT_TEXT:
                DobbyLog.v("MainActivity:FirebaseMessage added mesg to Bot chat");
                chatFragment.showBotResponse(messageReceived);
                break;
            case ExpertChat.MSG_TYPE_USER_TEXT:
                DobbyLog.v("MainActivity:FirebaseMessage added mesg to User chat");
                chatFragment.showUserResponse(messageReceived);
                if (!dobbyAi.getIsChatInExpertMode()) {
                    showStatus(getString(R.string.agent_is_typing));
                }
                break;
        }
    }

    @Override
    public void onNoHistoryAvailable() {

    }

    @Override
    public void onEtaUpdated(long newEtaSeconds, boolean isPresent) {
        updateEta(newEtaSeconds, isPresent, false);
    }

    @Override
    public void onEtaAvailable(long newEtaSeconds, boolean isPresent) {
        updateEta(newEtaSeconds, isPresent, false);
    }


    private void updateEta(long newEtaSeconds, boolean isPresent, boolean showInChat) {
        String messagePrefix = getResources().getString(R.string.expected_response_time_for_expert);
        String message = EMPTY_STRING;
        if (!isPresent || newEtaSeconds > ExpertChatService.ETA_12HOURS) {
            message = "Our experts are currently offline. You shall receive a response in about 12 hours.";
        } else {
            message = messagePrefix + " Less than " + Utils.timeSecondsToString(newEtaSeconds);
        }
        currentEtaSeconds = newEtaSeconds;
        expertIsPresent = isPresent;
        if (showInChat) {
            showBotResponseToUser(message);
        }
        dobbyAi.updatedEtaAvailable(currentEtaSeconds);
    }



    public void sendEvent(String eventString) {
        if (dobbyAi != null) {
            dobbyAi.sendEvent(eventString);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    private void processIntent(Intent intent) {
        String notifSource = intent.getStringExtra(ExpertChatService.INTENT_NOTIF_SOURCE);
        if (notifSource != null) {
            dobbyAnalytics.expertChatNotificationConsumed();
        }
    }

    private void saveExpertChatStarted() {
        Utils.saveSharedSetting(this,
                PREF_FIRST_CHAT, Utils.FALSE_STRING);
    }

    private boolean isFirstChat() {
        return Boolean.valueOf(Utils.readSharedSetting(this,
                PREF_FIRST_CHAT, Utils.TRUE_STRING));
    }

    private void saveSwitchedToBotMode() {
        Utils.saveSharedSetting(this,
                PREF_CHAT_IN_EXPERT_MODE, Utils.FALSE_STRING);
        Utils.saveSharedSetting(this,
                EXPERT_MODE_INITIATED_TIMESTAMP, 0);
    }

    private void saveSwitchedToExpertMode() {
        Utils.saveSharedSetting(this,
                PREF_CHAT_IN_EXPERT_MODE, Utils.TRUE_STRING);
        Utils.saveSharedSetting(this,
                EXPERT_MODE_INITIATED_TIMESTAMP, System.currentTimeMillis());
    }

    private boolean checkSharedPrefForExpertModeResume() {
        long lastExpertInitiatedAtMs = Utils.readSharedSetting(this, EXPERT_MODE_INITIATED_TIMESTAMP, 0);
        if (lastExpertInitiatedAtMs > 0 &&
                System.currentTimeMillis() - lastExpertInitiatedAtMs < MAX_TIME_ELAPSED_FOR_RESUMING_EXPERT_MODE_MS) {
            return true;
        }
        return false;
    }

    private void pushUserChatMessage(String text, boolean shouldShowToExpert) {
        DobbyLog.v("MainActivity:Firebase Pushing user message " + text);
        ExpertChat expertChat = new
                ExpertChat(text, ExpertChat.MSG_TYPE_USER_TEXT);
        expertChatService.pushUserChatMessage(expertChat, shouldShowToExpert);
    }

    private void pushBotChatMessage(String text) {
        DobbyLog.v("MainActivity:Firebase Pushing bot message " + text);
        ExpertChat expertChat = new
                ExpertChat(text, ExpertChat.MSG_TYPE_BOT_TEXT);
        expertChatService.pushBotChatMessage(expertChat);
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void showLocationPermissionRequest() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Assume thisActivity is the current activity
            int permissionCheck = this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                WifiDocDialogFragment fragment = WifiDocDialogFragment.forDobbyLocationPermission(this);
                fragment.show(this.getSupportFragmentManager(), "Request Location Permission.");
            }
        }
    }

    public void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_COARSE_LOCATION_REQUEST_CODE);
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
                return;
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //expertChatService.setCallback(this);
        dobbyAi.setResponseCallback(this);
        fetchChatMessages();
        processIntent(getIntent());
        expertChatService.sendUserEnteredMetaMessage();
        expertChatService.disableNotifications();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isTaskRoot) {
            expertChatService.unregisterChatCallback();
            expertChatService.disconnect();
            dobbyAi.cleanup();
        }
    }

    @Override
    protected void onStop() {
        expertChatService.sendUserLeftMetaMessage();
        expertChatService.enableNotifications();
        super.onStop();
    }

    @Override
    protected void onStart() {
        expertChatService.registerToEventBusListener();
        super.onStart();
    }

    @Override
    public void observeBandwidth(BandwidthObserver observer) {
        if (chatFragment != null) {
            chatFragment.observeBandwidthNonUi(observer);
        }
    }

    @Override
    public void onMicPressed() {
        listen();
    }

    @Override
    public void onRecyclerViewReady() {
        //Get the welcome message
        DobbyLog.v("MainActivity:onRecyclerViewReady");
        showLocationPermissionRequest();
        updateExpertIndicator();
    }

    @Override
    public void onFirstTimeResumed() {
        DobbyLog.v("MainActivity:onFirstTimeResumed");
        if (dobbyAi != null) {
            threadpool.getScheduledExecutorService().schedule(new Runnable() {
                @Override
                public void run() {
                    dobbyAi.sendWelcomeEvent();
                }
            }, 500, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void onFragmentDetached() {
        DobbyLog.v("MainActivity:onFragmentDetached Setting chatFragment to null");
        //Setting chat fragment to null
        chatFragment = null;
    }

    @Override
    public void onFragmentAttached() {
        DobbyLog.v("MainActivity:onFragmentAttached Setting chatFragment based on tag");
        //Setting chat fragment here
        chatFragment = getChatFragmentFromTag();
        fetchChatMessages();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SPEECH_RECOGNITION_REQUEST_CODE) {
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> res = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String inSpeech = res.get(0);
                if (chatFragment != null) {
                    chatFragment.addSpokenText(inSpeech);
                }
                onUserQuery(inSpeech, false);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void updateExpertIndicator() {
        if (chatFragment != null) {
            if (dobbyAi.getIsExpertListening()) {
                chatFragment.showExpertIndicatorWithText(getString(R.string.you_are_now_talking_to_human_expert));
            } else if (dobbyAi.getIsChatInExpertMode()){
                chatFragment.showExpertIndicatorWithText(getString(R.string.contacting_human_expert));
            } else if (dobbyAi.getUserAskedForExpertMode() && !dobbyAi.getIsChatInExpertMode()){
                chatFragment.showExpertIndicatorWithText(getString(R.string.pre_human_contact_tests));
            } else {
                chatFragment.hideExpertIndicator();
            }
        }
    }

    private void showAboutAndPrivacyPolicy() {
        WifiDocDialogFragment fragment = WifiDocDialogFragment.forAboutAndPrivacyPolicy();
        fragment.show(getSupportFragmentManager(), "About");
        dobbyAnalytics.aboutShown();
    }

    private void showFeedbackForm() {
        WifiDocDialogFragment fragment = WifiDocDialogFragment.forFeedback(R.layout.activity_main);
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
                Toast.makeText(MainActivity.this, "Your device doesn't support Speech Recognition", Toast.LENGTH_SHORT).show();
            }
    }
}
