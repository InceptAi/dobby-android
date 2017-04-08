package com.inceptai.dobby.dagger;

import com.inceptai.dobby.MainActivity;
import com.inceptai.dobby.NetworkLayer;
import com.inceptai.dobby.ai.DobbyAi;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Production dagger component.
 */
@Singleton
@Component(modules={ProdModule.class})
public interface ProdComponent {

    void inject(MainActivity mainActivity);

    void inject(DobbyAi dobbyAi);

    void inject(NetworkLayer networkLayer);
}
