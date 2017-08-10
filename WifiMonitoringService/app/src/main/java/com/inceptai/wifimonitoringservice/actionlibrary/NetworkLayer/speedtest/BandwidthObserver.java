package com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.speedtest;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import com.inceptai.wifimonitoringservice.actionlibrary.utils.ActionLibraryCodes;
import com.inceptai.wifimonitoringservice.utils.ServiceLog;

import java.util.List;
import java.util.concurrent.ExecutorService;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;

/**
 * Created by arunesh on 4/6/17.
 */

public class BandwidthObserver implements BandwidthAnalyzer.ResultsCallback, ObservableOnSubscribe<BandwidthProgressSnapshot> {
    @Nullable
    private ObservableEmitter<BandwidthProgressSnapshot> emitter;
    private boolean testsRunning = false;
    private BandwidthResult bandwidthResult;
    private Observable<BandwidthProgressSnapshot> bandwidthResultObservable;
    private BandwidthAnalyzer bandwidthAnalyzer;
    @ActionLibraryCodes.BandwidthTestMode
    private int testModeRequested;
    @ActionLibraryCodes.BandwidthTestMode
    private int testsDone;
    private ExecutorService executorService;
    private ListeningScheduledExecutorService listeningScheduledExecutorService;
    private Context context;
    private SettableFuture<Boolean> cancelBandwidthTestFuture;

    public BandwidthObserver(Context context, ExecutorService executorService,
                             ListeningScheduledExecutorService listeningScheduledExecutorService) {
        this.testModeRequested = ActionLibraryCodes.BandwidthTestMode.IDLE;
        this.executorService = executorService;
        this.context = context;
        this.listeningScheduledExecutorService = listeningScheduledExecutorService;
        this.testsDone = ActionLibraryCodes.BandwidthTestMode.IDLE;
        bandwidthResultObservable = Observable.create(this).share();
    }

    public synchronized boolean testsRunning() {
        return testsRunning;
    }

    public Observable<BandwidthProgressSnapshot> getBandwidthResultObservable() {
        return bandwidthResultObservable;
    }

    public BandwidthResult getLastBandwidthResult() {
        return bandwidthResult;
    }

    // BandwidthAnalyzer.ResultsCallback overrides:

