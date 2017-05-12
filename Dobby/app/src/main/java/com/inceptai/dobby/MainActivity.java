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
import com.inceptai.dobby.ai.DobbyAi;
import com.inceptai.dobby.ai.RtDataSource;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.speedtest.BandwidthObserver;
import com.inceptai.dobby.ui.ChatFragment;
import com.inceptai.dobby.ui.DebugFragment;
import com.inceptai.dobby.ui.FakeDataFragment;
import com.inceptai.dobby.ui.WifiFragment;
import com.inceptai.dobby.utils.DobbyLog;
import com.inceptai.dobby.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        DobbyAi.ResponseCallback,
        Handler.Callback, ChatFragment.OnFragmentInteractionListener {

    private static final int PERMISSION_COARSE_LOCATION_REQUEST_CODE = 101;
    private static final int SPEECH_RECOGNITION_REQUEST_CODE = 102;

    @Inject DobbyApplication dobbyApplication;
    @Inject DobbyThreadpool threadpool;
    @Inject DobbyAi dobbyAi;
    @Inject NetworkLayer networkLayer;
    @Inject DobbyEventBus eventBus;

    private Handler handler;
    private ChatFragment chatFragment;

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

        dobbyAi.setResponseCallback(this);
        handler = new Handler(this);

        setupChatFragment();
        requestPermissions();
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
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
        return existingFragment;
    }

    private void setupChatFragment() {
        chatFragment = new ChatFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.placeholder_fl,
                chatFragment, ChatFragment.FRAGMENT_TAG);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
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

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        item.setChecked(false);
        return true;
    }

    // From DobbyAi.ResponseCallback interface.
    @Override
    public void showResponse(String text) {
        chatFragment.showResponse(text);
    }

    // From DobbyAi.ResponseCallback interface.
    @Override
    public void showRtGraph(RtDataSource<Float, Integer> rtDataSource) {
        // chatFragment.showRtGraph(rtDataSource);
    }

    @Override
    public void onUserQuery(String text) {
        dobbyAi.sendQuery(text);
    }

    @Override
    public boolean handleMessage(Message msg) {
        return false;
    }

    private Fragment getFragmentByTag(String tag) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        return fragmentManager.findFragmentByTag(tag);
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
    protected void onDestroy() {
        super.onDestroy();
        dobbyAi.cleanup();
    }

    @Override
    public void observeBandwidth(BandwidthObserver observer) {
        chatFragment.observeBandwidthNonUi(observer);
    }

    @Override
    public void onMicPressed() {
        listen();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SPEECH_RECOGNITION_REQUEST_CODE) {
            if (resultCode == RESULT_OK && null != data) {
                ArrayList<String> res = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String inSpeech = res.get(0);
                chatFragment.addSpokenText(inSpeech);
                onUserQuery(inSpeech);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
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