package com.inceptai.dobby;

import com.inceptai.dobby.dagger.DaggerProdComponent;
import com.inceptai.dobby.dagger.ObjectRegistry;
import com.inceptai.dobby.dagger.ProdComponent;

/**
 * Created by arunesh on 5/10/17.
 */

public class TestApplication extends DobbyApplication {

    public static ProdComponent testComponent;

    @Override
    protected void setupDagger() {
        super.setupDagger();
        testComponent = prodComponent;
        prodComponent.inject(ObjectRegistry.get());
    }
}
