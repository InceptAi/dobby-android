package com.inceptai.dobby.dagger;

import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.NetworkLayer;
import com.inceptai.dobby.eventbus.DobbyEventBus;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by arunesh on 5/10/17.
 */
public class ObjectRegistry {

    private static ObjectRegistry INSTANCE = null;

    @Inject
    DobbyApplication application;
    @Inject
    DobbyThreadpool threadpool;
    @Inject
    NetworkLayer networkLayer;

    @Inject
    DobbyEventBus eventBus;

    ObjectRegistry() {
        INSTANCE = this;
    }

    public static synchronized ObjectRegistry get() {
        if (INSTANCE == null) {
            INSTANCE = new ObjectRegistry();
        }
        return INSTANCE;
    }

    public DobbyThreadpool getThreadpool() {
        return threadpool;
    }

    public DobbyApplication getApplication() {
        return application;
    }

    public NetworkLayer getNetworkLayer() {
        return networkLayer;
    }

    public DobbyEventBus getEventBus() {
        return eventBus;
    }
}
