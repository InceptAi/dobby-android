package com.inceptai.expertchat;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by arunesh on 6/22/17.
 */

public class ExpertChatThreadpool {
    private static ExpertChatThreadpool INSTANCE;

    private ListeningExecutorService listeningExecutorService;
    private ListeningScheduledExecutorService scheduledExecutorService;

    private UiThreadExecutor uiThreadExecutor;

    private ExpertChatThreadpool() {
        scheduledExecutorService = MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());
        listeningExecutorService = MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());
        uiThreadExecutor = new UiThreadExecutor();
    }

    public static ExpertChatThreadpool get() {
        if (INSTANCE == null) {
            INSTANCE = new ExpertChatThreadpool();
        }
        return INSTANCE;
    }

    public static class UiThreadExecutor implements Executor {
        private final Handler handler = new Handler(Looper.getMainLooper());


        @Override
        public void execute(@NonNull Runnable command) {
            handler.post(command);
        }
    }

    public ExecutorService getExecutorService() {
        return listeningExecutorService;
    }

    public ListeningExecutorService getListeningExecutorService() {
        return listeningExecutorService;
    }
}
