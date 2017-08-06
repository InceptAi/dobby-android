package com.inceptai.wifiexpert.dagger;


import com.inceptai.wifiexpert.DobbyApplication;
import com.inceptai.wifiexpert.DobbyThreadPool;
import com.inceptai.wifiexpert.analytics.DobbyAnalytics;
import com.inceptai.wifiexpert.eventbus.DobbyEventBus;
import com.inceptai.wifiexpert.expert.ExpertChatService;

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
    public DobbyThreadPool providesThreadPool() {
        return new DobbyThreadPool();
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
}
