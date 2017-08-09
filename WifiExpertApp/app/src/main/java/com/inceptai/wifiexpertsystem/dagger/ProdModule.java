package com.inceptai.wifiexpertsystem.dagger;


import com.inceptai.wifiexpertsystem.DobbyApplication;
import com.inceptai.wifiexpertsystem.DobbyThreadPool;
import com.inceptai.wifiexpertsystem.analytics.DobbyAnalytics;
import com.inceptai.wifiexpertsystem.expert.ExpertChatService;

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
                                                       DobbyThreadPool dobbyThreadPool) {
        ExpertChatService expertChatService = new ExpertChatService(
                application.getUserUuid(),
                application.getApplicationContext(),
                dobbyThreadPool.getExecutorService(),
                dobbyAnalytics);
        application.getProdComponent().inject(expertChatService);
        return expertChatService;
    }
}
