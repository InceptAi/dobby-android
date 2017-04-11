package com.inceptai.dobby.fake;

import android.util.Log;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import fr.bmartel.speedtest.RepeatWrapper;
import fr.bmartel.speedtest.SpeedTestReport;
import fr.bmartel.speedtest.SpeedTestSocket;
import fr.bmartel.speedtest.inter.IRepeatListener;
import fr.bmartel.speedtest.inter.ISpeedTestListener;
import fr.bmartel.speedtest.model.SpeedTestError;
import fr.bmartel.speedtest.model.SpeedTestMode;
import fr.bmartel.speedtest.model.UploadStorageType;

import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * FakeSpeedTestSocket allows fake behavior to be programmed.
 * Other related fake classes: SpeedTestReport and SpeedTestError, SpeedTestConfig.UploadConfig,
 * ISpeedTestListener and IRepeatListener.
 */

public class FakeSpeedTestSocket extends SpeedTestSocket {
    private static final int PACKET_SIZE = 1000;  // some default value.
    private static final float PROGRESS_PERCENT = 0.5F; // default value.
    private static final long INITIAL_DELAY_MS = 100L;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private HashSet<ISpeedTestListener> listeners;
    private HashSet<FakeRequest> requestSet;
    private RepeatWrapper repeatWrapper = new RepeatWrapper(this);


    public static class Config {
        private double maxDownloadBandwidthMbps;
        private double maxUploadBandwidthMbps;

        private SpeedTestError errorToBeInjected; // null for no error

        public Config(double maxDownloadBandwidthMbps, double maxUploadBandwidthMbps,
                      SpeedTestError errorToBeInjected) {
            this.maxDownloadBandwidthMbps = maxDownloadBandwidthMbps;
            this.maxUploadBandwidthMbps = maxUploadBandwidthMbps;
            this.errorToBeInjected = errorToBeInjected;
        }

        public double getMaxDownloadBandwidthMbps() {
            return maxDownloadBandwidthMbps;
        }

        public double getMaxUploadBandwidthMbps() {
            return maxUploadBandwidthMbps;
        }

        public SpeedTestError getErrorToBeInjected() {
            return errorToBeInjected;
        }
    }

    public static final Config DEFAULT_FAKE_CONFIG = new Config(10.0, 5.0, null);
    private static Config testConfig = DEFAULT_FAKE_CONFIG;

    public static synchronized void setFakeConfig(Config config) {
        testConfig = config;
    }

    public FakeSpeedTestSocket() {
        super();
        listeners = new HashSet<>();
        requestSet = new HashSet<>();
        Log.i(TAG, "Fake speed test socket created.");
    }

    @Override
    public void addSpeedTestListener(ISpeedTestListener listener) {
        listeners.add(listener);
    }


    @Override
    public void startUploadRepeat(String hostname, String uri, int repeatWindow, int reportPeriodMillis, int fileSizeOctet, IRepeatListener repeatListener) {
        FakeRequest request = new FakeRequest(SpeedTestMode.UPLOAD, testConfig, scheduler, uri, reportPeriodMillis, repeatListener, (long) repeatWindow);
        requestSet.add(request);
        request.start();
    }

    @Override
    public void startDownloadRepeat(String hostname, String uri, int repeatWindow, int reportPeriodMillis, IRepeatListener repeatListener) {
        FakeRequest request = new FakeRequest(SpeedTestMode.DOWNLOAD, testConfig, scheduler, uri, reportPeriodMillis, repeatListener, (long) repeatWindow);
        requestSet.add(request);
        request.start();
    }

    @Override
    public void forceStopTask() {
        // Callbacks.
        for (FakeRequest request : requestSet) {
            request.stopNow();
        }
        super.forceStopTask();  // This is needed to stop the executors created by the parent class.
    }

    /**
     * Class represents a fake request (download or upload), implements the ability to generate fake
     * data for that request.
     */
    private class FakeRequest implements Runnable {

        SpeedTestMode speedTestMode;
        Config config;
        ScheduledExecutorService executorService;
        String uri;
        int reportPeriodMs;
        ScheduledFuture<?> scheduledFuture;
        IRepeatListener repeatListener;
        long startTs;
        double lastCurrBw;
        long durationMs;


        public FakeRequest(SpeedTestMode speedTestMode, Config config, ScheduledExecutorService executorService,
                           String uri, int reportPeriodMs, IRepeatListener repeatListener, long durationMs) {
            this.speedTestMode = speedTestMode;
            this.config = config;
            this.executorService = executorService;
            this.uri = uri;
            this.reportPeriodMs = reportPeriodMs;
            this.repeatListener = repeatListener;
            this.durationMs = durationMs;
        }

        void start() {
            startTs = System.currentTimeMillis();

            // Initialize and callbacks.
            scheduledFuture = executorService.scheduleAtFixedRate(this, INITIAL_DELAY_MS, reportPeriodMs, TimeUnit.MILLISECONDS);
        }

