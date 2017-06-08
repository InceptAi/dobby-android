package com.inceptai.expertchat;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import static com.inceptai.expertchat.Utils.EMPTY_STRING;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, UserSelectionFragment.OnUserSelected, GoogleApiClient.OnConnectionFailedListener {
    private static final String DEFAULT_FLAVOR = Utils.WIFIDOC_FLAVOR;
    private static final String DEFAULT_BUILD_TYPE = Utils.BUILD_TYPE_RELEASE;

    private String selectedUserId = EMPTY_STRING;
    private String selectedFlavor = DEFAULT_FLAVOR;
    private String selectedBuildType = DEFAULT_BUILD_TYPE;

    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private String mUsername;
    private String mPhotoUrl;
    private GoogleApiClient mGoogleApiClient;
    private DrawerLayout drawerLayout;
    private NavigationView  navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        if (mFirebaseUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(new Intent(this, SignInActivity.class));
            finish();
            return;
        } else {
            mUsername = mFirebaseUser.getDisplayName();
            if (mFirebaseUser.getPhotoUrl() != null) {
                mPhotoUrl = mFirebaseUser.getPhotoUrl().toString();
            }
        }

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build();

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        if (mUsername != null) {
            Snackbar.make(drawerLayout, "Welcome " + mUsername, Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        selectedFlavor = Utils.readSharedSetting(this, Utils.SELECTED_FLAVOR, DEFAULT_FLAVOR);
        selectedUserId = Utils.readSharedSetting(this, Utils.SELECTED_USER_UUID, EMPTY_STRING);
        selectedBuildType = Utils.readSharedSetting(this, Utils.SELECTED_BUILD_TYPE, DEFAULT_BUILD_TYPE);
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

    private Fragment setupFragment(Class fragmentClass, String tag, Bundle args) {
        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment existingFragment = fragmentManager.findFragmentByTag(tag);

        if (existingFragment == null) {
            try {
                existingFragment = (Fragment) fragmentClass.newInstance();
            } catch (Exception e) {
                Log.e(Utils.TAG, "Unable to create fragment: " + fragmentClass.getCanonicalName());
                return null;
            }
        }
        if (args != null) {
            existingFragment.setArguments(args);
        }
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.placeholder_fl,
                existingFragment, tag);
        //fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
        return existingFragment;
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

        if (id == R.id.nav_select_user) {
            showUserSelectionFragment(selectedUserId, selectedFlavor, selectedBuildType);
        } else if (id == R.id.nav_user_chat) {
            showChatFragment(selectedUserId, selectedFlavor, selectedBuildType);
        } else if (id == R.id.nav_settings) {
            showPreferenceFragment();
        } else if (id == R.id.nav_stats) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showChatFragment(String userId, String flavor, String buildType) {
        ChatFragment fragment = (ChatFragment) setupFragment(ChatFragment.class,
                ChatFragment.FRAGMENT_TAG, ChatFragment.getArgumentBundle(userId, flavor, buildType));
    }

    private void showUserSelectionFragment(String userId, String flavor, String buildType) {
        UserSelectionFragment fragment = (UserSelectionFragment) setupFragment(UserSelectionFragment.class,
                UserSelectionFragment.FRAGMENT_TAG, UserSelectionFragment.getArgumentBundle(userId, flavor, buildType));
    }

    private void showPreferenceFragment() {
        PreferenceFragment fragment = (PreferenceFragment) setupFragment(PreferenceFragment.class, PreferenceFragment.FRAGMENT_TAG, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!selectedFlavor.isEmpty()) {
            Utils.saveSharedSetting(this, Utils.SELECTED_FLAVOR, selectedFlavor);
        }
        if (!selectedUserId.isEmpty()) {
            Utils.saveSharedSetting(this, Utils.SELECTED_USER_UUID, selectedUserId);
        }
    }

    @Override
    public void onUserSelected(String userId) {
        selectedUserId = userId;
        showChatFragment(userId, selectedFlavor, selectedBuildType);
        navigationView.setCheckedItem(R.id.nav_user_chat);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(Utils.TAG, "onConnectionFailed:" + connectionResult);
        Snackbar.make(drawerLayout, "Unable to connect to Google Play services.", Snackbar.LENGTH_LONG).show();
    }
}
