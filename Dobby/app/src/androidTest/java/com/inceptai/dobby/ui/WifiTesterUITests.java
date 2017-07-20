package com.inceptai.dobby.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.os.Build;
import android.os.SystemClock;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.action.ViewActions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.inceptai.dobby.R;
import com.inceptai.dobby.utils.Utils;
import com.squareup.spoon.Spoon;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.InstrumentationRegistry.getTargetContext;
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
public class WifiTesterUITests {
    private final int BW_WAITING_TIME_MS = 21000; // ~21 secs
    private final int SUGGESTION_WAITING_TIME_AFTER_BW_MS = 7000; // ~6 secs
    private final int BOTTOM_DRAWER_WAITING_TIME_MS = 1000; // ~200 ms
    private final int WAITING_BETWEEN_BW_TESTS_MS = 1000; // 200 ms
    private final String FETCHING_CONFIG_MESSAGE = "Fetching server configuration";
    private final String STATUS_TITLE = "Status";
    private final String SUGGESTIONS_TITLE = "Suggestions";
    private final String DISMISS_TITLE = "DISMISS";
    private final String CANCEL_TITLE = "CANCEL";
    private final String MORE_TITLE = "MORE";
    private final String CONTACT_EXPERT = "CONTACT EXPERT";
    private final String STATUS_RUNNING_TESTS_MESSAGE = "Running tests..";
    private final String STATUS_IDLE_MESSAGE = "Ready to run tests.";
    private final int CANCEL_WAIT_MS = 5000; // ~500 ms
    private static final boolean ENABLE_SCREENSHOTS = true  ;

    private void captureScreenshot(String label) {
        if (ENABLE_SCREENSHOTS) {
            Spoon.screenshot(getActivity(), label);
        }
    }

    /*
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
    public void unlockScreen() {
        final WifiDocActivity activity = mActivityRule.getActivity();
        Runnable wakeUpDevice = new Runnable() {
            public void run() {
                activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        };
        activity.runOnUiThread(wakeUpDevice);
    }

    public void grantPhonePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getInstrumentation().getUiAutomation().executeShellCommand(
                    "pm grant " + getTargetContext().getPackageName()
                            + " android.permission.WRITE_EXTERNAL_STORAGE");
        }
    }




    @After
    public void tearDown() throws Exception {
        Log.d(TAG, "TEARDOWN");

        goBackN();

        //super.tearDown();
    }

    private void goBackN() {
        final int N = 10; // how many times to hit back button
        try {
            for (int i = 0; i < N; i++)
                Espresso.pressBack();
        } catch (NoActivityResumedException e) {
            Log.e(TAG, "Closed all activities", e);
        }
    }


    @Before
    public void grantPhonePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getInstrumentation().getUiAutomation().executeShellCommand(
                    "pm grant " + getTargetContext().getPackageName()
                            + " android.permission.ACCESS_FINE_LOCATION");
        }
    }
*/
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

    private Activity getActivity() {
        return mActivityRule.getActivity();
    }

    private void checkIdleUIState() {
        //Before the test, upload and download matches 0.0
        onView(allOf(withParent(withId(R.id.cg_download)), withId(R.id.gauge_tv))).check(matches(withText(Utils.ZERO_POINT_ZERO)));
        onView(allOf(withParent(withId(R.id.cg_upload)), withId(R.id.gauge_tv))).check(matches(withText(Utils.ZERO_POINT_ZERO)));

        //Before the tests are run -- status card view is displayed with text "Ready to run tests."
        onView(withId(R.id.status_cardview)).check(matches(isDisplayed()));
        onView(withId(R.id.status_tv)).check(matches(withText(STATUS_IDLE_MESSAGE)));
        onView(withId(R.id.bottom_dialog_inc)).check(matches(not(isDisplayed())));

        //Check Contact expert button exists
        ViewInteraction frameLayout2 = onView(withId(R.id.contact_expert_fl));
        frameLayout2.check(matches(isDisplayed()));

        //Check Run button exists
        ViewInteraction textViewRunTests = onView((withText("RUN TESTS")));
        textViewRunTests.check(matches(isDisplayed()));

        ViewInteraction textViewShare = onView((withText("Share")));
        textViewShare.check(matches(isDisplayed()));
    }

    private void checkRunningUIState() {
        //Check that the status card view text changes
        onView(withId(R.id.status_tv)).check(matches(withText(STATUS_RUNNING_TESTS_MESSAGE)));

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
    }

    private void checkBWTestFinishedState() {
        //Check that the status dialog box shows running download
        onView(allOf(withParent(withId(R.id.cg_download)), withId(R.id.gauge_tv))).check(matches(withText(not(containsString(Utils.ZERO_POINT_ZERO)))));

        //Check that the status dialog box shows running upload
        onView(allOf(withParent(withId(R.id.cg_upload)), withId(R.id.gauge_tv))).check(matches(withText(not(containsString(Utils.ZERO_POINT_ZERO)))));
    }

    private void checkSuggestionsAvailableState() {
        //Finally when bw tests are done -- check that wifi card, ping card are visible
        onView(withId(R.id.ping_cardview)).check(matches(isDisplayed()));
        onView(withId(R.id.net_cardview)).check(matches(isDisplayed()));
        onView(withId(R.id.status_cardview)).check(matches(not(isDisplayed())));

        //When suggestions are available -- make sure the more button is available and bottom dialog title changes to "Suggestions"
        onView(withId(R.id.bottomDialog_title)).check(matches(withText(SUGGESTIONS_TITLE)));
        onView(withId(R.id.bottomDialog_details)).check(matches(withText(CONTACT_EXPERT)));
    }

