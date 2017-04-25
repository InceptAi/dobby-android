package com.inceptai.dobby.eventbus;

import com.google.common.eventbus.EventBus;
import com.inceptai.dobby.utils.DobbyLog;

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
        try {
            eventBus.unregister(listener);
        }catch (IllegalArgumentException e) {
            DobbyLog.w("This Object was not registered, so can't unregister from event bus" + listener.toString());
        }
    }

    public void postEvent(DobbyEvent event) {
        eventBus.post(event);
    }

    /**
     * For empty payload.
     * @param eventType
     */
    public void postEvent(@DobbyEvent.EventType int eventType) {
        postEvent(new DobbyEvent(eventType));
    }

    public void postEvent(@DobbyEvent.EventType int eventType, Object payload) {
        postEvent(new DobbyEvent(eventType, payload));
    }
}
