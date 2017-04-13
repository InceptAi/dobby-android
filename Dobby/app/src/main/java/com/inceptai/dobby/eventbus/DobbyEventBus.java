package com.inceptai.dobby.eventbus;

import com.google.common.eventbus.EventBus;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by vivek on 4/11/17.
 */
@Singleton
public class DobbyEventBus {

    public final EventBus eventBus = new EventBus("dobby");

    @Inject
    public DobbyEventBus() {
    }

    public void registerListener(Object listener) {
        eventBus.register(listener);
    }

    public void unregisterListener(Object listener) {
        eventBus.unregister(listener);
    }

    public void postEvent(DobbyEvent event) {
        eventBus.post(event);
    }
}
