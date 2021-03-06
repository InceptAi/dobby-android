package com.inceptai.dobby;

import android.support.annotation.Nullable;
import android.support.test.runner.AndroidJUnit4;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.inceptai.dobby.dagger.ObjectRegistry;
import com.inceptai.dobby.model.BandwidthStats;
import com.inceptai.dobby.speedtest.BandwidthResult;
import com.inceptai.dobby.speedtest.BandwidthTestCodes;
import com.inceptai.dobby.speedtest.NewBandwidthAnalyzer;
import com.inceptai.dobby.speedtest.ServerInformation;
import com.inceptai.dobby.speedtest.SpeedTestConfig;
import com.inceptai.dobby.utils.DobbyLog;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.junit.Assert.assertNotNull;

/**
 * Runs upload / download tests.
 */
@RunWith(AndroidJUnit4.class)
public class BandwidthAnalyzerTest {

    NewBandwidthAnalyzer newBandwidthAnalyzer;
    ObjectRegistry objectRegistry;
    Random random;

    @Before
    public void setupInstance() {
        objectRegistry = ObjectRegistry.get();
        newBandwidthAnalyzer = new NewBandwidthAnalyzer(objectRegistry.getThreadpool(),
                objectRegistry.getEventBus(), objectRegistry.getApplication());
        random = new Random();
    }

    /*
    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("com.inceptai.dobby.wifidoc", appContext.getPackageName());
    }
    */

    @Test
    public void runUploadTest() throws InterruptedException, ExecutionException {
        newBandwidthAnalyzer = new NewBandwidthAnalyzer(objectRegistry.getThreadpool(),
                objectRegistry.getEventBus(), objectRegistry.getApplication());
        Future<BandwidthResult> statsFuture = startBandwidthTest(BandwidthTestCodes.TestMode.UPLOAD);
        while (!statsFuture.isDone()) {
            Thread.sleep(2500);
        }
        assertNotNull(statsFuture.get());
    }

    @Test
    public void runDownloadTest() throws InterruptedException, ExecutionException {
        Future<BandwidthResult> statsFuture = startBandwidthTest(BandwidthTestCodes.TestMode.DOWNLOAD);
        while (!statsFuture.isDone()) {
            Thread.sleep(2500);
        }
        assertNotNull(statsFuture.get());
    }

    @Test
    public void runBothUploadAndDownloadTest() throws InterruptedException, ExecutionException {
        Future<BandwidthResult> statsFuture = startBandwidthTest(BandwidthTestCodes.TestMode.DOWNLOAD_AND_UPLOAD);
        while (!statsFuture.isDone()) {
            Thread.sleep(2500);
        }
        assertNotNull(statsFuture.get());
    }

