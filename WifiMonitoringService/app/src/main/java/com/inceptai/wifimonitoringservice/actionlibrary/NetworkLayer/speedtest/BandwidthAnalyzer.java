package com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.speedtest;

/**
 * Created by vivek on 3/30/17.
 */

import android.content.Context;
import android.support.annotation.IntDef;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.inceptai.wifimonitoringservice.R;
import com.inceptai.wifimonitoringservice.actionlibrary.utils.ActionLibraryCodes;
import com.inceptai.wifimonitoringservice.utils.ServiceLog;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.concurrent.ExecutorService;

import fr.bmartel.speedtest.model.SpeedTestError;

/**
 * Class contains logic performing bandwidth tests.
 */
public class BandwidthAnalyzer {
    private static final int DOWNLOAD_THREADS = 1;
    private static final int UPLOAD_THREADS = 1;
    private static final int REPORT_INTERVAL_MS = 250;
    private static final int MAX_AGE_FOR_FRESHNESS_MS = 300000; // 5 mins

    @IntDef({BandwidthAnalyzerState.STOPPED, BandwidthAnalyzerState.RUNNING,
            BandwidthAnalyzerState.CANCELLING, BandwidthAnalyzerState.CANCELLED})
    @Retention(RetentionPolicy.SOURCE)
    @interface BandwidthAnalyzerState {
        int STOPPED = 0;
        int RUNNING = 1;
        int CANCELLING = 2;
        int CANCELLED = 4;
    }

    @BandwidthAnalyzerState
    private int bandwidthAnalyzerState;

    private ParseSpeedTestConfig parseSpeedTestConfig;
    private ParseServerInformation parseServerInformation;
    private SpeedTestConfig speedTestConfig;
    private ServerInformation serverInformation;
    private ServerInformation.ServerDetails bestServer;
    private DownloadAnalyzer downloadAnalyzer;
    private UploadAnalyzer uploadAnalyzer;
    private long lastConfigFetchTimestampMs;
    private long lastBestServerDeterminationTimestampMs;
    private boolean enableServerListFetch = true;
    private ExecutorService executorService;
    private ListeningScheduledExecutorService networkLayerExecutorService;

    private BandwidthTestListener bandwidthTestListener;

    @ActionLibraryCodes.BandwidthTestMode
    private int testMode;
    //Callbacks
    private ResultsCallback resultsCallback;

    /**
     * Callback interface for results. More methods to follow.
     */
    public interface ResultsCallback {

        //Config callbacks
        void onConfigFetch(SpeedTestConfig config);
        //Getting nearby servers
        void onServerInformationFetch(ServerInformation serverInformation);
        //Closest servers selected
        void onClosestServersSelected(List<ServerInformation.ServerDetails> closestServers);
        //Best server callbacks
        void onBestServerSelected(ServerInformation.ServerDetails bestServer);

        //Test callbacks
        void onTestFinished(@ActionLibraryCodes.BandwidthTestMode int testMode, BandwidthStats stats);
        void onTestProgress(@ActionLibraryCodes.BandwidthTestMode int testMode, double instantBandwidth);

        //Error callback
        void onBandwidthTestError(@ActionLibraryCodes.BandwidthTestMode int testMode,
                                  @ActionLibraryCodes.ErrorCodes int errorCode,
                                  @Nullable String errorMessage);
    }

    synchronized private void setSpeedTestConfig(SpeedTestConfig speedTestConfig) {
        this.speedTestConfig = speedTestConfig;
        lastConfigFetchTimestampMs = System.currentTimeMillis();
    }

    synchronized private void setServerInformation(ServerInformation serverInformation) {
        this.serverInformation = serverInformation;
    }

    synchronized private void setBestServer(ServerInformation.ServerDetails bestServer) {
        this.bestServer = bestServer;
        lastBestServerDeterminationTimestampMs = System.currentTimeMillis();
    }


    public BandwidthAnalyzer(ExecutorService executorService,
                             ListeningScheduledExecutorService networkLayerExecutorService,
                             Context context,
                             ResultsCallback resultsCallback) {
        this.bandwidthTestListener = new BandwidthTestListener();
        this.testMode = ActionLibraryCodes.BandwidthTestMode.IDLE;
        this.parseSpeedTestConfig = new ParseSpeedTestConfig(this.bandwidthTestListener);
        this.parseServerInformation = new ParseServerInformation(R.xml.speed_test_server_list,
                context.getApplicationContext(), this.bandwidthTestListener);
        this.networkLayerExecutorService = networkLayerExecutorService;
        this.executorService = executorService;
        lastConfigFetchTimestampMs = 0;
        lastBestServerDeterminationTimestampMs = 0;
        this.enableServerListFetch = true;
        registerCallback(resultsCallback);
        ServiceLog.v("NEW BANDWIDTH ANALYZER INSTANCE CREATED.");
    }

