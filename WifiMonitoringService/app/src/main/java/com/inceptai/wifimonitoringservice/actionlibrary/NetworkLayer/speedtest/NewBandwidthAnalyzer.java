package com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.speedtest;

/**
 * Created by vivek on 3/30/17.
 */

import android.support.annotation.IntDef;
import android.support.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.R;
import com.inceptai.dobby.eventbus.DobbyEvent;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.model.BandwidthStats;
import com.inceptai.dobby.speedtest.BandwidthTestCodes.ErrorCodes;
import com.inceptai.dobby.speedtest.BandwidthTestCodes.TestMode;
import com.inceptai.dobby.utils.DobbyLog;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.bmartel.speedtest.model.SpeedTestError;

/**
 * Class contains logic performing bandwidth tests.
 */
@Singleton
public class NewBandwidthAnalyzer {
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
    private NewDownloadAnalyzer downloadAnalyzer;
    private NewUploadAnalyzer uploadAnalyzer;
    private DobbyThreadpool dobbyThreadpool;
    private DobbyEventBus eventBus;
    private long lastConfigFetchTimestampMs;
    private long lastBestServerDeterminationTimestampMs;
    private boolean enableServerListFetch = true;

    private BandwidthTestListener bandwidthTestListener;

    @BandwidthTestCodes.TestMode
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
        void onTestFinished(@BandwidthTestCodes.TestMode int testMode, BandwidthStats stats);
        void onTestProgress(@BandwidthTestCodes.TestMode int testMode, double instantBandwidth);

