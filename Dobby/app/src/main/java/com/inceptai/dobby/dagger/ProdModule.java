package com.inceptai.dobby.dagger;

import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.ai.DobbyAi;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.NetworkLayer;

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
    public DobbyAi providesDobbyAi(DobbyApplication application, DobbyThreadpool threadpool) {
        DobbyAi dobbyAi = new DobbyAi(application.getApplicationContext(), threadpool);
        application.getProdComponent().inject(dobbyAi);
        return dobbyAi;
    }

    @Singleton
    @Provides
    public NetworkLayer providesNetworkLayer(DobbyApplication application, DobbyThreadpool threadpool) {
        NetworkLayer networkLayer = new NetworkLayer(application.getApplicationContext(), threadpool);
        networkLayer.initialize();
        return networkLayer;
    }
}