    /**
     * Registers new callback -- overrides old listener
     * @param resultsCallback
     */
    public void registerCallback(ResultsCallback resultsCallback) {
        this.resultsCallback = CallbackThreadSwitcher.wrap(
                networkLayerExecutorService, resultsCallback);
    }

    public void startBandwidthTestSync(@ActionLibraryCodes.BandwidthTestMode int testMode) {
        try {
            startBandwidthTest(testMode);
        } catch(Exception e) {
            ServiceLog.i("Exception starting bandwidth test." + e);
        }
    }

    public synchronized void cancelBandwidthTests() {
        ServiceLog.v("NBA start with bw cancellation");
        markTestsAsCancelling();
        if (downloadAnalyzer != null) {
            ServiceLog.v("Cancelling downloadAnalyzer");
            downloadAnalyzer.cancelAllTests(executorService);
        }
        if (uploadAnalyzer != null) {
            ServiceLog.v("Cancelling uploadAnalyzer");
            uploadAnalyzer.cancelAllTests(executorService);
        }
        unRegisterCallback();
        markTestsAsCancelled();
        ServiceLog.v("NBA done with bw cancellation");
    }

    public SpeedTestConfig getSpeedTestConfig() {
        return speedTestConfig;
    }

    public ServerInformation getServerInformation() {
        return serverInformation;
    }

    private ServerInformation.ServerDetails getBestServer() {
        return bestServer;
    }

    private SpeedTestConfig fetchSpeedTestConfigIfNeeded() {
        final String downloadMode = "http";
        //Get config if not fresh
        if (getSpeedTestConfig() == null || System.currentTimeMillis() - lastConfigFetchTimestampMs > MAX_AGE_FOR_FRESHNESS_MS) {
            ServiceLog.v("NBA Config not fresh enough, fetching again");
            SpeedTestConfig speedTestConfig = parseSpeedTestConfig.getConfig(downloadMode);
            ServiceLog.v("NBA Fetched new config");
            if (speedTestConfig == null) {
                reportBandwidthError(ActionLibraryCodes.BandwidthTestMode.CONFIG_FETCH,
                        ActionLibraryCodes.ErrorCodes.ERROR_FETCHING_CONFIG, "Config fetch returned null");
                return null;
            }
            //Update speed test config
            setSpeedTestConfig(speedTestConfig);
        } else { //we already have a fresh config. Do nothing. Use speedTestConfig.
            //Inform if anyone is listening for this.
            if (resultsCallback != null) {
                resultsCallback.onConfigFetch(getSpeedTestConfig());
            }
        }
        return getSpeedTestConfig();
    }

    private ServerInformation.ServerDetails fetchServerConfigAndDetermineBestServerIfNeeded() {
        //Get best server information if stale
        if (bestServer == null || System.currentTimeMillis() - lastBestServerDeterminationTimestampMs > MAX_AGE_FOR_FRESHNESS_MS) {
            ServiceLog.v("NBA Server info not fresh, getting again");
            ServerInformation serverInformation = parseServerInformation.getServerInfo(enableServerListFetch);
            if (serverInformation == null) {
                reportBandwidthError(ActionLibraryCodes.BandwidthTestMode.SERVER_FETCH,
                        ActionLibraryCodes.ErrorCodes.ERROR_FETCHING_SERVER_INFO,
                        "Server info fetch returned null");
                return null;
            }
            //Set server information
            setServerInformation(serverInformation);

            //Get best server
            ServiceLog.v("NBA Getting best server again, since not fresh");
            ServerInformation.ServerDetails bestServer = computeBestServer(getSpeedTestConfig(), getServerInformation());
            if (bestServer == null) {
                reportBandwidthError(ActionLibraryCodes.BandwidthTestMode.SERVER_FETCH,
                        ActionLibraryCodes.ErrorCodes.ERROR_SELECTING_BEST_SERVER,
                        "best server returned as null");
                return null;
            }
            //Set best server
            setBestServer(bestServer);
        } else { //Use the existing best server for testing.
            if (resultsCallback != null) {
                resultsCallback.onServerInformationFetch(getServerInformation());
                ServiceLog.v("NBA Calling onServerInformationFetch with cached info");
                resultsCallback.onBestServerSelected(getBestServer());
                ServiceLog.v("NBA Calling onBestServerSelected with cached info");
            }
        }
        return getBestServer();
    }

    /**
     * Un Registers callback -- sets to null
     */
    private void unRegisterCallback() {
        this.resultsCallback = null;
    }

    public void cleanup() {
        unRegisterCallback();
    }

