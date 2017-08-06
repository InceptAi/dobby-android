package com.inceptai.wifimonitoringservice;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by arunesh on 7/1/17.
 */

public class ServiceThreadPool {

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
    private ListeningScheduledExecutorService scheduledExecutorServiceForActions;
    private ListeningScheduledExecutorService networkLayerExecutorService;


    public ServiceThreadPool() {

        // Instantiates the queue of Runnables as a LinkedBlockingQueue
        workQueue = new LinkedBlockingQueue<>();
        // Creates a thread pool manager
        threadPoolExecutor = new ThreadPoolExecutor(
                INTIAL_POOL_SIZE,       // Initial pool size
                MAX_POOL_SIZE,       // Max pool size
                KEEP_ALIVE_TIME,
                KEEP_ALIVE_TIME_UNIT,
                workQueue);

        listeningExecutorService = MoreExecutors.listeningDecorator(threadPoolExecutor);
        scheduledExecutorService = MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());
        scheduledExecutorServiceForActions = MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());
        networkLayerExecutorService = MoreExecutors.listeningDecorator(Executors.newSingleThreadScheduledExecutor());
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

    public ScheduledExecutorService getScheduledExecutorServiceForActions() {
        return scheduledExecutorServiceForActions;
    }

    public ListeningScheduledExecutorService getNetworkLayerExecutorService() {
        return networkLayerExecutorService;
    }

    public void shutdown() {
        listeningExecutorService.shutdown();
        scheduledExecutorService.shutdown();
        scheduledExecutorServiceForActions.shutdown();
        threadPoolExecutor.shutdown();
    }
}