    @Test
    public void bwTestDefaultTest() {
        Utils.safeSleep(5000);
        //clickNextOnLocationPermission();

        checkIdleUIState();
        captureScreenshot("initial_state");
        //Click the run tests button
        onView(withId(R.id.bottom_run_tests_fl)).perform(click());
        SystemClock.sleep(BOTTOM_DRAWER_WAITING_TIME_MS);
        //Check that the status card view text changes
        captureScreenshot("running_state");

        // Now we wait
        SystemClock.sleep(BW_WAITING_TIME_MS);
        captureScreenshot("bw_finished_state");
        checkBWTestFinishedState();
        SystemClock.sleep(SUGGESTION_WAITING_TIME_AFTER_BW_MS);
        captureScreenshot("suggestion_state");
        checkSuggestionsAvailableState();
        /* Normal test code... */

    }

    private void clickNextOnLocationPermission() {
        ViewInteraction frameLayout = onView(
                allOf(withId(R.id.bottom_next_fl), isDisplayed()));
        frameLayout.perform(click());
    }
    /*
    @Test
    public void bwTestCancelTest() {
        Utils.safeSleep(10000);

        Spoon.screenshot(getActivity(), "initial_state");

        checkIdleUIState();
        //Click the run tests button
        onView(withId(R.id.main_fab_button)).perform(click());

        SystemClock.sleep(BOTTOM_DRAWER_WAITING_TIME_MS);
        Spoon.screenshot(getActivity(), "running_state");

        checkRunningUIState();
        //Cancel the tests
        onView(withId(R.id.bottomDialog_cancel)).perform(click());
        // Now we wait
        SystemClock.sleep(CANCEL_WAIT_MS);

        Spoon.screenshot(getActivity(), "cancelled_state");
        checkIdleUIState();
    }

    @Test
    public void bwTestRerunAfterCancelTest() {
        Utils.safeSleep(10000);

        Spoon.screenshot(getActivity(), "initial_state");
        checkIdleUIState();

        //Click the run tests button
        onView(withId(R.id.main_fab_button)).perform(click());
        SystemClock.sleep(BOTTOM_DRAWER_WAITING_TIME_MS);

        Spoon.screenshot(getActivity(), "running_state");
        checkRunningUIState();

        //Cancel the tests
        onView(withId(R.id.bottomDialog_cancel)).perform(click());
        // Now we wait
        SystemClock.sleep(CANCEL_WAIT_MS);

        Spoon.screenshot(getActivity(), "cancelled_state");
        checkIdleUIState();

        //Run tests again
        onView(withId(R.id.main_fab_button)).perform(click());
        SystemClock.sleep(BOTTOM_DRAWER_WAITING_TIME_MS);

        //Check that the status card view text changes
        Spoon.screenshot(getActivity(), "run_after_cancel_state");
        checkRunningUIState();

        // Now we wait
        SystemClock.sleep(BW_WAITING_TIME_MS);

        Spoon.screenshot(getActivity(), "bw_test_finished_state");
        checkBWTestFinishedState();

        SystemClock.sleep(SUGGESTION_WAITING_TIME_AFTER_BW_MS);

        Spoon.screenshot(getActivity(), "suggestions_available_state");
        checkSuggestionsAvailableState();
    }
*/

    @Test
    public void bwTestRunBackToBackTest() {
        Utils.safeSleep(10000);

        captureScreenshot("first_initial_state");
        checkIdleUIState();
        //Click the run tests button
        onView(withId(R.id.bottom_run_tests_fl)).perform(click());
        SystemClock.sleep(BOTTOM_DRAWER_WAITING_TIME_MS);
        //Check that the status card view text changes

        captureScreenshot("first_running_state");
        //checkRunningUIState();

        // Now we wait
        SystemClock.sleep(BW_WAITING_TIME_MS);
        captureScreenshot("bw_test_finished_state");
        checkBWTestFinishedState();

        SystemClock.sleep(SUGGESTION_WAITING_TIME_AFTER_BW_MS);

        captureScreenshot("first_suggestion_available_state");
        checkSuggestionsAvailableState();

        //Second run -- first dismiss the suggestion
        onView(withId(R.id.bottomDialog_cancel)).perform(click());
        SystemClock.sleep(WAITING_BETWEEN_BW_TESTS_MS);
        captureScreenshot("ready_for_second_bw_test_state");

        //Then start the test again
        onView(withId(R.id.bottom_run_tests_fl)).perform(click());
        SystemClock.sleep(BOTTOM_DRAWER_WAITING_TIME_MS);

        //Check that the status card view text changes
        captureScreenshot("second_running_state");
        //checkRunningUIState();

        // Now we wait
        SystemClock.sleep(BW_WAITING_TIME_MS);

        captureScreenshot("second_finished_state");
        checkBWTestFinishedState();

        SystemClock.sleep(SUGGESTION_WAITING_TIME_AFTER_BW_MS);

        captureScreenshot("second_suggestion_available_state");
        checkSuggestionsAvailableState();
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Test
    public void animationScalesSetToZeroDuringTest() throws Exception {
        Utils.safeSleep(10000);
        boolean isSystemAnimationEnabled = Utils.areSystemAnimationsEnabled(getTargetContext());
        Assert.assertFalse(isSystemAnimationEnabled);
    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }

}