package com.inceptai.dobby.ui;

import android.app.Activity;
import android.os.SystemClock;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingPolicies;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;

import com.inceptai.dobby.R;
import com.inceptai.dobby.testutils.ElapsedTimeIdlingResource;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.TimeUnit;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

/**
 * Basic tests showcasing simple view matchers and actions like {@link ViewMatchers#withId},
 * {@link ViewActions#click} and {@link ViewActions#typeText}.
 * <p>
 * Note that there is no need to tell Espresso that a view is in a different {@link Activity}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class CheckMainScreenWifiDocTest {
    private final int BW_WAITING_TIME_MS = 21000; // ~21 secs
    private final int SUGGESTION_WAITING_TIME_AFTER_BW_MS = 7000; // ~6 secs
    private final int BOTTOM_DRAWER_WAITING_TIME_MS = 1000; // ~200 ms
    private final String FETCHING_CONFIG_MESSAGE = "Fetching server configuration ...";
    private final String STATUS_TITLE = "Status";
    private final String SUGGESTIONS_TITLE = "Suggestions";
    private final String DISMISS_TITLE = "DISMISS";
    private final String CANCEL_TITLE = "CANCEL";
    private final String MORE_TITLE = "MORE";
    private final String STATUS_RUNNING_TESTS_MESSAGE = "Running tests..";
    private final String STATUS_IDLE_MESSAGE = "Ready to run tests.";



    private IdlingResource waitFor(long waitMs) {
        // Make sure Espresso does not time out
        IdlingPolicies.setMasterPolicyTimeout(waitMs * 4, TimeUnit.MILLISECONDS);
        IdlingPolicies.setIdlingResourceTimeout(waitMs * 4, TimeUnit.MILLISECONDS);
        // Now we wait
        return new ElapsedTimeIdlingResource(waitMs);
    }

    private void cleanup(IdlingResource idlingResource) {
        // Clean up
        Espresso.unregisterIdlingResources(idlingResource);
    }

    @Before
    public void resetTimeout() {
        //IdlingPolicies.setMasterPolicyTimeout(60, TimeUnit.SECONDS);
        //IdlingPolicies.setIdlingResourceTimeout(60, TimeUnit.SECONDS);
    }

    /**
     * A JUnit {@link Rule @Rule} to launch your activity under test. This is a replacement
     * for {@link ActivityInstrumentationTestCase2}.
     * <p>
     * Rules are interceptors which are executed for each test method and will run before
     * any of your setup code in the {@link Before @Before} method.
     * <p>
     * {@link ActivityTestRule} will create and launch of the activity for you and also expose
     * the activity under test. To get a reference to the activity you can use
     * the {@link ActivityTestRule#getActivity()} method.
     */
    @Rule
    public ActivityTestRule<WifiDocActivity> mActivityRule = new ActivityTestRule<>(
            WifiDocActivity.class);


    @Test
    public void bwTestDefaultTest() {
        //Before the test, upload and download matches 0.0
        onView(allOf(withParent(withId(R.id.cg_download)), withId(R.id.gauge_tv))).check(matches(withText(WifiDocMainFragment.ZERO_POINT_ZERO)));
        onView(allOf(withParent(withId(R.id.cg_upload)), withId(R.id.gauge_tv))).check(matches(withText(WifiDocMainFragment.ZERO_POINT_ZERO)));

        //Before the tests are run -- status card view is displayed with text "Ready to run tests."
        onView(withId(R.id.status_cardview)).check(matches(isDisplayed()));
        onView(withId(R.id.status_tv)).check(matches(withText(STATUS_IDLE_MESSAGE)));

        //Click the run tests button
        onView(withId(R.id.main_fab_button)).perform(click());

        //Check that the status card view text changes
        onView(withId(R.id.status_tv)).check(matches(withText(STATUS_RUNNING_TESTS_MESSAGE)));

        SystemClock.sleep(BOTTOM_DRAWER_WAITING_TIME_MS);

        //Check that the status dialog box comes up
        onView(withId(R.id.bottom_dialog_inc)).check(matches(isDisplayed()));
        onView(withId(R.id.bottomDialog_icon)).check(matches(isDisplayed()));
        onView(withId(R.id.bottomDialog_cancel)).check(matches(isDisplayed()));
        onView(withId(R.id.bottomDialog_cancel)).check(matches(withText(CANCEL_TITLE)));
        onView(withId(R.id.bottomDialog_ok)).check(matches(not(isDisplayed())));
        onView(withId(R.id.bottomDialog_content)).check(matches(isDisplayed()));

        //Check that the status dialog box shows fetching config
        //Either starts with or ends with
        //onView(withId(R.id.bottomDialog_content)).check(matches(withText(anyOf(startsWith(FETCHING_CONFIG_MESSAGE), endsWith(FETCHING_CONFIG_MESSAGE)))));
        onView(withId(R.id.bottomDialog_content)).check(matches(withText(containsString(FETCHING_CONFIG_MESSAGE))));
        onView(withId(R.id.bottomDialog_title)).check(matches(withText(STATUS_TITLE)));

        // Now we wait
        SystemClock.sleep(BW_WAITING_TIME_MS);

        //Check that the status dialog box shows running download
        onView(allOf(withParent(withId(R.id.cg_download)), withId(R.id.gauge_tv))).check(matches(withText(not(containsString(WifiDocMainFragment.ZERO_POINT_ZERO)))));

        //Check that the status dialog box shows running upload
        onView(allOf(withParent(withId(R.id.cg_upload)), withId(R.id.gauge_tv))).check(matches(withText(not(containsString(WifiDocMainFragment.ZERO_POINT_ZERO)))));

        SystemClock.sleep(SUGGESTION_WAITING_TIME_AFTER_BW_MS);

        //Finally when bw tests are done -- check that wifi card, ping card are visible
        onView(withId(R.id.ping_cardview)).check(matches(isDisplayed()));
        onView(withId(R.id.net_cardview)).check(matches(isDisplayed()));
        onView(withId(R.id.status_cardview)).check(matches(not(isDisplayed())));

        //When suggestions are available -- make sure the more button is available and bottom dialog title changes to "Suggestions"
        onView(withId(R.id.bottomDialog_title)).check(matches(withText(SUGGESTIONS_TITLE)));
        onView(withId(R.id.bottomDialog_cancel)).check(matches(withText(DISMISS_TITLE)));
        onView(withId(R.id.bottomDialog_ok)).check(matches(withText(MORE_TITLE)));
        onView(withId(R.id.bottomDialog_ok)).check(matches(isDisplayed()));
    }
}