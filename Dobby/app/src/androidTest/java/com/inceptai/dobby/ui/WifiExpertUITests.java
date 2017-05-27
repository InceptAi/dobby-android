package com.inceptai.dobby.ui;


import android.app.Activity;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.inceptai.dobby.MainActivity;
import com.inceptai.dobby.R;
import com.inceptai.dobby.utils.Utils;
import com.squareup.spoon.Spoon;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class WifiExpertUITests {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    private Activity getActivity() {
        return mActivityTestRule.getActivity();
    }

    private void checkSlowInternetCheckWifiAndRunSpeedTestButtons () {
        ViewInteraction button = onView(
                allOf(withText("Slow internet"), childAtPosition(
                        allOf(ViewMatchers.withId(R.id.action_menu),
                                childAtPosition(
                                        withId(R.id.scrollview_buttons),
                                        0)),
                        0),
                        isDisplayed()));
        button.check(matches(isDisplayed()));

        ViewInteraction button2 = onView(
                allOf(withText("Run speed test"), childAtPosition(
                        allOf(withId(R.id.action_menu),
                                childAtPosition(
                                        withId(R.id.scrollview_buttons),
                                        0)),
                        1),
                        isDisplayed()));
        button2.check(matches(isDisplayed()));

        ViewInteraction button3 = onView(
                allOf(withText("Check wifi"), childAtPosition(
                        allOf(withId(R.id.action_menu),
                                childAtPosition(
                                        withId(R.id.scrollview_buttons),
                                        0)),
                        2),
                        isDisplayed()));
        button3.check(matches(isDisplayed()));
    }

    private void checkIdleState(boolean initialAppLaunch) {
        Spoon.screenshot(getActivity(), "idle_state");
        checkSlowInternetCheckWifiAndRunSpeedTestButtons();

        if (initialAppLaunch) {
            ViewInteraction textView = onView(
                    allOf(withId(R.id.dobbyTextTv), withText(containsString("run tests")),
                            childAtPosition(
                                    allOf(withId(R.id.dobbyChatLayout),
                                            childAtPosition(
                                                    withId(R.id.chatRv),
                                                    0)),
                                    1),
                            isDisplayed()));
            textView.check(matches(withText(containsString("run tests"))));

        }
    }

    private void checkShowingWifiAnalysisState() {
        Spoon.screenshot(getActivity(), "wifi_analysis_state");

        ViewInteraction frameLayout = onView(withId(R.id.net_cardview));
        frameLayout.check(matches(isDisplayed()));

        ViewInteraction textView2 = onView(allOf(withId(R.id.dobbyTextTv), withText(containsString("You are"))));
        textView2.check(matches(withText(containsString("You are connected and online via wifi network:"))));
        textView2.check(matches(isDisplayed()));

        ViewInteraction button5 = onView(
                allOf(withText("Yes"), childAtPosition(
                        allOf(withId(R.id.action_menu),
                                childAtPosition(
                                        withId(R.id.scrollview_buttons),
                                        0)),
                        0),
                        isDisplayed()));
        button5.check(matches(isDisplayed()));

        ViewInteraction button6 = onView(
                allOf(withText("No"), childAtPosition(
                        allOf(withId(R.id.action_menu),
                                childAtPosition(
                                        withId(R.id.scrollview_buttons),
                                        0)),
                        1),
                        isDisplayed()));
        button6.check(matches(isDisplayed()));
    }

    private void checkRunningDownloadSpeedTestState() {
        Spoon.screenshot(getActivity(), "running_download_test_state");
        onView(allOf(withParent(withId(R.id.cg_download)), withId(R.id.gauge_tv))).check(matches(withText(not(containsString(Utils.ZERO_POINT_ZERO)))));
        onView(allOf(withParent(withId(R.id.cg_upload)), withId(R.id.gauge_tv))).check(matches(withText(containsString(Utils.ZERO_POINT_ZERO))));

        ViewInteraction linearLayout = onView(
                allOf(withId(R.id.bw_gauge_ll),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.placeholder_fl),
                                        0),
                                0),
                        isDisplayed()));
        linearLayout.check(matches(isDisplayed()));
        //cancel button
        ViewInteraction button3 = onView(
                allOf(withText("Cancel"), childAtPosition(
                        allOf(withId(R.id.action_menu),
                                childAtPosition(
                                        withId(R.id.scrollview_buttons),
                                        0)),
                        0),
                        isDisplayed()));
        button3.check(matches(isDisplayed()));
    }

    private void checkCancelledTestState() {
        Spoon.screenshot(getActivity(), "cancelled_state");

        ViewInteraction textView4 = onView(
                allOf(withId(R.id.user_text_tv), withText("Cancel"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.user_chat_layout),
                                        1),
                                0),
                        isDisplayed()));
        textView4.check(matches(withText("Cancel")));

        ViewInteraction textView3 = onView(
                allOf(withId(R.id.dobbyTextTv), withText("Ok sure. I am cancelling the tests. You can run it anytime by simply typing \"run tests\""),
                        childAtPosition(
                                allOf(withId(R.id.dobbyChatLayout),
                                        childAtPosition(
                                                withId(R.id.chatRv),
                                                5)),
                                1),
                        isDisplayed()));
        textView3.check(matches(withText("Ok sure. I am cancelling the tests. You can run it anytime by simply typing \"run tests\"")));


        checkSlowInternetCheckWifiAndRunSpeedTestButtons();
    }

    private void checkShowingSpeedTestAnalysisState() {
        Spoon.screenshot(getActivity(), "speed_test_analysis_state");

        ViewInteraction frameLayout2 = onView(withId(R.id.bandwidth_results_cardview));
        frameLayout2.check(matches(isDisplayed()));

        ViewInteraction textView3 = onView(allOf(withId(R.id.dobbyTextTv), withText(containsString("?"))));
        textView3.check(matches(isDisplayed()));

        ViewInteraction button9 = onView(
                allOf(withText("Yes"), childAtPosition(
                        allOf(withId(R.id.action_menu),
                                childAtPosition(
                                        withId(R.id.scrollview_buttons),
                                        0)),
                        0),
                        isDisplayed()));
        button9.check(matches(isDisplayed()));

        ViewInteraction button10 = onView(
                allOf(withText("No"), childAtPosition(
                        allOf(withId(R.id.action_menu),
                                childAtPosition(
                                        withId(R.id.scrollview_buttons),
                                        0)),
                        1),
                        isDisplayed()));
        button10.check(matches(isDisplayed()));
    }

    private void checkShowingDetailedSuggestionState () {
        Spoon.screenshot(getActivity(), "detailed_suggestion_state");
        checkSlowInternetCheckWifiAndRunSpeedTestButtons();
    }

    private void safeSleep(int sleepTimeMs) {
        try {
            Thread.sleep(sleepTimeMs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void checkDecliningFullSpeedTestState () {
        Spoon.screenshot(getActivity(), "declining_speed_test_state");
        ViewInteraction textView2 = onView(
                allOf(withId(R.id.user_text_tv), withText("No"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.user_chat_layout),
                                        1),
                                0),
                        isDisplayed()));
        textView2.check(matches(withText("No")));
        checkSlowInternetCheckWifiAndRunSpeedTestButtons();

        ViewInteraction textView = onView(
                allOf(withId(R.id.dobbyTextTv), withText("Ok no worries. Let me know if you want to run tests at any time."),
                        childAtPosition(
                                allOf(withId(R.id.dobbyChatLayout),
                                        childAtPosition(
                                                withId(R.id.chatRv),
                                                4)),
                                1),
                        isDisplayed()));
        textView.check(matches(withText("Ok no worries. Let me know if you want to run tests at any time.")));

        checkSlowInternetCheckWifiAndRunSpeedTestButtons();
    }

    private void checkOneFullRun (boolean initialAppLaunch) {

        checkIdleState(initialAppLaunch);

        //Press slow internet
        ViewInteraction button4 = onView(
                allOf(withText("Slow internet"),
                        withParent(allOf(withId(R.id.action_menu),
                                withParent(withId(R.id.scrollview_buttons))))));
        button4.perform(scrollTo(), click());

        safeSleep(2000);

        checkShowingWifiAnalysisState();

        //Press yes for running full tests
        ViewInteraction button7 = onView(
                allOf(withText("Yes"),
                        withParent(allOf(withId(R.id.action_menu),
                                withParent(withId(R.id.scrollview_buttons))))));
        button7.perform(scrollTo(), click());

        safeSleep(5000);

        checkRunningDownloadSpeedTestState();

        safeSleep(30000);

        checkShowingSpeedTestAnalysisState();

        //Click yes button
        ViewInteraction button11 = onView(
                allOf(withText("Yes"),
                        withParent(allOf(withId(R.id.action_menu),
                                withParent(withId(R.id.scrollview_buttons))))));
        button11.perform(scrollTo(), click());

        safeSleep(2000);

        checkShowingDetailedSuggestionState();
    }

    @Test
    public void wifiExpertRunOnceTest() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html

        safeSleep(10000);
        checkOneFullRun(true);
    }

    @Test
    public void wifiExpertRunCancelRun() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html

        safeSleep(10000);

        checkIdleState(true);

        //Press slow internet
        ViewInteraction slowInternetButton = onView(
                allOf(withText("Slow internet"),
                        withParent(allOf(withId(R.id.action_menu),
                                withParent(withId(R.id.scrollview_buttons))))));
        slowInternetButton.perform(scrollTo(), click());

        safeSleep(2000);

        checkShowingWifiAnalysisState();

        //Press yes for running full tests
        ViewInteraction button7 = onView(
                allOf(withText("Yes"),
                        withParent(allOf(withId(R.id.action_menu),
                                withParent(withId(R.id.scrollview_buttons))))));
        button7.perform(scrollTo(), click());

        safeSleep(5000);

        checkRunningDownloadSpeedTestState();

        ViewInteraction cancelButton = onView(
                allOf(withText("Cancel"),
                        withParent(allOf(withId(R.id.action_menu),
                                withParent(withId(R.id.scrollview_buttons))))));
        cancelButton.perform(scrollTo(), click());

        safeSleep(2000);

        checkCancelledTestState();

        checkOneFullRun(false);
    }

    @Test
    public void wifiExpertRunBackToBack() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html

        safeSleep(10000);

        checkOneFullRun(true);

        checkOneFullRun(false);
    }

    @Test
    public void wifiExpertRunWifiAnalysisOnly() {
        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html

        safeSleep(5000);

        checkIdleState(true);

        //Press slow internet
        ViewInteraction button4 = onView(
                allOf(withText("Slow internet"),
                        withParent(allOf(withId(R.id.action_menu),
                                withParent(withId(R.id.scrollview_buttons))))));
        button4.perform(scrollTo(), click());

        safeSleep(2000);

        checkShowingWifiAnalysisState();

        //Press yes for running full tests
        ViewInteraction button7 = onView(
                allOf(withText("No"),
                        withParent(allOf(withId(R.id.action_menu),
                                withParent(withId(R.id.scrollview_buttons))))));
        button7.perform(scrollTo(), click());

        safeSleep(1000);

        checkDecliningFullSpeedTestState();
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
