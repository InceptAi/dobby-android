package com.inceptai.wifiexpertsystem.eventbus;

import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.inceptai.wifiexpertsystem.DobbyThreadPool;
import com.inceptai.wifiexpertsystem.utils.DobbyLog;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by vivek on 4/11/17.
 */
@Singleton
public class DobbyEventBus {

    public final EventBus eventBus = new EventBus("dobby");
    private ListeningScheduledExecutorService executorService;

    @Inject
    public DobbyEventBus(DobbyThreadPool dobbyThreadPool) {
        this.executorService = dobbyThreadPool.getExecutorServiceForEventBus();
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


    // Do a thread switch so that all post() and subscribe (or listen) calls happen on a single
    // thread to reduce the chances of a deadlock.
    public void postEvent(final DobbyEvent event) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                eventBus.post(event);
            }
        });
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
