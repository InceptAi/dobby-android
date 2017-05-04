package com.inceptai.dobby;

import android.os.Process;
import android.util.Log;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.inceptai.dobby.ui.UiThreadExecutor;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
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
    private static int INTIAL_POOL_SIZE = Math.max(NUMBER_OF_CORES, 4);  // Lower bound by 4
    private static final int MAX_POOL_SIZE = Math.max(NUMBER_OF_CORES * 2, 10); //Lower bound by 10

    // A queue of Runnables
    private final BlockingQueue<Runnable> workQueue;

    private ThreadPoolExecutor threadPoolExecutor;
    private ListeningExecutorService listeningExecutorService;
    private ListeningScheduledExecutorService scheduledExecutorService;

    private ListeningScheduledExecutorService networkLayerExecutorService;
    private ListeningScheduledExecutorService eventBusExecutorService;

    private ListeningScheduledExecutorService scheduledExecutorServiceForPing;

    private UiThreadExecutor uiThreadExecutor;

    public DobbyThreadpool() {

        // Instantiates the queue of Runnables as a LinkedBlockingQueue
        workQueue = new LinkedBlockingQueue<>();
        Thread.UncaughtExceptionHandler handler = Thread.getDefaultUncaughtExceptionHandler();
        Log.i("Dobby", "DobbyThreadpool: Old handler:" + handler.getClass().getCanonicalName());
        Thread.UncaughtExceptionHandler newHandler = new DobbyUncaughtExceptionHandler(handler);
        // Thread.setDefaultUncaughtExceptionHandler(newHandler);

        // Creates a thread pool manager
        threadPoolExecutor = new ThreadPoolExecutor(
                INTIAL_POOL_SIZE,       // Initial pool size
                MAX_POOL_SIZE,       // Max pool size
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                workQueue, new DobbyThreadFactory(newHandler));

        listeningExecutorService = MoreExecutors.listeningDecorator(threadPoolExecutor);
        scheduledExecutorService = MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());
        uiThreadExecutor = new UiThreadExecutor();
        networkLayerExecutorService = MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());
        eventBusExecutorService = MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());

        scheduledExecutorServiceForPing = MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());
    }

    public void submit(Runnable runnable) {
        threadPoolExecutor.submit(runnable);
    }

    public ListeningExecutorService getListeningExecutorService() {
        return listeningExecutorService;
    }

    public ExecutorService getExecutorService() { return threadPoolExecutor; }
    public Executor getExecutor() {
        return threadPoolExecutor;
    }

    public ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }

    public ScheduledExecutorService getScheduledExecutorServiceForPing() {
        return scheduledExecutorServiceForPing;
    }

    public ListeningScheduledExecutorService getListeningScheduledExecutorService() {
        return scheduledExecutorService;
    }

    public ListeningScheduledExecutorService getExecutorServiceForNetworkLayer() {
        return networkLayerExecutorService;
    }

    public ListeningScheduledExecutorService getExecutorServiceForEventBus() {
        return eventBusExecutorService;
    }

    public Executor getUiThreadExecutor() {
        return uiThreadExecutor;
    }

    private static class DobbyThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        private final Thread.UncaughtExceptionHandler handler;

        DobbyThreadFactory(Thread.UncaughtExceptionHandler handler) {
            this.handler = handler;
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
            // t.setUncaughtExceptionHandler(handler);
            return t;
        }
    }

    public static class DobbyUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
        private Thread.UncaughtExceptionHandler uncaughtExceptionHandler;

        public DobbyUncaughtExceptionHandler(Thread.UncaughtExceptionHandler handler) {
            uncaughtExceptionHandler = handler;
        }

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            Log.wtf("FATAL", e);
            Log.e(DobbyApplication.TAG, "FATAL EXCEPTION :" + e.getStackTrace().toString());
            System.err.println("FATAL" + e);
            // uncaughtExceptionHandler.uncaughtException(t, e);
            Process.killProcess(Process.myPid());
            System.exit(2);
        }
    }
}
