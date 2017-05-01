package com.inceptai.dobby;

import android.app.Application;
import android.util.Log;

import com.inceptai.dobby.dagger.DaggerProdComponent;
import com.inceptai.dobby.dagger.ProdComponent;
import com.inceptai.dobby.dagger.ProdModule;
import com.inceptai.dobby.utils.Utils;

import java.util.EnumMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.inceptai.dobby.utils.Utils.EMPTY_STRING;

/**
 * Created by arunesh on 3/28/17.
 */

public class DobbyApplication extends Application {
    public static final String TAG = "Dobby";
    public static final String DOBBY_FLAVOR = "dobby";
    public static final String WIFIDOC_FLAVOR = "wifidoc";
    public static final String USER_UUID = "userUuid";
    public static final AtomicBoolean USE_FAKES = new AtomicBoolean(false);
    private final AtomicReference<String> userUuid = new AtomicReference<>();

    private ProdComponent prodComponent;

    @Override
    public void onCreate() {
        super.onCreate();

        prodComponent = DaggerProdComponent.builder().prodModule(new ProdModule(this)).build();
        Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
        Log.i("Dobby", "DobbyApplication: Old handler:" + handler.getClass().getCanonicalName());
        // Thread.setDefaultUncaughtExceptionHandler(new DobbyThreadpool.DobbyUncaughtExceptionHandler(handler));
    }

    private synchronized void fetchUuid() {
        String uuid = Utils.readSharedSetting(this, USER_UUID, EMPTY_STRING);
        if (EMPTY_STRING.equals(uuid)) {
            uuid = UUID.randomUUID().toString();
            Utils.saveSharedSetting(this, USER_UUID, uuid);
        }
        userUuid.set(uuid);
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
}
