package com.inceptai.dobby;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
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
import com.inceptai.dobby.ui.ExpertChatActivity;
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
    private static final boolean ENABLE_HISTORY = false;

    private static final String PREF_FIRST_CHAT = "first_dobby_expert_chat";


    @Inject DobbyApplication dobbyApplication;
    @Inject DobbyThreadpool threadpool;
    @Inject DobbyAi dobbyAi;
    @Inject NetworkLayer networkLayer;
    @Inject DobbyEventBus eventBus;
    @Inject DobbyAnalytics dobbyAnalytics;
    @Inject HeartBeatManager heartBeatManager;


    private Handler handler;
    private ChatFragment chatFragment;
    private boolean isFragmentActive = false;
    private ExpertChatService expertChatService;
    private boolean expertIsPresent = false;
    private long currentEtaSeconds;
    private long sessionStartedTimestamp;
    private Set<String> expertChatIdsDisplayed;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((DobbyApplication) getApplication()).getProdComponent().inject(this);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

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

        dobbyAi.setResponseCallback(this);
        handler = new Handler(this);

        setupChatFragment();
        heartBeatManager.setDailyHeartBeat();
        expertChatService = ExpertChatService.get();


        //TODO: Check do we need this
        fetchChatMessages();
        currentEtaSeconds = ExpertChatService.ETA_OFFLINE;
        expertIsPresent = false;
        sessionStartedTimestamp = System.currentTimeMillis();
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
        chatFragment = new ChatFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.placeholder_fl,
                chatFragment, ChatFragment.FRAGMENT_TAG);
        //fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void fetchChatMessages() {
        expertChatService.setCallback(this);
        if (!expertChatService.isListenerConnected()) {
            //recyclerViewAdapter.clear();
            expertChatService.fetchChatMessages();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
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
    public void showResponse(String text) {
        DobbyLog.v("In showResponse of MainActivity: text: " + text);
        if (isFragmentActive) {
            chatFragment.showResponse(text);
        }
        pushBotChatMessage(text);
    }

    @Override
    public void showStatus(String text) {
        DobbyLog.v("In showStatus of MainActivity: text: " + text);
        if (isFragmentActive) {
            chatFragment.showStatus(text);
        }
        pushBotChatMessage(text);
    }

    @Override
    public void showBandwidthViewCard(DataInterpreter.BandwidthGrade bandwidthGrade) {
        if (isFragmentActive) {
            chatFragment.addBandwidthResultsCardView(bandwidthGrade);
        }
    }

    @Override
    public void showNetworkInfoViewCard(DataInterpreter.WifiGrade wifiGrade, String isp, String ip) {
        if (isFragmentActive) {
            chatFragment.addOverallNetworkResultsCardView(wifiGrade, isp, ip);
        }
    }

    @Override
    public void showUserActionOptions(List<Integer> userResponseTypes) {
        DobbyLog.v("In showUserActionOptions of MainActivity: responseTypes: " + userResponseTypes);
        if (isFragmentActive) {
            chatFragment.showUserActionOptions(userResponseTypes);
        }
    }

    @Override
    public void showDetailedSuggestions(SuggestionCreator.Suggestion suggestion) {
        DobbyLog.v("In showDetailedSuggestions of MainActivity");
        if (isFragmentActive) {
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
        if (isFragmentActive) {
            chatFragment.cancelTests();
        }
    }


    @Override
    public void onUserMessageAvailable(String text, boolean sendMessageToExpert) {
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
        //Contacting expert
        ExpertChat expertChat = new ExpertChat("Expert help needed here", ExpertChat.MSG_TYPE_BOT_TEXT);
        expertChatService.pushUserChatMessage(expertChat, true);
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
            return;
        }

        expertChatIdsDisplayed.add(expertChat.getId());

        switch (expertChat.getMessageType()) {
            case ExpertChat.MSG_TYPE_EXPERT_TEXT:
                chatFragment.showExpertChatMessage(expertChat.getText());
                saveExpertChatStarted();
                break;
            case ExpertChat.MSG_TYPE_BOT_TEXT:
                chatFragment.showResponse(expertChat.getText());
                break;
            case ExpertChat.MSG_TYPE_USER_TEXT:
                chatFragment.addUserChat(expertChat.getText());
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
            message = "Our experts are current offline. You shall receive a response in about 12 hours.";
        } else {
            message = messagePrefix + " Less than " + Utils.timeSecondsToString(newEtaSeconds);
        }
        currentEtaSeconds = newEtaSeconds;
        expertIsPresent = isPresent;
        if (showInChat && isFragmentActive) {
            chatFragment.showStatus(message);
        }
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
        String notifSource = intent.getStringExtra(ExpertChatActivity.INTENT_NOTIF_SOURCE);
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

    private void pushUserChatMessage(String text, boolean shouldShowToExpert) {
        ExpertChat expertChat = new
                ExpertChat(text, ExpertChat.MSG_TYPE_USER_TEXT);
        expertChatService.pushUserChatMessage(expertChat, shouldShowToExpert);
    }

    private void pushBotChatMessage(String text) {
        ExpertChat expertChat = new
                ExpertChat(text, ExpertChat.MSG_TYPE_BOT_TEXT);
        expertChatService.pushBotChatMessage(expertChat);
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check 
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can analyze your wifi network.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_COARSE_LOCATION_REQUEST_CODE);
                    }
                });
                builder.show();
            }
        }
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
        fetchChatMessages();
        processIntent(getIntent());
        expertChatService.sendUserEnteredMetaMessage();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        dobbyAi.cleanup();
    }

    @Override
    protected void onStop() {
        expertChatService.sendUserLeftMetaMessage();
        expertChatService.disconnect();
        expertChatService.unregisterChatCallback();
        super.onStop();
    }

    @Override
    protected void onStart() {
        expertChatService.connect();
        super.onStart();
    }

    @Override
    public void observeBandwidth(BandwidthObserver observer) {
        if (isFragmentActive) {
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
        DobbyLog.v("MainActivity:onRecyclerViewReady Setting isFragmentActive to true");
        isFragmentActive = true;
    }

    @Override
    public void onFirstTimeCreated() {
        if (dobbyAi != null) {
            dobbyAi.sendWelcomeEvent();
        }
    }

    @Override
    public void onFragmentDetached() {
        DobbyLog.v("MainActivity:onFragmentDetached Setting isFragmentActive to false");
        isFragmentActive = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SPEECH_RECOGNITION_REQUEST_CODE) {
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> res = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String inSpeech = res.get(0);
                if (isFragmentActive) {
                    chatFragment.addSpokenText(inSpeech);
                }
                onUserQuery(inSpeech, false);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
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