        //Error callback
        void onBandwidthTestError(@BandwidthTestCodes.TestMode int testMode,
                                  @ErrorCodes int errorCode,
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

    @Inject
    public NewBandwidthAnalyzer(DobbyThreadpool dobbyThreadpool,
                                DobbyEventBus eventBus,
                                DobbyApplication dobbyApplication) {
        this(dobbyThreadpool, eventBus, dobbyApplication, true); //Enabling server list fetch
    }

    public NewBandwidthAnalyzer(DobbyThreadpool dobbyThreadpool,
                                DobbyEventBus eventBus,
                                DobbyApplication dobbyApplication,
                                boolean enableServerListFetch) {
        this.bandwidthTestListener = new BandwidthTestListener();
        this.testMode = BandwidthTestCodes.TestMode.IDLE;
        this.eventBus = eventBus;
        this.parseSpeedTestConfig = new ParseSpeedTestConfig(this.bandwidthTestListener);
        //this.parseServerInformation = new ParseServerInformation(this.bandwidthTestListener);
        this.parseServerInformation = new ParseServerInformation(R.xml.speed_test_server_list,
                dobbyApplication.getApplicationContext(), this.bandwidthTestListener);
        this.dobbyThreadpool = dobbyThreadpool;
        lastConfigFetchTimestampMs = 0;
        lastBestServerDeterminationTimestampMs = 0;
        this.enableServerListFetch = enableServerListFetch;
        DobbyLog.v("NEW BANDWIDTH ANALYZER INSTANCE CREATED.");
    }


    /**
     * Factory constructor to create an instance
     * @return Instance of NewBandwidthAnalyzer or null on error.
     */
    @Nullable
    public static NewBandwidthAnalyzer create(ResultsCallback resultsCallback,
                                              DobbyThreadpool dobbyThreadpool,
                                              DobbyEventBus eventBus,
                                              DobbyApplication dobbyApplication) {
        Preconditions.checkNotNull(dobbyThreadpool);
        NewBandwidthAnalyzer analyzer = new NewBandwidthAnalyzer(dobbyThreadpool, eventBus, dobbyApplication);
        analyzer.registerCallback(resultsCallback);
        return analyzer;
    }

    /**
     * Registers new callback -- overrides old listener
     * @param resultsCallback
     */
    public void registerCallback(ResultsCallback resultsCallback) {
        this.resultsCallback = CallbackThreadSwitcher.wrap(
                dobbyThreadpool.getExecutorServiceForNetworkLayer(), resultsCallback);
    }

    public void startBandwidthTestSync(@TestMode int testMode) {
        try {
            startBandwidthTest(testMode);
        } catch(Exception e) {
            DobbyLog.i("Exception starting bandwidth test." + e);
        }
    }

    public synchronized void cancelBandwidthTests() {
        DobbyLog.v("NBA start with bw cancellation");
        markTestsAsCancelling();
        if (downloadAnalyzer != null) {
            DobbyLog.v("Cancelling downloadAnalyzer");
            downloadAnalyzer.cancelAllTests(dobbyThreadpool.getExecutorService());
        }
        if (uploadAnalyzer != null) {
            DobbyLog.v("Cancelling uploadAnalyzer");
            uploadAnalyzer.cancelAllTests(dobbyThreadpool.getExecutorService());
        }
        unRegisterCallback();
        markTestsAsCancelled();
        DobbyLog.v("NBA done with bw cancellation");
    }

    public void processDobbyBusEvents(DobbyEvent event) {
        int eventType = event.getEventType();
        DobbyLog.v("NBA Got event: " + event);
        if (event.getEventType() == DobbyEvent.EventType.WIFI_INTERNET_CONNECTIVITY_ONLINE) {
            dobbyThreadpool.submit(new Runnable() {
                @Override
                public void run() {
                    fetchSpeedTestConfigIfNeeded();
                    fetchServerConfigAndDetermineBestServerIfNeeded();
                }
            });
        }
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
            DobbyLog.v("NBA Config not fresh enough, fetching again");
            SpeedTestConfig speedTestConfig = parseSpeedTestConfig.getConfig(downloadMode);
            DobbyLog.v("NBA Fetched new config");
            if (speedTestConfig == null) {
                reportBandwidthError(BandwidthTestCodes.TestMode.CONFIG_FETCH,
                        ErrorCodes.ERROR_FETCHING_CONFIG, "Config fetch returned null");
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
            DobbyLog.v("NBA Server info not fresh, getting again");
            ServerInformation serverInformation = parseServerInformation.getServerInfo(enableServerListFetch);
            if (serverInformation == null) {
                reportBandwidthError(BandwidthTestCodes.TestMode.SERVER_FETCH,
                        ErrorCodes.ERROR_FETCHING_SERVER_INFO,
                        "Server info fetch returned null");
                return null;
            }
            //Set server information
            setServerInformation(serverInformation);

            //Get best server
            DobbyLog.v("NBA Getting best server again, since not fresh");
            ServerInformation.ServerDetails bestServer = computeBestServer(getSpeedTestConfig(), getServerInformation());
            if (bestServer == null) {
                reportBandwidthError(BandwidthTestCodes.TestMode.SERVER_FETCH,
                        ErrorCodes.ERROR_SELECTING_BEST_SERVER,
                        "best server returned as null");
                return null;
            }
            //Set best server
            setBestServer(bestServer);
        } else { //Use the existing best server for testing.
            if (resultsCallback != null) {
                resultsCallback.onServerInformationFetch(getServerInformation());
                DobbyLog.v("NBA Calling onServerInformationFetch with cached info");
                resultsCallback.onBestServerSelected(getBestServer());
                DobbyLog.v("NBA Calling onBestServerSelected with cached info");
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
            ParseServerInformation.ResultsCallback, NewDownloadAnalyzer.ResultsCallback,
            NewUploadAnalyzer.ResultsCallback, BestServerSelector.ResultsCallback {

        //Config callbacks
        @Override
        public void onConfigFetch(SpeedTestConfig config) {
            DobbyLog.v("NBA Speed test config fetched");
            if (resultsCallback != null) {
                resultsCallback.onConfigFetch(config);
            }
        }

        @Override
        public void onConfigFetchError(String error) {
            DobbyLog.v("NBA Speed test config fetched error: " + error);
            reportBandwidthError(BandwidthTestCodes.TestMode.CONFIG_FETCH,
                    ErrorCodes.ERROR_FETCHING_CONFIG,
                    error);
        }

        // Server information callbacks
        @Override
        public void onServerInformationFetch(ServerInformation serverInformation) {
            DobbyLog.v("NBA Speed test server information fetched, num servers:" + serverInformation.serverList.size());
            if (resultsCallback != null) {
                if (serverInformation != null && serverInformation.serverList.size() > 0) {
                    resultsCallback.onServerInformationFetch(serverInformation);
                } else {
                    reportBandwidthError(BandwidthTestCodes.TestMode.SERVER_FETCH,
                            ErrorCodes.ERROR_FETCHING_SERVER_INFO,
                            "Server information returned empty");
                }
            }
        }

        @Override
        public void onServerInformationFetchError(String error) {
            reportBandwidthError(BandwidthTestCodes.TestMode.SERVER_FETCH,
                    ErrorCodes.ERROR_FETCHING_SERVER_INFO,
                    error);
        }

        //Best server selection
        @Override
        public void onBestServerSelected(ServerInformation.ServerDetails bestServer) {
            DobbyLog.v("NBA bestServerSelected: " + bestServer.name);
            if (resultsCallback != null)
                resultsCallback.onBestServerSelected(bestServer);
        }

        @Override
        public void onBestServerSelectionError(String error) {
            reportBandwidthError(BandwidthTestCodes.TestMode.SERVER_FETCH,
                    ErrorCodes.ERROR_SELECTING_BEST_SERVER, error);
        }

        @Override
        public void onClosestServersSelected(List<ServerInformation.ServerDetails> closestServers) {
            if (resultsCallback != null)
                resultsCallback.onClosestServersSelected(closestServers);
        }

        @Override
        public void onFinish(@BandwidthTestCodes.TestMode int callbackTestMode, BandwidthStats bandwidthStats) {
            DobbyLog.v("NewBandwidthAnalyzer onFinish");
            if (resultsCallback != null) {
                resultsCallback.onTestFinished(callbackTestMode, bandwidthStats);
            }
            //Do we need to do upload here ?
            if (bandwidthAnalyzerState == BandwidthAnalyzerState.RUNNING &&
                    callbackTestMode == BandwidthTestCodes.TestMode.DOWNLOAD &&
                    testMode == BandwidthTestCodes.TestMode.DOWNLOAD_AND_UPLOAD) {
                dobbyThreadpool.submit(new Runnable() {
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
        public void onProgress(@BandwidthTestCodes.TestMode int callbackTestMode, double instantBandwidth) {
            DobbyLog.v("NewBandwidthAnalyzer onProgress");
            if (resultsCallback != null) {
                resultsCallback.onTestProgress(callbackTestMode, instantBandwidth);
            }
        }

        @Override
        public void onError(@BandwidthTestCodes.TestMode int callbackTestMode, SpeedTestError speedTestError, String errorMessage) {
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
            downloadAnalyzer = new NewDownloadAnalyzer(speedTestConfig.downloadConfig,
                    bestServer, bandwidthTestListener);
        }
        downloadAnalyzer.downloadTestWithMultipleThreads(DOWNLOAD_THREADS,REPORT_INTERVAL_MS);
    }

    private void performUpload() {
        if (uploadAnalyzer == null) {
            uploadAnalyzer = new NewUploadAnalyzer(speedTestConfig.uploadConfig,
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
            reportBandwidthError(TestMode.STARTING,
                    ErrorCodes.ERROR_TEST_ALREADY_RUNNING,
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
        DobbyLog.v("Trying to mark test as cancelled, current state: " + bandwidthAnalyzerState);
        if (bandwidthAnalyzerState == BandwidthAnalyzerState.CANCELLING) {
            DobbyLog.v("Marking test as cancelled");
            bandwidthAnalyzerState = BandwidthAnalyzerState.CANCELLED;
        }
    }

    synchronized private void markTestsAsCancelling() {
        DobbyLog.v("Trying to mark test as cancelling, current state: " + bandwidthAnalyzerState);
        if (bandwidthAnalyzerState == BandwidthAnalyzerState.RUNNING) {
            DobbyLog.v("Marking test as cancelling");
            bandwidthAnalyzerState = BandwidthAnalyzerState.CANCELLING;
        }
    }

    /**
     * start the speed test
     */
    private void startBandwidthTest(@BandwidthTestCodes.TestMode int testMode) {
        if (!checkTestStatusAndMarkRunningIfInactive()) {
            DobbyLog.i("Tests already running.");
            //Tests are already
            return;
        }
        DobbyLog.i("Starting bandwidth tests in NewBandwidthAnalyzer.");
        this.testMode = testMode;
        DobbyLog.i("NBA Fetching config.");
        if(fetchSpeedTestConfigIfNeeded() == null) {
            DobbyLog.e("Could not fetch config for speed test. Aborting");
            cancelBandwidthTests();
            return;
        }
        DobbyLog.i("NBA getting servers and determining best.");
        if (fetchServerConfigAndDetermineBestServerIfNeeded() == null) {
            DobbyLog.e("Could not fetch server information for speed test. Aborting");
            cancelBandwidthTests();
            return;
        }
        if (testMode == TestMode.DOWNLOAD_AND_UPLOAD || testMode == BandwidthTestCodes.TestMode.DOWNLOAD) {
            DobbyLog.i("NBA starting download.");
            performDownload();
        }
        if (testMode == BandwidthTestCodes.TestMode.UPLOAD) {
            DobbyLog.i("NBA starting upload.");
            performUpload();
        }
    }

    @ErrorCodes
    private int convertToBandwidthTestCodes(SpeedTestError error) {
        int errorCodeToReturn = ErrorCodes.ERROR_UNKNOWN;
        switch (error) {
            case INVALID_HTTP_RESPONSE:
                errorCodeToReturn = ErrorCodes.ERROR_INVALID_HTTP_RESPONSE;
                break;
            case SOCKET_ERROR:
                errorCodeToReturn = ErrorCodes.ERROR_SOCKET_ERROR;
                break;
            case SOCKET_TIMEOUT:
                errorCodeToReturn = ErrorCodes.ERROR_SOCKET_TIMEOUT;
                break;
            case CONNECTION_ERROR:
                errorCodeToReturn = ErrorCodes.ERROR_CONNECTION_ERROR;
                break;
        }
        return errorCodeToReturn;
    }

    private void reportBandwidthError(@BandwidthTestCodes.TestMode int testMode,
                              @ErrorCodes int errorCode,
                              @Nullable String errorMessage) {
        if (resultsCallback != null) {
            resultsCallback.onBandwidthTestError(testMode, errorCode, errorMessage);
        }
    }

}
