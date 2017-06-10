package com.inceptai.dobby.testutils;

/**
 * Created by vivek on 6/10/17.
 */

import android.app.Activity;
import android.app.Instrumentation;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitor;
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import android.support.test.runner.lifecycle.Stage;
import android.util.Log;

import com.google.common.base.Throwables;
import com.google.common.collect.Sets;

import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Make sure this gets added to the manifest of the application under test (typically a manifest in the debug variant).
 *
 *
 * @param <T>
 */
public class EspressoTestRule<T extends Activity> extends ActivityTestRule<T> {
    private static final String TAG = EspressoTestRule.class.getSimpleName();


    // Giving the device a full second to turn the screen on.  This should be a one time hit.
    private static final long SCREEN_ON_ATTEMPT_DELAY = 1000;
    private static final int MAX_SCREEN_ON_ATTEMPTS = 20;

    public interface UIRunner {
        void run();
    }

    public EspressoTestRule(Class<T> activityClass) {
        super(activityClass);
    }

    public EspressoTestRule(Class<T> activityClass, boolean initialTouchMode) {
        super(activityClass, initialTouchMode);
    }

    public EspressoTestRule(Class<T> activityClass, boolean initialTouchMode, boolean launchActivity) {
        super(activityClass, initialTouchMode, launchActivity);
    }

    @Override
    public Statement apply(Statement base, Description description) {
        try {
            return super.apply(base, description);
        } finally {
            closeAllActivities();
        }
    }

    // See https://code.google.com/p/android-test-kit/issues/detail?id=66
    private void closeAllActivities() {
        try {
            Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
            closeAllActivities(instrumentation);
        } catch (Exception ex) {
            Log.e(TAG, "Could not use close all activities", ex);
        }
    }

    private void closeAllActivities(Instrumentation instrumentation) throws Exception {
        final int NUMBER_OF_RETRIES = 100;
        int i = 0;
        while (closeActivity(instrumentation)) {
            if (i++ > NUMBER_OF_RETRIES) {
                throw new AssertionError("Limit of retries excesses");
            }
            Thread.sleep(200);
        }
    }

    private boolean closeActivity(Instrumentation instrumentation) throws Exception {
        final Boolean activityClosed = callOnMainSync(instrumentation, new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                final Set<Activity> activities = getActivitiesInStages(Stage.RESUMED,
                        Stage.STARTED, Stage.PAUSED, Stage.STOPPED, Stage.CREATED);
                activities.removeAll(getActivitiesInStages(Stage.DESTROYED));
                if (activities.size() > 0) {
                    final Activity activity = activities.iterator().next();
                    activity.finish();
                    return true;
                } else {
                    return false;
                }
            }
        });
        if (activityClosed) {
            instrumentation.waitForIdleSync();
        }
        return activityClosed;
    }

    private <X> X callOnMainSync(Instrumentation instrumentation, final Callable<X> callable) throws Exception {
        final AtomicReference<X> retAtomic = new AtomicReference<>();
        final AtomicReference<Throwable> exceptionAtomic = new AtomicReference<>();
        instrumentation.runOnMainSync(new Runnable() {
            @Override
            public void run() {
                try {
                    retAtomic.set(callable.call());
                } catch (Throwable e) {
                    exceptionAtomic.set(e);
                }
            }
        });
        final Throwable exception = exceptionAtomic.get();
        if (exception != null) {
            Throwables.propagateIfInstanceOf(exception, Exception.class);
            Throwables.propagate(exception);
        }
        return retAtomic.get();
    }

    public static Set<Activity> getActivitiesInStages(Stage... stages) {
        final Set<Activity> activities = Sets.newHashSet();
        final ActivityLifecycleMonitor instance = ActivityLifecycleMonitorRegistry.getInstance();
        for (Stage stage : stages) {
            final Collection<Activity> activitiesInStage = instance.getActivitiesInStage(stage);
            if (activitiesInStage != null) {
                activities.addAll(activitiesInStage);
            }
        }
        return activities;
    }
}