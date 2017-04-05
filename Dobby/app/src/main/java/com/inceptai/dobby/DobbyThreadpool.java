package com.inceptai.dobby;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

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
    private static int INTIAL_POOL_SIZE = 1;

    // A queue of Runnables
    private final BlockingQueue<Runnable> workQueue;

    private ThreadPoolExecutor dobbyThreadPool;
    private ListeningExecutorService listeningExecutorService;

    DobbyThreadpool() {

        // Instantiates the queue of Runnables as a LinkedBlockingQueue
        workQueue = new LinkedBlockingQueue<Runnable>();

        // Creates a thread pool manager
        dobbyThreadPool = new ThreadPoolExecutor(
                INTIAL_POOL_SIZE,       // Initial pool size
                NUMBER_OF_CORES,       // Max pool size
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                workQueue);

        listeningExecutorService = MoreExecutors.listeningDecorator(dobbyThreadPool);
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
}
