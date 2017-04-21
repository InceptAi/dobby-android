package com.inceptai.dobby.fake;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.Nullable;

import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.connectivity.ConnectivityAnalyzer;


/**
 * Created by vivek on 4/12/17.
 */
public class FakeConnectivityAnalyzer extends ConnectivityAnalyzer {

    @WifiConnectivityMode public static int fakeWifiConnectivityMode = WifiConnectivityMode.UNKNOWN;

    private FakeConnectivityAnalyzer(Context context, ConnectivityManager connectivityManager,
                                     DobbyThreadpool threadpool, DobbyEventBus eventBus) {
        super(context, connectivityManager, threadpool, eventBus);
        wifiConnectivityMode = fakeWifiConnectivityMode;
    }

    /**
     * Factory constructor to create an instance
     * @param context Application context.
     * @return Instance of WifiAnalyzer or null on error.
     */
    @Nullable
    public static FakeConnectivityAnalyzer create(Context context, DobbyThreadpool threadpool, DobbyEventBus eventBus) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            return new FakeConnectivityAnalyzer(context.getApplicationContext(), connectivityManager, threadpool, eventBus);
        }
        return null;
    }

    //To set the fake mode
    public static void setFakeWifiConnectivityMode(@WifiConnectivityMode int mode) {
        fakeWifiConnectivityMode = mode;
    }

    @WifiConnectivityMode
    public int performConnectivityAndPortalTest(NetworkInfo activeNetwork) {
        return fakeWifiConnectivityMode;
    }

}
