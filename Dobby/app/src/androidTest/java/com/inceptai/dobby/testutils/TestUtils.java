package com.inceptai.dobby.testutils;

import android.view.View;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Created by vivek on 6/3/17.
 */

public class TestUtils {
    private TestUtils() {
    }

    public static Matcher<View> withIndex(final Matcher<View> matcher, final int index) {
        return new TypeSafeMatcher<View>() {
            int currentIndex = 0;

            @Override
            public void describeTo(Description description) {
                description.appendText("with index: ");
                description.appendValue(index);
                matcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                if (matcher.matches(view)) {
                    return  currentIndex++ == index;
                }
                return false;
                //return matcher.matches(view) && currentIndex++ == index;
            }
        };
    }
}
