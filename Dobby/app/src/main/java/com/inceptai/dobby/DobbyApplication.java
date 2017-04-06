package com.inceptai.dobby;

import android.app.Application;

import com.inceptai.dobby.dagger.DaggerProdComponent;
import com.inceptai.dobby.dagger.ProdComponent;
import com.inceptai.dobby.dagger.ProdModule;

/**
 * Created by arunesh on 3/28/17.
 */

public class DobbyApplication extends Application {
    public static final String TAG = "Dobby";
    private ProdComponent prodComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        prodComponent = DaggerProdComponent.builder().prodModule(new ProdModule(this)).build();
    }

    ProdComponent getProdComponent() {
        return prodComponent;
    }
}