        void stopNow() {
            scheduledFuture.cancel(true);
            long currentTs = System.currentTimeMillis();
            SpeedTestReport report = createReport(speedTestMode, 1.0F, startTs, currentTs, lastCurrBw, 1);
            sendProgress(speedTestMode, PROGRESS_PERCENT, report);
            repeatListener.onFinish(report);
            sendFinishedCallback(speedTestMode, report);
        }

        @Override
        public void run() {
            // Update stats and make callback. Inject error if requested.
            long currentTs = System.currentTimeMillis();
            long delta = currentTs - startTs;
            double maxBw = speedTestMode == SpeedTestMode.DOWNLOAD ? config.getMaxDownloadBandwidthMbps() : config.getMaxUploadBandwidthMbps();
            double currBwMbps = getInstantBandwidth(delta, maxBw);
            Log.i(TAG, "Current b/w:" + currBwMbps + " Mbps.");
            lastCurrBw = currBwMbps;
            float progressPercent = PROGRESS_PERCENT;
            SpeedTestReport report = createReport(speedTestMode, progressPercent, startTs, currentTs,
                    currBwMbps * 1.0e6, 1);
            sendProgress(speedTestMode, progressPercent, report);
            repeatListener.onReport(report);
            if (delta >= durationMs) {
                stopNow();
            }
        }
    }

    private static double getInstantBandwidth(long elapsedTimeMillis, double maxBandwidthMbps) {
        double elapsedTimeSeconds = (double) elapsedTimeMillis / 1000.0;
        return maxBandwidthMbps * 1.0 / ( 1.0 + Math.pow(Math.E,  -1.0 * elapsedTimeSeconds));
    }

    private static SpeedTestReport createReport(SpeedTestMode testMode, float progressPercent, long startTimeMs,
                                                long reportTimeMs, double currentBw, int requestNum) {
        return new SpeedTestReport(testMode, progressPercent, startTimeMs, reportTimeMs,
                PACKET_SIZE, PACKET_SIZE, BigDecimal.valueOf(currentBw / 8),
                BigDecimal.valueOf(currentBw), requestNum);
    }

    private SpeedTestReport sendProgress(SpeedTestMode testMode, float progressPercent, SpeedTestReport report) {
        for (ISpeedTestListener listener : listeners) {
            if (testMode == SpeedTestMode.DOWNLOAD) {
                listener.onDownloadProgress(progressPercent, report);
            } else {
                listener.onUploadProgress(progressPercent, report);
            }
        }
        return report;
    }

    private void sendFinishedCallback(SpeedTestMode testMode, SpeedTestReport report) {
        for (ISpeedTestListener listener : listeners) {
            if (testMode == SpeedTestMode.DOWNLOAD) {
                listener.onDownloadFinished(report);
            } else {
                listener.onUploadFinished(report);
            }
        }
    }

    /**
     * Unfortunately we have to implement this method since SpeedTestTask in SpeedTestSocket uses
     * getRepeatWrapper() in its constructor.
     * @return
     */
    @Override
    public RepeatWrapper getRepeatWrapper() {
        return repeatWrapper;
    }


    //////////////////  Unimplemented methods:
    @Override
    public void removeSpeedTestListener(ISpeedTestListener listener) {
        throw new RuntimeException("Unimplemented removeSpeedTestListener()");
    }

    @Override
    public void shutdownAndWait() {
        throw new RuntimeException("Unimplemented shutdownAndWait().");
    }

    @Override
    public void startFixedDownload(String hostname, int port, String uri, int maxDuration) {
        throw new RuntimeException("Unimplemented startFixedDownload().");
    }

    @Override
    public void startFixedDownload(String hostname, int port, String uri, int maxDuration, int reportInterval) {
        throw new RuntimeException("Unimplemented startFixedDownload().");
    }

    @Override
    public void startDownload(String hostname, String uri) {
        throw new RuntimeException("Unimplemented startDownload().");
    }

    @Override
    public void startDownload(String hostname, String uri, int reportInterval) {
        throw new RuntimeException("Unimplemented startDownload().");
    }

    @Override
    public void startDownload(String hostname, int port, String uri, int reportInterval) {
        throw new RuntimeException("Unimplemented startDownload().");
    }

    @Override
    public void startDownload(String hostname, int port, String uri) {
        throw new RuntimeException("Unimplemented startDownload().");
    }

    @Override
    public void startFtpDownload(String hostname, String uri, int reportInterval) {
        throw new RuntimeException("Unimplemented startFtpDownload().");
    }

    @Override
    public void startFtpFixedDownload(String hostname, String uri, int maxDuration, int reportInterval) {
        throw new RuntimeException("Unimplemented startFtpFixedDownload().");
    }

    @Override
    public void startFtpFixedDownload(String hostname, String uri, int maxDuration) {
        throw new RuntimeException("Unimplemented startFtpFixedDownload().");
    }

    @Override
    public void startFtpDownload(String hostname, String uri) {
        throw new RuntimeException("Unimplemented startFtpDownload().");
    }

    @Override
    public void startFtpDownload(String hostname, int port, String uri, String user, String password) {
        throw new RuntimeException("Unimplemented startFtpDownload().");
    }

    @Override
    public void startFixedUpload(String hostname, int port, String uri, int fileSizeOctet, int maxDuration) {
        throw new RuntimeException("Unimplemented startFixedUpload().");
    }

