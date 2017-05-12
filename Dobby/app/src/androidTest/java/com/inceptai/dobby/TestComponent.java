package com.inceptai.dobby;

import com.inceptai.dobby.dagger.ProdComponent;
import com.inceptai.dobby.dagger.ProdModule;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Created by arunesh on 5/10/17.
 */
@Singleton
@Component(modules={ProdModule.class})
public interface TestComponent extends ProdComponent {

    void inject(BandwidthAnalyzerTest test);
}
