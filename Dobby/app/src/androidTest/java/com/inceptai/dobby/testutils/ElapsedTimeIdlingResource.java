package com.inceptai.dobby.testutils;

import android.support.test.espresso.IdlingResource;

import com.inceptai.dobby.utils.DobbyLog;

/**
 * Created by vivek on 5/11/17.
 */

public class ElapsedTimeIdlingResource implements IdlingResource {
    private final long startTime;
    private final long waitingTime;
    private ResourceCallback resourceCallback;

    public ElapsedTimeIdlingResource(long waitingTime) {
        this.startTime = System.currentTimeMillis();
        this.waitingTime = waitingTime;
    }

    @Override
    public String getName() {
        return ElapsedTimeIdlingResource.class.getName() + ":" + waitingTime;
    }

    @Override
    public boolean isIdleNow() {
        long elapsed = System.currentTimeMillis() - startTime;
        boolean idle = (elapsed >= waitingTime);
        DobbyLog.v("In " + getName() + " with elapsed " + elapsed);
        if (idle) {
            DobbyLog.v("In " + getName() + " transitioning to idle");
            resourceCallback.onTransitionToIdle();
        }
        return idle;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
        this.resourceCallback = resourceCallback;
    }
}

