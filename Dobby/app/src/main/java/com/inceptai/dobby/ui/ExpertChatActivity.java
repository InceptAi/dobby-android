package com.inceptai.dobby.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.inceptai.dobby.DobbyAnalytics;
import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.InteractionManager;
import com.inceptai.dobby.R;
import com.inceptai.dobby.ai.DataInterpreter;
import com.inceptai.dobby.ai.SuggestionCreator;
import com.inceptai.dobby.expert.ExpertChatService;
import com.inceptai.dobby.speedtest.BandwidthObserver;
import com.inceptai.dobby.utils.DobbyLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

public class ExpertChatActivity extends AppCompatActivity implements
        InteractionManager.InteractionCallback,
        ChatFragment.OnFragmentInteractionListener {
    private static final int SPEECH_RECOGNITION_REQUEST_CODE = 102;
    private InteractionManager interactionManager;

    @Inject
    DobbyAnalytics dobbyAnalytics;

    private ChatFragment chatFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((DobbyApplication) getApplication()).getProdComponent().inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifidoc_expert_chat);
        setupChatFragment();
        interactionManager = new InteractionManager(getApplicationContext(), this);
        if (interactionManager.isFirstChatAfterInstall()) {
            WifiDocDialogFragment fragment = WifiDocDialogFragment.forExpertOnBoarding();
            fragment.show(getSupportFragmentManager(), "Wifi Expert Chat");
            dobbyAnalytics.chatActivityEnteredFirstTime();
        }
    }


    //Activity overrides
    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        interactionManager.onUserEnteredChat();
        processIntent(getIntent());
    }


    @Override
    protected void onStop() {
        super.onStop();
        interactionManager.onUserExitChat();
    }

    @Override
    protected void onDestroy() {
        DobbyLog.v("MainActivity: onDestroy");
        super.onDestroy();
        interactionManager.cleanup();
    }



    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    //Interaction Manager callbacks

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
    public void observeBandwidth(BandwidthObserver observer) {
        if (chatFragment != null) {
            chatFragment.observeBandwidthNonUi(observer);
        }
    }

    //From chatFragment onInteractionListener
    @Override
    public void onUserQuery(String text, boolean isButtonActionText) {
        interactionManager.onUserQuery(text, isButtonActionText);
    }

    @Override
    public void onMicPressed() {
        listen();
    }

    @Override
    public void onRecyclerViewReady() {
        //Get the welcome message
        DobbyLog.v("MainActivity:onRecyclerViewReady");
    }

    @Override
    public void onFirstTimeResumed() {
        DobbyLog.v("MainActivity:onFirstTimeResumed");
        interactionManager.onFirstTimeEnteredChat();
    }

    @Override
    public void onFragmentGone() {
        DobbyLog.v("MainActivity:onFragmentGone Setting chatFragment to null");
        //Setting chat fragment to null
        chatFragment = null;
    }

    @Override
    public void onFragmentReady() {
        DobbyLog.v("MainActivity:onFragmentReady Setting chatFragment based on tag");
        //Setting chat fragment here
        chatFragment = getChatFragmentFromTag();
        //Don't do resume stuff for now
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

    private void processIntent(Intent intent) {
        String notifSource = intent.getStringExtra(ExpertChatService.INTENT_NOTIF_SOURCE);
        if (notifSource != null) {
            dobbyAnalytics.expertChatNotificationConsumed();
        }
    }

    //Private methods

    private void setupChatFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.placeholder_fl,
                new ChatFragment(), ChatFragment.FRAGMENT_TAG);
        //fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
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

    private void listen() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say something");

        try {
            startActivityForResult(i, SPEECH_RECOGNITION_REQUEST_CODE);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(ExpertChatActivity.this, "Your device doesn't support Speech Recognition", Toast.LENGTH_SHORT).show();
        }
    }
}
