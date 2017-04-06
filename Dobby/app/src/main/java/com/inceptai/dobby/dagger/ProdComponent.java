package com.inceptai.dobby.dagger;

import com.inceptai.dobby.MainActivity;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Production dagger component.
 */
@Singleton
@Component(modules={ProdModule.class})
public interface ProdComponent {

    void inject(MainActivity mainActivity);
}
