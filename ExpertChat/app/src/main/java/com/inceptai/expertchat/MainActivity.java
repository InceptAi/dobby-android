package com.inceptai.expertchat;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.squareup.picasso.Picasso;

import static com.inceptai.expertchat.Utils.EMPTY_STRING;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, OnUserSelected, GoogleApiClient.OnConnectionFailedListener, ExpertChatService.OnExpertDataFetched {

    private static final String DEFAULT_FLAVOR = Utils.WIFIDOC_FLAVOR;
    private static final String DEFAULT_BUILD_TYPE = Utils.BUILD_TYPE_DEBUG;

    private static final String WIFIDOC_RECENTS_FRAGMENT_TAG = "WifiDocRecentsFragment";
    private static final String WIFIEXPERT_RECENTS_FRAGMENT_TAG = "WifiExpertRecentsFragment";

    public static final String FRAGMENT_ARG_FLAVOR_TYPE = "FlavorType";

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
    private ExpertChatService service;
    private boolean respondToNotification = false;
    private boolean onCreateCalled = false;

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
            service = ExpertChatService.fetchInstance(getApplicationContext());
            service.fetchAvatar(mFirebaseUser, this);
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
        checkForNotificationStart(getIntent());
        onCreateCalled = true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        this.setIntent(intent);
        checkForNotificationStart(intent);
    }

    private void checkForNotificationStart(Intent intent) {
        String notifUuid = intent.getStringExtra(Utils.NOTIFICATION_USER_UUID);
        if (notifUuid != null && !notifUuid.isEmpty()) {
            /// We have a user id from a notification.
            // TODO open chat for selected user ID.
            Utils.saveSharedSetting(this, Utils.SELECTED_USER_UUID, notifUuid);
            selectedUserId = notifUuid;
            Snackbar.make(drawerLayout, "User ID: " + notifUuid, Snackbar.LENGTH_SHORT).show();
            respondToNotification = true;
            service.setSelectedUserId(selectedUserId);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        service.setExpertDataFetchedCallback(this);
        selectedFlavor = Utils.readSharedSetting(this, Utils.SELECTED_FLAVOR, DEFAULT_FLAVOR);
        selectedUserId = Utils.readSharedSetting(this, Utils.SELECTED_USER_UUID, EMPTY_STRING);
        selectedBuildType = Utils.readSharedSetting(this, Utils.SELECTED_BUILD_TYPE, DEFAULT_BUILD_TYPE);
        if (service.isExpertOffline()) {
            Snackbar.make(drawerLayout, "YOU ARE OFFLINE.", Snackbar.LENGTH_LONG).show();
        } else {
            service.goOnline();
        }
        if (respondToNotification) {
            clearFrameLayout();
            showChatFragment();
            selectedUserId = service.getSelectedUserId();
            navigationView.setCheckedItem(R.id.nav_user_chat);
            respondToNotification = false;  // consume it.
            return;
        }
        if (onCreateCalled) {
            showWelcome(mPhotoUrl, mUsername);
            onCreateCalled = false;
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
    public void onExpertData(ExpertData expertData) {
        TextView avatarTv = (TextView) findViewById(R.id.profile_avatar);
        String avatar = expertData.getAvatar();
        // HACK: We need to check for avatarTv not being null since this can get invoked when the
        // Activity is backgrounded.
        if (avatar != null && avatarTv != null) {
            avatarTv.setText("Avatar: " + avatar);
        }
    }

    private Fragment setupFragment(Class fragmentClass, String tag) {
        // Insert the fragment by replacing any existing fragment
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment existingFragment = fragmentManager.findFragmentByTag(tag);
        Fragment newFragment = null;
        try {
            newFragment = (Fragment) fragmentClass.newInstance();
        } catch (Exception e) {
            Log.e(Utils.TAG, "Unable to create fragment: " + fragmentClass.getCanonicalName());
            return null;
        }
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        // fragmentTransaction.remove(existingFragment);
        fragmentTransaction.replace(R.id.placeholder_fl,
                newFragment, tag);
        fragmentTransaction.commit();
        return newFragment;
    }

    private void showWelcome(String photoUrl, String name) {
        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.placeholder_fl);
        frameLayout.removeAllViews();
        View welcome = getLayoutInflater().inflate(R.layout.welcome, frameLayout, false);
        frameLayout.addView(welcome);
        ImageView iv = (ImageView) welcome.findViewById(R.id.profile_image);
        TextView tv = (TextView) welcome.findViewById(R.id.profile_name);
        if (name != null) tv.setText(name);
        if (photoUrl != null) Picasso.with(this).load(photoUrl).into(iv);

        TextView avatarTv = (TextView) welcome.findViewById(R.id.profile_avatar);
        String avatar = service.getAvatar();
        if (avatar != null) {
            avatarTv.setText("Avatar: " + avatar);
        }
    }

    private void clearFrameLayout() {
        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.placeholder_fl);
        frameLayout.removeAllViews();
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

        clearFrameLayout();
        if (id == R.id.nav_wifidoc_recents) {
            showWifidocRecentsFragment();
        } else if (id == R.id.nav_wifiexpert_recents) {
            showWifiExpertRecentsFragment();
        } else if (id == R.id.nav_notif_recents) {
            showNotifRecentsFragment();
        } else if (id == R.id.nav_user_chat) {
            showChatFragment();
        } else if (id == R.id.nav_settings) {
            showPreferenceFragment();
        } else if (id == R.id.clear_recent) {
            clearRecents();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void clearRecents() {
        String wifidocRecents = Utils.WIFIDOC_RECENTS + "/release";
        String dobbyRecents = Utils.DOBBY_RECENTS + "/release";
        DatabaseReference wifiDocRecentRef = FirebaseDatabase.getInstance().getReference().child(wifidocRecents);
        DatabaseReference dobbyRecentRef = FirebaseDatabase.getInstance().getReference().child(dobbyRecents);
        wifiDocRecentRef.setValue(null);
        dobbyRecentRef.setValue(null);
    }

    private void showChatFragment() {
        ChatFragment fragment = (ChatFragment) setupFragment(ChatFragment.class, ChatFragment.FRAGMENT_TAG);
    }

    private void showUserSelectionFragment() {
        UserSelectionFragment fragment = (UserSelectionFragment) setupFragment(UserSelectionFragment.class, UserSelectionFragment.FRAGMENT_TAG);
    }

    private void showPreferenceFragment() {
        PreferenceFragment fragment = (PreferenceFragment) setupFragment(PreferenceFragment.class, PreferenceFragment.FRAGMENT_TAG);
    }

    private void showNotifRecentsFragment() {
        NotifRecentsFragment fragment = (NotifRecentsFragment) setupFragment(NotifRecentsFragment.class, NotifRecentsFragment.FRAGMENT_TAG);
    }

    private void showWifidocRecentsFragment() {
        Bundle args = new Bundle();
        args.putString(FRAGMENT_ARG_FLAVOR_TYPE, Utils.WIFIDOC_FLAVOR);
        RecentUsersFragment fragment = new RecentUsersFragment();
        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction().replace(R.id.placeholder_fl,
                fragment, WIFIDOC_RECENTS_FRAGMENT_TAG).commitAllowingStateLoss();
    }

    private void showWifiExpertRecentsFragment() {
        Bundle args = new Bundle();
        args.putString(FRAGMENT_ARG_FLAVOR_TYPE, Utils.DOBBY_FLAVOR);
        RecentUsersFragment fragment = new RecentUsersFragment();
        fragment.setArguments(args);
        getSupportFragmentManager().beginTransaction().replace(R.id.placeholder_fl,
                fragment, WIFIEXPERT_RECENTS_FRAGMENT_TAG).commitAllowingStateLoss();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!service.isExpertOffline()) {
            service.notOnline();
        }
        service.saveToSettings();
        service.clearExpertDataFetchedCallback();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // destroy fragments.
    }

    @Override
    public void onUserSelected(String userId) {
        selectedUserId = userId;
        Utils.saveSharedSetting(this, Utils.SELECTED_USER_UUID, selectedUserId);
        ExpertChatService.fetchInstance(this.getApplicationContext()).setSelectedUserId(selectedUserId);
        clearFrameLayout();
        showChatFragment();
        navigationView.setCheckedItem(R.id.nav_user_chat);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(Utils.TAG, "onConnectionFailed:" + connectionResult);
        Snackbar.make(drawerLayout, "Unable to connect to Google Play services.", Snackbar.LENGTH_LONG).show();
    }
}
