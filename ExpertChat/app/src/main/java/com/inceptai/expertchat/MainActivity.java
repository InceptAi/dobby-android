package com.inceptai.expertchat;

import android.content.Intent;
import android.os.Bundle;
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

import static com.inceptai.expertchat.Utils.EMPTY_STRING;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, UserSelectionFragment.OnUserSelected {

    private String selectedUserId = EMPTY_STRING;
    private String selectedFlavor = EMPTY_STRING;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        selectedFlavor = Utils.readSharedSetting(this, Utils.SELECTED_FLAVOR, EMPTY_STRING);
        selectedUserId = Utils.readSharedSetting(this, Utils.SELECTED_USER_UUID, EMPTY_STRING);
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
            UserSelectionFragment fragment = (UserSelectionFragment) setupFragment(UserSelectionFragment.class,
                    UserSelectionFragment.FRAGMENT_TAG, UserSelectionFragment.getArgumentBundle(selectedUserId));
        } else if (id == R.id.nav_user_chat) {
            showChatFragment(selectedUserId, selectedFlavor);
        } else if (id == R.id.nav_settings) {

        } else if (id == R.id.nav_stats) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void showChatFragment(String userId, String flavor) {
        ChatFragment fragment = (ChatFragment) setupFragment(ChatFragment.class,
                ChatFragment.FRAGMENT_TAG, ChatFragment.getArgumentBundle(userId, flavor));
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
        showChatFragment(userId, selectedFlavor);
    }
}
