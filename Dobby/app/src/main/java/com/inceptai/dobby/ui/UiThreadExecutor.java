package com.inceptai.dobby.ui;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.util.concurrent.Executor;

/**
 * Created by arunesh on 4/8/17.
 */

public class UiThreadExecutor implements Executor {
    private final Handler handler = new Handler(Looper.getMainLooper());


    @Override
    public void execute(@NonNull Runnable command) {
        handler.post(command);
    }
}