    @Override
    public void startFixedUpload(String hostname, int port, String uri, int fileSizeOctet, int maxDuration, int reportInterval) {
        throw new RuntimeException("Unimplemented startFixedUpload().");
    }

    @Override
    public void startFtpFixedUpload(String hostname, String uri, int fileSizeOctet, int maxDuration, int reportInterval) {
        throw new RuntimeException("Unimplemented startFtpFixedUpload().");
    }

    @Override
    public void startFtpFixedUpload(String hostname, String uri, int fileSizeOctet, int maxDuration) {
        throw new RuntimeException("Unimplemented startFtpFixedUpload().");
    }

    @Override
    public void startFtpUpload(String hostname, String uri, int fileSizeOctet, int reportInterval) {
        throw new RuntimeException("Unimplemented startFtpUpload().");
    }

    @Override
    public void startFtpUpload(String hostname, String uri, int fileSizeOctet) {
        throw new RuntimeException("Unimplemented startFtpUpload().");
    }

    @Override
    public void startFtpUpload(String hostname, int port, String uri, int fileSizeOctet, String user, String password) {
        throw new RuntimeException("Unimplemented startFtpUpload().");
    }

    @Override
    public void startUpload(String hostname, String uri, int fileSizeOctet) {
        throw new RuntimeException("Unimplemented startUpload().");
    }

    @Override
    public void startUpload(String hostname, String uri, int fileSizeOctet, int reportInterval) {
        throw new RuntimeException("Unimplemented startUpload().");
    }

    @Override
    public void startUpload(String hostname, int port, String uri, int fileSizeOctet, int reportInterval) {
        throw new RuntimeException("Unimplemented startUpload().");
    }

    @Override
    public void startUpload(String hostname, int port, String uri, int fileSizeOctet) {
        throw new RuntimeException("Unimplemented startUpload().");
    }


    @Override
    public void startDownloadRepeat(String hostname, int port, String uri, int repeatWindow, int reportPeriodMillis, IRepeatListener repeatListener) {
        throw new RuntimeException("Unimplemented startDownloadRepeat().");
    }


    @Override
    public void startUploadRepeat(String hostname, int port, String uri, int repeatWindow, int reportPeriodMillis, int fileSizeOctet, IRepeatListener repeatListener) {
        throw new RuntimeException("Unimplemented startUploadRepeat().");
    }


    @Override
    public SpeedTestReport getLiveDownloadReport() {
        throw new RuntimeException("Unimplemented getLiveDownloadReport().");
    }

    @Override
    public SpeedTestReport getLiveUploadReport() {
        throw new RuntimeException("Unimplemented getLiveUploadReport().");
    }

    @Override
    public void closeSocket() {
        throw new RuntimeException("Unimplemented closeSocket().");
    }

    @Override
    public SpeedTestMode getSpeedTestMode() {
        throw new RuntimeException("Unimplemented getSpeedTestMode() .");
    }

    @Override
    public void setSocketTimeout(int socketTimeoutMillis) {
        throw new RuntimeException("Unimplemented setSocketTimeout().");
    }

    @Override
    public int getSocketTimeout() {
        throw new RuntimeException("Unimplemented getSocketTimeout().");
    }

    @Override
    public int getUploadChunkSize() {
        throw new RuntimeException("Unimplemented getUploadChunkSize().");
    }


    @Override
    public void setUploadChunkSize(int uploadChunkSize) {
        throw new RuntimeException("Unimplemented setUploadChunkSize().");
    }

    @Override
    public void setDefaultRoundingMode(RoundingMode roundingMode) {
        throw new RuntimeException("Unimplemented setDefaultRoundingMode().");
    }

    @Override
    public void setDefaultScale(int scale) {
        throw new RuntimeException("Unimplemented setDefaultScale().");
    }

    @Override
    public void setUploadSetupTime(long setupTime) {
        throw new RuntimeException("Unimplemented setUploadSetupTime().");
    }

    @Override
    public void setDownloadSetupTime(long setupTime) {
        throw new RuntimeException("Unimplemented setDownloadSetupTime().");
    }

    @Override
    public long getDownloadSetupTime() {
        throw new RuntimeException("Unimplemented getDownloadSetupTime().");
    }

    @Override
    public long getUploadSetupTime() {
        throw new RuntimeException("Unimplemented getUploadSetupTime().");
    }

    @Override
    public RoundingMode getDefaultRoundingMode() {
        throw new RuntimeException("Unimplemented getDefaultRoundingMode().");
    }

    @Override
    public int getDefaultScale() {
        throw new RuntimeException("Unimplemented getDefaultScale().");
    }

    @Override
    public UploadStorageType getUploadStorageType() {
        throw new RuntimeException("Unimplemented getUploadStorageType().");
    }

    @Override
    public void setUploadStorageType(UploadStorageType uploadStorageType) {
        throw new RuntimeException("Unimplemented setUploadStorageType().");
    }

    @Override
    public void clearListeners() {
        throw new RuntimeException("Unimplemented clearListeners().");
    }
}
