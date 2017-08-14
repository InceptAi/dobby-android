package com.inceptai.dobby;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;
import android.util.Log;

import com.inceptai.dobby.dagger.DaggerProdComponent;
import com.inceptai.dobby.dagger.ProdComponent;
import com.inceptai.dobby.dagger.ProdModule;
import com.inceptai.dobby.utils.EmulatorDetector;
import com.inceptai.dobby.utils.Utils;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by arunesh on 3/28/17.
 */

public class DobbyApplication extends Application {
    public static final String TAG = "Dobby";
    public static final String DOBBY_FLAVOR = "dobby";
    public static final String WIFIDOC_FLAVOR = "wifidoc";

    public static final AtomicBoolean USE_FAKES = new AtomicBoolean(false);
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
        prodComponent = DaggerProdComponent.builder().prodModule(new ProdModule(this)).build();
    }

    public ProdComponent getProdComponent() {
        return prodComponent;
    }

    public static boolean isDobbyFlavor() {
        return BuildConfig.FLAVOR.equals(DOBBY_FLAVOR);
    }

    public static boolean isWifiDocFlavor() {
        return BuildConfig.FLAVOR.equals(WIFIDOC_FLAVOR);
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
