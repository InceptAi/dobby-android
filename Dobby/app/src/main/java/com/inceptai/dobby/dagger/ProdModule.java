package com.inceptai.dobby.dagger;

import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.NetworkLayer;
import com.inceptai.dobby.ai.DobbyAi;
import com.inceptai.dobby.database.DatabaseWriter;
import com.inceptai.dobby.eventbus.DobbyEventBus;

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


    //Commented here since we are creating a singleton right in DobbyEventBus.java
    /*
    @Singleton
    @Provides
    public DobbyEventBus providesEventBus() {
        return new DobbyEventBus();
    }
    */

    @Singleton
    @Provides
    public DobbyAi providesDobbyAi(DobbyApplication application,
                                   DobbyThreadpool threadpool,
                                   DatabaseWriter databaseWriter) {
        DobbyAi dobbyAi = new DobbyAi(application.getApplicationContext(),
                threadpool, databaseWriter);
        application.getProdComponent().inject(dobbyAi);
        return dobbyAi;
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

}