    @Override
    public synchronized void onConfigFetch(final SpeedTestConfig config) {
        //Set client info
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (isEmitterValid()) {
                    emitter.onNext(new BandwidthProgressSnapshot(config));
                }
            }
        });
    }

    @Override
    public synchronized void onServerInformationFetch(ServerInformation serverInformation) {

    }

    @Override
    public synchronized void onClosestServersSelected(final List<ServerInformation.ServerDetails> closestServers) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (isEmitterValid()) {
                    emitter.onNext(new BandwidthProgressSnapshot(closestServers));
                }
            }
        });
    }

    @Override
    public synchronized void onBestServerSelected(final ServerInformation.ServerDetails bestServer) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (isEmitterValid()) {
                    emitter.onNext(new BandwidthProgressSnapshot(bestServer));
                }
            }
        });
    }



    @Override
    public synchronized void onTestFinished(@ActionLibraryCodes.BandwidthTestMode int testMode, BandwidthStats stats) {
        ServiceLog.v("BandwidthObserver onTestFinished");
        if (testMode == ActionLibraryCodes.BandwidthTestMode.UPLOAD && bandwidthResult != null)  {
            bandwidthResult.setUploadStats(stats);
        } else if (testMode == ActionLibraryCodes.BandwidthTestMode.DOWNLOAD && bandwidthResult != null) {
            bandwidthResult.setDownloadStats(stats);
        }

        if (areTestsDone(testMode)) {
            ServiceLog.v("Calling tests Done with testmode: " + testMode);
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    if (isEmitterValid()) {
                        if (bandwidthResult != null) {
                            ServiceLog.v("BO: onNext: Sending final bw results");
                            emitter.onNext(new BandwidthProgressSnapshot(bandwidthResult));
                        }
                        emitter.onComplete();
                    }
                }
            });
            testsDone();
        } else {
            ServiceLog.v("Tests not done.");
        }
    }

    @Override
    public synchronized void onTestProgress(final @ActionLibraryCodes.BandwidthTestMode int testMode, final double instantBandwidth) {
        ServiceLog.v("BandwidthObserver onTestProgress");
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (isEmitterValid()) {
                    emitter.onNext(new BandwidthProgressSnapshot(instantBandwidth, System.currentTimeMillis(), testMode));
                }
            }
        });
    }

    @Override
    public synchronized void onBandwidthTestError(final @ActionLibraryCodes.BandwidthTestMode int testMode,
                                     final @ActionLibraryCodes.ErrorCodes int errorCode,
                                     final @Nullable String errorMessage) {
        //Switch threads back
        ServiceLog.v("BandwidthObserver: onBandwidthTestError Got bw test error: " + errorCode + " testmode: " + testMode);
        //No need to call onComplete -- this terminates it
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                if (isEmitterValid()) {
                    emitter.onError(new BandwidthTestException(testMode, errorCode, errorMessage));
                }
            }
        });
        testsDone();
    }


    //Bandwidth Analyzer stuff
    public synchronized Observable<BandwidthProgressSnapshot> startBandwidthTest(final @ActionLibraryCodes.BandwidthTestMode int mode) {
        if (!testsRunning()) {
            markTestsAsRunning();
            testModeRequested = mode;
            bandwidthResult = new BandwidthResult(mode);
            bandwidthAnalyzer = new BandwidthAnalyzer(executorService, listeningScheduledExecutorService, context, this);
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    bandwidthAnalyzer.startBandwidthTestSync(mode);
                }
            });
        }
        return bandwidthResultObservable;
    }

    public synchronized boolean areBandwidthTestsRunning() {
        return testsRunning();
    }

    public synchronized ListenableFuture<Boolean> cancelBandwidthTests() {
        ServiceLog.v("NL cancel bw test");
        cancelBandwidthTestFuture = SettableFuture.create();
        if (!testsRunning()) {
            cancelBandwidthTestFuture.set(false);
            return cancelBandwidthTestFuture;
        }
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                if (bandwidthAnalyzer != null) {
                    bandwidthAnalyzer.cancelBandwidthTests();
                    if (isEmitterValid()) {
                        emitter.onError(new BandwidthTestException(testModeRequested,
                                ActionLibraryCodes.ErrorCodes.ERROR_TEST_INTERRUPTED, "Tests cancelled"));
                    }
                }
            }
        });
        testsDone();
        ServiceLog.v("NL done with bw cancellation");
        cancelBandwidthTestFuture.set(true);
        return cancelBandwidthTestFuture;
    }

    public void cleanup() {
        if (testsRunning()) {
            cancelBandwidthTests();
        }
        if (bandwidthAnalyzer != null) {
            bandwidthAnalyzer.cleanup();
            bandwidthAnalyzer = null;
        }
    }
    //Private stuff

    private void testsDone() {
        ServiceLog.v("Tests done");
        testsRunning = false;
        testsDone = ActionLibraryCodes.BandwidthTestMode.IDLE;
        testModeRequested = ActionLibraryCodes.BandwidthTestMode.IDLE;
        if (bandwidthAnalyzer != null) {
            bandwidthAnalyzer.cleanup();
            bandwidthAnalyzer = null;
        }
    }

    private synchronized void markTestsAsRunning() {
        testsRunning = true;
    }

    private boolean areTestsDone(@ActionLibraryCodes.BandwidthTestMode int testModeDone) {
        if (testsDone == ActionLibraryCodes.BandwidthTestMode.IDLE) {
            testsDone = testModeDone;
            return testsDone == testModeRequested;
        }

        if (testsDone == ActionLibraryCodes.BandwidthTestMode.UPLOAD && testModeDone == ActionLibraryCodes.BandwidthTestMode.DOWNLOAD) {
            testsDone = ActionLibraryCodes.BandwidthTestMode.DOWNLOAD_AND_UPLOAD;
        }

        if (testsDone == ActionLibraryCodes.BandwidthTestMode.DOWNLOAD && testModeDone == ActionLibraryCodes.BandwidthTestMode.UPLOAD) {
            testsDone = ActionLibraryCodes.BandwidthTestMode.DOWNLOAD_AND_UPLOAD;
        }
        return testsDone == testModeRequested;
    }

    // ObservableOnSubscribe<> methods

    @Override
    public void subscribe(ObservableEmitter<BandwidthProgressSnapshot> e) throws Exception {
        emitter = e;
    }

    //BandwidthTestException
    public static class BandwidthTestException extends Exception {
        @ActionLibraryCodes.BandwidthTestMode
        private int testMode;
        @ActionLibraryCodes.ErrorCodes private int errorCode;
        private String errorMessage;

        public BandwidthTestException(@ActionLibraryCodes.BandwidthTestMode int testMode,
                                      @ActionLibraryCodes.ErrorCodes int errorCode,
                                      @Nullable String errorMessage) {
            this.testMode = testMode;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        @ActionLibraryCodes.BandwidthTestMode
        public int getTestMode() {
            return testMode;
        }

        @ActionLibraryCodes.ErrorCodes
        public int getErrorCode() {
            return errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    private boolean isEmitterValid() {
        return emitter != null && !emitter.isDisposed();
    }

}
