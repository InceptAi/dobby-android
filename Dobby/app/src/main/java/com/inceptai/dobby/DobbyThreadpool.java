package com.inceptai.dobby;

import android.util.Log;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.inceptai.dobby.ui.UiThreadExecutor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages a threadpool for background execution.
 */

public class DobbyThreadpool {

    // Sets the amount of time an idle thread waits before terminating
    private static final int KEEP_ALIVE_TIME = 1;

    // Sets the Time Unit to seconds
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

    private static int NUMBER_OF_CORES =
            Runtime.getRuntime().availableProcessors();
    private static int INTIAL_POOL_SIZE = NUMBER_OF_CORES;

    // A queue of Runnables
    private final BlockingQueue<Runnable> workQueue;

    private ThreadPoolExecutor dobbyThreadPool;
    private ListeningExecutorService listeningExecutorService;
    private ListeningScheduledExecutorService scheduledExecutorService;

    private UiThreadExecutor uiThreadExecutor;

    public DobbyThreadpool() {

        // Instantiates the queue of Runnables as a LinkedBlockingQueue
        workQueue = new LinkedBlockingQueue<>();
        Thread.currentThread().setUncaughtExceptionHandler(new DobbyUncaughtExceptionHandler());

        // Creates a thread pool manager
        dobbyThreadPool = new ThreadPoolExecutor(
                INTIAL_POOL_SIZE,       // Initial pool size
                2 * NUMBER_OF_CORES,       // Max pool size
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                workQueue, new DobbyThreadFactory());

        listeningExecutorService = MoreExecutors.listeningDecorator(dobbyThreadPool);
        scheduledExecutorService = MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());
        uiThreadExecutor = new UiThreadExecutor();
    }

    public void submit(Runnable runnable) {
        dobbyThreadPool.submit(runnable);
    }

    public ListeningExecutorService getListeningExecutorService() {
        return listeningExecutorService;
    }

    public Executor getExecutor() {
        return dobbyThreadPool;
    }

    public ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }

    public ListeningScheduledExecutorService getListeningScheduledExecutorService() {
        return scheduledExecutorService;
    }

    public Executor getUiThreadExecutor() {
        return uiThreadExecutor;
    }

    private static class DobbyThreadFactory implements ThreadFactory {
        private static DobbyUncaughtExceptionHandler exceptionHandler = new DobbyUncaughtExceptionHandler();
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        DobbyThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() :
                    Thread.currentThread().getThreadGroup();
            namePrefix = "pool-" +
                    poolNumber.getAndIncrement() +
                    "-thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            t.setUncaughtExceptionHandler(exceptionHandler);
            return t;
        }
    }

    private static class DobbyUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            Log.wtf("FATAL", e);
            System.err.println("FATAL" + e);
            System.exit(2);
        }
    }
}
