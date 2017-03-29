package com.inceptai.dobby;

import android.app.Application;

/**
 * Created by arunesh on 3/28/17.
 */

public class DobbyApplication extends Application {
    public static final String TAG = "Dobby";
    private static final DobbyThreadpool THREADPOOL = new DobbyThreadpool();

    public DobbyThreadpool getThreadpool() {
        return THREADPOOL;
    }
}
