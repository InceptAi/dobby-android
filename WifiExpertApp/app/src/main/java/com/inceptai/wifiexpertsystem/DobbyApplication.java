package com.inceptai.wifiexpertsystem;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;
import android.util.Log;

import com.inceptai.wifiexpertsystem.dagger.DaggerProdComponent;
import com.inceptai.wifiexpertsystem.dagger.ProdComponent;
import com.inceptai.wifiexpertsystem.dagger.ProdModule;
import com.inceptai.wifiexpertsystem.utils.EmulatorDetector;
import com.inceptai.wifiexpertsystem.utils.Utils;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by arunesh on 3/28/17.
 */

public class DobbyApplication extends Application {
    public static final String TAG = "ExpertApp";

    private final AtomicReference<String> userUuid = new AtomicReference<>();

    protected ProdComponent prodComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        fetchUuid();
        setupDagger();
        Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
        Log.i("Dobby", "DobbyApplication: Old handler:" + handler.getClass().getCanonicalName());
        // Thread.setDefaultUncaughtExceptionHandler(new DobbyThreadpool.DobbyUncaughtExceptionHandler(handler));
        //ExpertChatService instance = ExpertChatService.fetchInstance(userUuid.get(), prodComponent);
    }

    private synchronized void fetchUuid() {
        String uuid = Utils.fetchUuid(this);
        userUuid.set(uuid);
    }

    // Can be overridden by child classes, such as for testing.
    protected void setupDagger() {
        prodComponent =  DaggerProdComponent.builder().prodModule(new ProdModule(this)).build();
    }

    public ProdComponent getProdComponent() {
        return prodComponent;
    }

    public String getUserUuid() {
        return userUuid.get();
    }

    public static String getAppVersion() {
        return BuildConfig.VERSION_NAME;
    }

    public String getPhoneInfo() {
        return Utils.getDeviceDetails();
    }

    public boolean isRunningOnEmulator() {
        return EmulatorDetector.isEmulator();
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