    private class BandwidthTestListener implements ParseSpeedTestConfig.ResultsCallback,
            ParseServerInformation.ResultsCallback, DownloadAnalyzer.ResultsCallback,
            UploadAnalyzer.ResultsCallback, BestServerSelector.ResultsCallback {

        //Config callbacks
        @Override
        public void onConfigFetch(SpeedTestConfig config) {
            ServiceLog.v("NBA Speed test config fetched");
            if (resultsCallback != null) {
                resultsCallback.onConfigFetch(config);
            }
        }

        @Override
        public void onConfigFetchError(String error) {
            ServiceLog.v("NBA Speed test config fetched error: " + error);
            reportBandwidthError(ActionLibraryCodes.BandwidthTestMode.CONFIG_FETCH,
                    ActionLibraryCodes.ErrorCodes.ERROR_FETCHING_CONFIG,
                    error);
        }

        // Server information callbacks
        @Override
        public void onServerInformationFetch(ServerInformation serverInformation) {
            ServiceLog.v("NBA Speed test server information fetched, num servers:" + serverInformation.serverList.size());
            if (resultsCallback != null) {
                if (serverInformation != null && serverInformation.serverList.size() > 0) {
                    resultsCallback.onServerInformationFetch(serverInformation);
                } else {
                    reportBandwidthError(ActionLibraryCodes.BandwidthTestMode.SERVER_FETCH,
                            ActionLibraryCodes.ErrorCodes.ERROR_FETCHING_SERVER_INFO,
                            "Server information returned empty");
                }
            }
        }

        @Override
        public void onServerInformationFetchError(String error) {
            reportBandwidthError(ActionLibraryCodes.BandwidthTestMode.SERVER_FETCH,
                    ActionLibraryCodes.ErrorCodes.ERROR_FETCHING_SERVER_INFO,
                    error);
        }

        //Best server selection
        @Override
        public void onBestServerSelected(ServerInformation.ServerDetails bestServer) {
            ServiceLog.v("NBA bestServerSelected: " + bestServer.name);
            if (resultsCallback != null)
                resultsCallback.onBestServerSelected(bestServer);
        }

        @Override
        public void onBestServerSelectionError(String error) {
            reportBandwidthError(ActionLibraryCodes.BandwidthTestMode.SERVER_FETCH,
                    ActionLibraryCodes.ErrorCodes.ERROR_SELECTING_BEST_SERVER, error);
        }

        @Override
        public void onClosestServersSelected(List<ServerInformation.ServerDetails> closestServers) {
            if (resultsCallback != null)
                resultsCallback.onClosestServersSelected(closestServers);
        }

        @Override
        public void onFinish(@ActionLibraryCodes.BandwidthTestMode int callbackTestMode, BandwidthStats bandwidthStats) {
            ServiceLog.v("BandwidthAnalyzer onFinish");
            if (resultsCallback != null) {
                resultsCallback.onTestFinished(callbackTestMode, bandwidthStats);
            }
            //Do we need to do upload here ?
            if (bandwidthAnalyzerState == BandwidthAnalyzerState.RUNNING &&
                    callbackTestMode == ActionLibraryCodes.BandwidthTestMode.DOWNLOAD &&
                    testMode == ActionLibraryCodes.BandwidthTestMode.DOWNLOAD_AND_UPLOAD) {
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        performUpload();
                    }
                });
            } else {
                //Cleanup
                markTestsAsStopped();
            }
        }

        @Override
        public void onProgress(@ActionLibraryCodes.BandwidthTestMode int callbackTestMode, double instantBandwidth) {
            ServiceLog.v("BandwidthAnalyzer onProgress");
            if (resultsCallback != null) {
                resultsCallback.onTestProgress(callbackTestMode, instantBandwidth);
            }
        }

        @Override
        public void onError(@ActionLibraryCodes.BandwidthTestMode int callbackTestMode, SpeedTestError speedTestError, String errorMessage) {
            reportBandwidthError(callbackTestMode,
                    convertToBandwidthTestCodes(speedTestError), errorMessage);
            //Cancel bandwidth tests.
            cancelBandwidthTests();
        }
    }

    private ServerInformation.ServerDetails computeBestServer(SpeedTestConfig config,
                                                              ServerInformation info) {
        BestServerSelector serverSelector = new BestServerSelector(config, info, this.bandwidthTestListener);
        ServerInformation.ServerDetails serverDetails = serverSelector.getBestServer();
        serverSelector.cleanup();
        return serverDetails;
    }

    private void performDownload() {
        if (downloadAnalyzer == null) {
            downloadAnalyzer = new DownloadAnalyzer(speedTestConfig.downloadConfig,
                    bestServer, bandwidthTestListener);
        }
        downloadAnalyzer.downloadTestWithMultipleThreads(DOWNLOAD_THREADS,REPORT_INTERVAL_MS);
    }

    private void performUpload() {
        if (uploadAnalyzer == null) {
            uploadAnalyzer = new UploadAnalyzer(speedTestConfig.uploadConfig,
                    bestServer, bandwidthTestListener);
        }
        uploadAnalyzer.uploadTestWithMultipleThreads(UPLOAD_THREADS, REPORT_INTERVAL_MS);
    }

    private void finishTests() {
        markTestsAsStopped();
    }

    //TODO write a checkAndSet function for bandwidthAnalyzerState

    //Helper functions for state
    synchronized public boolean testsCurrentlyRunning() {
        return bandwidthAnalyzerState == BandwidthAnalyzerState.RUNNING;
    }

    synchronized private boolean checkTestStatusAndMarkRunningIfInactive() {
        if (testsCurrentlyRunning()) {
            //We don't want to report tests already running as an error --
            // since the UI shows this as an indication for user to restart the tests.
            reportBandwidthError(ActionLibraryCodes.BandwidthTestMode.STARTING,
                    ActionLibraryCodes.ErrorCodes.ERROR_TEST_ALREADY_RUNNING,
                    "Tests are already running.");
            return false;
        } else {
            bandwidthAnalyzerState = BandwidthAnalyzerState.RUNNING;
            return true;
        }
    }

    synchronized private void markTestsAsStopped() {
        bandwidthAnalyzerState = BandwidthAnalyzerState.STOPPED;
    }

    synchronized private void markTestsAsCancelled() {
        ServiceLog.v("Trying to mark test as cancelled, current state: " + bandwidthAnalyzerState);
        if (bandwidthAnalyzerState == BandwidthAnalyzerState.CANCELLING) {
            ServiceLog.v("Marking test as cancelled");
            bandwidthAnalyzerState = BandwidthAnalyzerState.CANCELLED;
        }
    }

    synchronized private void markTestsAsCancelling() {
        ServiceLog.v("Trying to mark test as cancelling, current state: " + bandwidthAnalyzerState);
        if (bandwidthAnalyzerState == BandwidthAnalyzerState.RUNNING) {
            ServiceLog.v("Marking test as cancelling");
            bandwidthAnalyzerState = BandwidthAnalyzerState.CANCELLING;
        }
    }

    /**
     * start the speed test
     */
    private void startBandwidthTest(@ActionLibraryCodes.BandwidthTestMode int testMode) {
        if (!checkTestStatusAndMarkRunningIfInactive()) {
            ServiceLog.i("Tests already running.");
            //Tests are already
            return;
        }
        ServiceLog.i("Starting bandwidth tests in BandwidthAnalyzer.");
        this.testMode = testMode;
        ServiceLog.i("NBA Fetching config.");
        if(fetchSpeedTestConfigIfNeeded() == null) {
            ServiceLog.e("Could not fetch config for speed test. Aborting");
            cancelBandwidthTests();
            return;
        }
        ServiceLog.i("NBA getting servers and determining best.");
        if (fetchServerConfigAndDetermineBestServerIfNeeded() == null) {
            ServiceLog.e("Could not fetch server information for speed test. Aborting");
            cancelBandwidthTests();
            return;
        }
        if (testMode == ActionLibraryCodes.BandwidthTestMode.DOWNLOAD_AND_UPLOAD || testMode == ActionLibraryCodes.BandwidthTestMode.DOWNLOAD) {
            ServiceLog.i("NBA starting download.");
            performDownload();
        }
        if (testMode == ActionLibraryCodes.BandwidthTestMode.UPLOAD) {
            ServiceLog.i("NBA starting upload.");
            performUpload();
        }
    }

    @ActionLibraryCodes.ErrorCodes
    private int convertToBandwidthTestCodes(SpeedTestError error) {
        int errorCodeToReturn = ActionLibraryCodes.ErrorCodes.ERROR_UNKNOWN;
        switch (error) {
            case INVALID_HTTP_RESPONSE:
                errorCodeToReturn = ActionLibraryCodes.ErrorCodes.ERROR_INVALID_HTTP_RESPONSE;
                break;
            case SOCKET_ERROR:
                errorCodeToReturn = ActionLibraryCodes.ErrorCodes.ERROR_SOCKET_ERROR;
                break;
            case SOCKET_TIMEOUT:
                errorCodeToReturn = ActionLibraryCodes.ErrorCodes.ERROR_SOCKET_TIMEOUT;
                break;
            case CONNECTION_ERROR:
                errorCodeToReturn = ActionLibraryCodes.ErrorCodes.ERROR_CONNECTION_ERROR;
                break;
        }
        return errorCodeToReturn;
    }

    private void reportBandwidthError(@ActionLibraryCodes.BandwidthTestMode int testMode,
                              @ActionLibraryCodes.ErrorCodes int errorCode,
                              @Nullable String errorMessage) {
        if (resultsCallback != null) {
            resultsCallback.onBandwidthTestError(testMode, errorCode, errorMessage);
        }
    }

}
