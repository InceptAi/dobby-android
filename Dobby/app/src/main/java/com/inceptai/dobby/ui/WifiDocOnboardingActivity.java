package com.inceptai.dobby.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.R;
import com.inceptai.dobby.analytics.DobbyAnalytics;
import com.inceptai.dobby.utils.Utils;

import javax.inject.Inject;

public class WifiDocOnboardingActivity extends AppCompatActivity {

    private static final int MAX_PAGE_NUM = 2;
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    private ImageButton nextButton;
    private Button finishButton, skipButton;
    private ImageView zeroIv, oneIv, twoIv;
    private ImageView[] indicators;
    private int page = 0;  // To track page.

    @Inject
    DobbyAnalytics dobbyAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((DobbyApplication) getApplication()).getProdComponent().inject(this);
        super.onCreate(savedInstanceState);
        if (isOnboardingDone()) {
            finishAndStartWifiDoc();
        }
        setContentView(R.layout.activity_wifi_doc_onboarding);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        nextButton = (ImageButton) findViewById(R.id.intro_btn_next);
        skipButton = (Button) findViewById(R.id.intro_btn_skip);
        finishButton = (Button) findViewById(R.id.intro_btn_finish);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                page = position;
                updateIndicators(position);
                nextButton.setVisibility(position == MAX_PAGE_NUM ? View.GONE : View.VISIBLE);
                finishButton.setVisibility(position == MAX_PAGE_NUM ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        zeroIv = (ImageView) findViewById(R.id.intro_indicator_0);
        oneIv = (ImageView) findViewById(R.id.intro_indicator_1);
        twoIv = (ImageView) findViewById(R.id.intro_indicator_2);
        indicators = new ImageView[]{zeroIv, oneIv, twoIv};
        updateIndicators(page);
        mViewPager.setCurrentItem(page);

        skipButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveOnboardingDone();
                finishAndStartWifiDoc();
                dobbyAnalytics.onBoardingSkipClicked();
            }
        });

        finishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveOnboardingDone();
                finishAndStartWifiDoc();
                dobbyAnalytics.onBoardingFinishClicked();
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (page < MAX_PAGE_NUM) {
                    page ++;
                    mViewPager.setCurrentItem(page, true);
                }
                dobbyAnalytics.onBoardingNextClicked();
            }
        });
        dobbyAnalytics.onBoardingShown();
    }

    void updateIndicators(int position) {
        for (int i = 0; i < indicators.length; i++) {
            indicators[i].setBackgroundResource(
                    i == position ? R.drawable.indicator_selected : R.drawable.indicator_unselected
            );
        }
    }

    private void saveOnboardingDone() {
        Utils.saveSharedSetting(WifiDocOnboardingActivity.this,
                WifiDocActivity.PREF_FIRST_TIME_USER, Utils.TRUE_STRING);
    }

    private boolean isOnboardingDone() {
        return Boolean.valueOf(Utils.readSharedSetting(WifiDocOnboardingActivity.this,
                WifiDocActivity.PREF_FIRST_TIME_USER, Utils.FALSE_STRING));
    }

    private void finishAndStartWifiDoc() {
        startActivity(new Intent(this, WifiDocActivity.class));
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_wifi_doc_onboarding, menu);
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

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        private static int[] layoutArray = new int[]{R.layout.fragment_onboarding_1,
                R.layout.fragment_onboarding_2, R.layout.fragment_onboarding_wifiservice};

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            int number = getArguments().getInt(ARG_SECTION_NUMBER);
            number = Math.min(number, 2);
            Log.i("Dobby", "Inflating number: " + number);
            View rootView = inflater.inflate(layoutArray[number], container, false);
            //TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            // textView.setText(getString(R.string.section_format, getArguments().getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "SECTION 1";
                case 1:
                    return "SECTION 2";
                case 2:
                    return "SECTION 3";
            }
            return null;
        }
    }

}
