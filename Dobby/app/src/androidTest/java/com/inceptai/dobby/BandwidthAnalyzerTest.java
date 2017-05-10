package com.inceptai.dobby;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.inceptai.dobby.dagger.ObjectRegistry;
import com.inceptai.dobby.model.BandwidthStats;
import com.inceptai.dobby.speedtest.BandwithTestCodes;
import com.inceptai.dobby.speedtest.NewBandwidthAnalyzer;
import com.inceptai.dobby.speedtest.ServerInformation;
import com.inceptai.dobby.speedtest.SpeedTestConfig;
import com.inceptai.dobby.utils.DobbyLog;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.inject.Inject;

import static org.junit.Assert.*;

/**
 * Runs upload / download tests.
 */
@RunWith(AndroidJUnit4.class)
public class BandwidthAnalyzerTest {

    NewBandwidthAnalyzer newBandwidthAnalyzer;
    ObjectRegistry objectRegistry;

    @Before
    public void setupInstance() {
        objectRegistry = ObjectRegistry.get();
        newBandwidthAnalyzer = new NewBandwidthAnalyzer(objectRegistry.getThreadpool(), objectRegistry.getEventBus());
    }

    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("com.inceptai.dobby.wifidoc", appContext.getPackageName());
    }

    @Test
    public void runUploadTest() throws InterruptedException, ExecutionException {
        BandwidthCallback callback = new BandwidthCallback();
        newBandwidthAnalyzer.registerCallback(callback);
        newBandwidthAnalyzer.startBandwidthTestSync(BandwithTestCodes.TestMode.UPLOAD);
        Future<BandwidthStats> statsFuture = callback.asFuture();
        while (!statsFuture.isDone()) {
            Thread.sleep(5000);
        }
        assertNotNull(statsFuture.get());
    }

    private static class BandwidthCallback implements NewBandwidthAnalyzer.ResultsCallback {
        SettableFuture<BandwidthStats> future = SettableFuture.create();

        BandwidthCallback() {

        }

        ListenableFuture<BandwidthStats> asFuture() {
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
        public void onTestFinished(@BandwithTestCodes.TestMode int testMode, BandwidthStats stats) {
            DobbyLog.i("Test finished: " + testMode);
            future.set(stats);
        }

        @Override
        public void onTestProgress(@BandwithTestCodes.TestMode int testMode, double instantBandwidth) {
            DobbyLog.i("Test mode = " + testMode + " bw = " + instantBandwidth / 1.0e-6 + " Mbps." );
        }

        @Override
        public void onBandwidthTestError(@BandwithTestCodes.TestMode int testMode, @BandwithTestCodes.ErrorCodes int errorCode, @Nullable String errorMessage) {
            future.set(null);
        }
    }
}
