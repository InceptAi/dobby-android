package com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.speedtest;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
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
    private BandwidthResult result;
    private Observable<BandwidthProgressSnapshot> bandwidthResultObservable;
    private BandwidthAnalyzer bandwidthAnalyzer;
    @ActionLibraryCodes.BandwidthTestMode
    private int testModeRequested;
    @ActionLibraryCodes.BandwidthTestMode
    private int testsDone;
    private ExecutorService executorService;

    public BandwidthObserver(Context context, ExecutorService executorService,
                             ListeningScheduledExecutorService listeningScheduledExecutorService) {
        this.testModeRequested = ActionLibraryCodes.BandwidthTestMode.IDLE;
        testsDone = ActionLibraryCodes.BandwidthTestMode.IDLE;
        bandwidthResultObservable = Observable.create(this).share();
        bandwidthAnalyzer = new BandwidthAnalyzer(executorService, listeningScheduledExecutorService, context, this);
        this.executorService = executorService;
    }

    public synchronized void onCancelled() {
        // Tests cancelled.
        if (emitter != null) {
            emitter.onError(new BandwidthTestException(testModeRequested,
                    ActionLibraryCodes.ErrorCodes.ERROR_TEST_INTERRUPTED, "Tests cancelled"));
        }
        testsDone();
    }

    public synchronized boolean testsRunning() {
        return testsRunning;
    }

    public Observable<BandwidthProgressSnapshot> getBandwidthResultObservable() {
        return bandwidthResultObservable;
    }

    public BandwidthResult getLastBandwidthResult() {
        return result;
    }

    // BandwidthAnalyzer.ResultsCallback overrides:

    @Override
    public synchronized void onConfigFetch(SpeedTestConfig config) {
        //Set client info
        if (emitter != null) {
            emitter.onNext(new BandwidthProgressSnapshot(config));
        }
    }

    @Override
    public synchronized void onServerInformationFetch(ServerInformation serverInformation) {

    }

    @Override
    public synchronized void onClosestServersSelected(List<ServerInformation.ServerDetails> closestServers) {
        if (emitter != null) {
            emitter.onNext(new BandwidthProgressSnapshot(closestServers));
        }
    }

    @Override
    public synchronized void onBestServerSelected(ServerInformation.ServerDetails bestServer) {
        if (emitter != null) {
            emitter.onNext(new BandwidthProgressSnapshot(bestServer));
        }
    }



    @Override
    public synchronized void onTestFinished(@ActionLibraryCodes.BandwidthTestMode int testMode, BandwidthStats stats) {
        ServiceLog.v("BandwidthObserver onTestFinished");
        if (testMode == ActionLibraryCodes.BandwidthTestMode.UPLOAD && result != null)  {
            result.setUploadStats(stats);
        } else if (testMode == ActionLibraryCodes.BandwidthTestMode.DOWNLOAD && result != null) {
            result.setDownloadStats(stats);
        }

        if (areTestsDone(testMode)) {
            ServiceLog.v("Calling tests Done with testmode: " + testMode);
            testsDone();
            if (emitter != null) {
                if (result != null) {
                    emitter.onNext(new BandwidthProgressSnapshot(result));
                }
                emitter.onComplete();
            }
        } else {
            ServiceLog.v("Tests not done.");
        }
    }

    @Override
    public synchronized void onTestProgress(@ActionLibraryCodes.BandwidthTestMode int testMode, double instantBandwidth) {
        ServiceLog.v("BandwidthObserver onTestProgress");
        if (emitter != null) {
            emitter.onNext(new BandwidthProgressSnapshot(instantBandwidth, System.currentTimeMillis(), testMode));
        }
    }

    @Override
    public synchronized void onBandwidthTestError(@ActionLibraryCodes.BandwidthTestMode int testMode,
                                     @ActionLibraryCodes.ErrorCodes int errorCode,
                                     @Nullable String errorMessage) {
        //TODO: Inform the inference engine that we encountered an error during bandwidth tests.
        ServiceLog.v("BandwidthObserver: onBandwidthTestError Got bw test error: " + errorCode + " testmode: " + testMode);
        if (emitter != null) {
            //No need to call onComplete -- this terminates it
            emitter.onError(new BandwidthTestException(testMode, errorCode, errorMessage));
        }
        testsDone();
    }


    //Bandwidth Analyzer stuff
    public synchronized Observable<BandwidthProgressSnapshot> startBandwidthTest(final @ActionLibraryCodes.BandwidthTestMode int mode) {
        if (!testsRunning()) {
            markTestsAsRunning();
            testModeRequested = mode;
            result = new BandwidthResult(mode);
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

    public synchronized void cancelBandwidthTests() {
        ServiceLog.v("NL cancel bw test");
        onCancelled();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                bandwidthAnalyzer.cancelBandwidthTests();
            }
        });
        ServiceLog.v("NL done with bw cancellation");
    }

    public void cleanup() {
        if (testsRunning()) {
            cancelBandwidthTests();
        }
        bandwidthAnalyzer = null;
        bandwidthResultObservable = null;
        emitter = null;
    }
    //Private stuff

    private void testsDone() {
        testsRunning = false;
        testModeRequested = ActionLibraryCodes.BandwidthTestMode.IDLE;
        result = null;
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


}
