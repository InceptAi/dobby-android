package com.inceptai.dobby.ui;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.NetworkLayer;
import com.inceptai.dobby.R;
import com.inceptai.dobby.ai.Action;
import com.inceptai.dobby.ai.DobbyAi;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.utils.DobbyLog;
import com.inceptai.dobby.utils.Utils;

import javax.inject.Inject;

import static com.inceptai.dobby.DobbyApplication.TAG;

public class WifiDocActivity extends AppCompatActivity implements WifiDocMainFragment.OnFragmentInteractionListener {

    private WifiDocMainFragment mainFragment;
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((DobbyApplication) getApplication()).getProdComponent().inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_doc);
        setupMainFragment();
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
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
        mainFragment = (WifiDocMainFragment) existingFragment;
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public void onMainButtonClick() {
        threadpool.submit(new Runnable() {
            @Override
            public void run() {
                dobbyAi.takeAction(new Action(Utils.EMPTY_STRING,
                        Action.ActionType.ACTION_TYPE_DIAGNOSE_SLOW_INTERNET));
            }
        });
    }

    public DobbyEventBus getEventBus() {
        return eventBus;
    }
}
