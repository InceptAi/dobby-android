package com.inceptai.dobby;

import android.app.Application;

/**
 * Created by arunesh on 3/28/17.
 */

public class DobbyApplication extends Application {
    public static final String TAG = "Dobby";
    private static final DobbyThreadpool THREADPOOL = new DobbyThreadpool();
    private NetworkLayer networkLayer;


    public DobbyThreadpool getThreadpool() {
        return THREADPOOL;
    }

    public NetworkLayer getNetworkLayer() {
        if (networkLayer == null) {
            networkLayer = new NetworkLayer(getApplicationContext(), THREADPOOL);
            networkLayer.initialize();
        }
        return networkLayer;
    }
}
