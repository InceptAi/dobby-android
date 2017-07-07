package com.inceptai.dobby.dagger;

import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.NetworkLayer;
import com.inceptai.dobby.actions.ActionTaker;
import com.inceptai.dobby.analytics.DobbyAnalytics;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.expert.ExpertChatService;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Dagger module for production.
 */

@Module
public class ProdModule {
    DobbyApplication application;

    public ProdModule(DobbyApplication application) {
        this.application = application;
    }

    @Singleton
    @Provides
    public DobbyApplication providesApplication() {
        return application;
    }

    @Singleton
    @Provides
    public DobbyThreadpool providesThreadpool() {
        return new DobbyThreadpool();
    }

    @Singleton
    @Provides
    public NetworkLayer providesNetworkLayer(DobbyApplication application,
                                             DobbyThreadpool threadpool,
                                             DobbyEventBus eventBus) {
        NetworkLayer networkLayer = new NetworkLayer(application.getApplicationContext(),
                threadpool, eventBus);
        application.getProdComponent().inject(networkLayer);
        networkLayer.initialize();
        return networkLayer;
    }

    @Singleton
    @Provides
    public ExpertChatService providesExpertChatService(DobbyApplication application,
                                                       DobbyAnalytics dobbyAnalytics,
                                                       DobbyEventBus dobbyEventBus) {

        ExpertChatService expertChatService = new ExpertChatService(
                application,
                dobbyAnalytics,
                dobbyEventBus);
        application.getProdComponent().inject(expertChatService);
        return expertChatService;
    }

    @Singleton
    @Provides
    public ActionTaker providesActionTaker(DobbyApplication application,
                                           DobbyThreadpool dobbyThreadpool) {
        ActionTaker actionTaker = new ActionTaker(application.getApplicationContext(), dobbyThreadpool);
        application.getProdComponent().inject(actionTaker);
        return actionTaker;
    }
}