    @Test
    public void uploadWithCancelTest()  {
        for (int i = 0; i < 10; i ++) {
            int randomDelay = random.nextInt(5000);
            DobbyLog.i("Starting tests with delay of " + randomDelay);
            Future<BandwidthResult> statsFuture = startBandwidthTest(BandwidthTestCodes.TestMode.UPLOAD);
            try {
                Thread.sleep(randomDelay);
                newBandwidthAnalyzer.cancelBandwidthTests();
                // assertNull(statsFuture.get());
            } catch (Exception e) {
                DobbyLog.e("Got exception " + e);
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    public void downloadWithCancelTest()  {
        for (int i = 0; i < 10; i ++) {
            int randomDelay = random.nextInt(5000);
            DobbyLog.i("Starting tests with delay of " + randomDelay);
            Future<BandwidthResult> statsFuture = startBandwidthTest(BandwidthTestCodes.TestMode.DOWNLOAD);
            try {
                Thread.sleep(randomDelay);
                newBandwidthAnalyzer.cancelBandwidthTests();
                // assertNull(statsFuture.get());
            } catch (Exception e) {
                DobbyLog.e("Got exception " + e);
                throw new RuntimeException(e);
            }
        }
    }


    @Test
    public void runBothUploadAndDownloadTestWithoutServerListFetch() throws InterruptedException, ExecutionException {
        newBandwidthAnalyzer = new NewBandwidthAnalyzer(objectRegistry.getThreadpool(),
                objectRegistry.getEventBus(), objectRegistry.getApplication(), false);
        Future<BandwidthResult> statsFuture = startBandwidthTest(BandwidthTestCodes.TestMode.DOWNLOAD_AND_UPLOAD);
        while (!statsFuture.isDone()) {
            Thread.sleep(2500);
        }
        assertNotNull(statsFuture.get());
        assertNotNull(newBandwidthAnalyzer.getServerInformation());
    }

    private ListenableFuture<BandwidthResult> startBandwidthTest(@BandwidthTestCodes.TestMode int testMode) {
        BandwidthCallback callback = new BandwidthCallback(testMode);
        newBandwidthAnalyzer.registerCallback(callback);
        newBandwidthAnalyzer.startBandwidthTestSync(testMode);
        return callback.asFuture();
    }

    private static class BandwidthCallback implements NewBandwidthAnalyzer.ResultsCallback {
        SettableFuture<BandwidthResult> future = SettableFuture.create();
        private BandwidthResult result;

        @BandwidthTestCodes.TestMode
        private int testModeRequested;
        @BandwidthTestCodes.TestMode private int testsDone;

        BandwidthCallback(int testModeRequested) {
            this.testModeRequested = testModeRequested;
            result = new BandwidthResult(testModeRequested);
            testsDone = BandwidthTestCodes.TestMode.IDLE;
        }

        ListenableFuture<BandwidthResult> asFuture() {
            return future;
        }

        @Override
        public void onConfigFetch(SpeedTestConfig config) {

        }

        @Override
        public void onServerInformationFetch(ServerInformation serverInformation) {

        }

        @Override
        public void onClosestServersSelected(List<ServerInformation.ServerDetails> closestServers) {

        }

        @Override
        public void onBestServerSelected(ServerInformation.ServerDetails bestServer) {

        }

        @Override
        public void onTestFinished(@BandwidthTestCodes.TestMode int testMode, BandwidthStats stats) {
            DobbyLog.i("Test finished: " + testMode);
            if (testMode == BandwidthTestCodes.TestMode.UPLOAD) {
                result.setUploadStats(stats);
            } else if (testMode == BandwidthTestCodes.TestMode.DOWNLOAD) {
                result.setDownloadStats(stats);
            }
            if (areTestsDone(testMode)) {
                future.set(result);
            }
        }

        @Override
        public void onTestProgress(@BandwidthTestCodes.TestMode int testMode, double instantBandwidth) {
            DobbyLog.i("Test mode = " + testMode + " bw = " + instantBandwidth / 1.0e-6 + " Mbps." );
        }

        @Override
        public void onBandwidthTestError(@BandwidthTestCodes.TestMode int testMode, @BandwidthTestCodes.ErrorCodes int errorCode, @Nullable String errorMessage) {
            future.set(null);
        }

        private boolean areTestsDone(@BandwidthTestCodes.TestMode int testModeDone) {
            if (testsDone == BandwidthTestCodes.TestMode.IDLE) {
                testsDone = testModeDone;
                return testsDone == testModeRequested;
            }

            if (testsDone == BandwidthTestCodes.TestMode.UPLOAD && testModeDone == BandwidthTestCodes.TestMode.DOWNLOAD) {
                testsDone = BandwidthTestCodes.TestMode.DOWNLOAD_AND_UPLOAD;
            }

            if (testsDone == BandwidthTestCodes.TestMode.DOWNLOAD && testModeDone == BandwidthTestCodes.TestMode.UPLOAD) {
                testsDone = BandwidthTestCodes.TestMode.DOWNLOAD_AND_UPLOAD;
            }
            return testsDone == testModeRequested;
        }
    }
}
