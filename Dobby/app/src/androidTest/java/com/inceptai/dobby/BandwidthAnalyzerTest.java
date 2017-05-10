package com.inceptai.dobby;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.inceptai.dobby.dagger.ObjectRegistry;
import com.inceptai.dobby.speedtest.BandwithTestCodes;
import com.inceptai.dobby.speedtest.NewBandwidthAnalyzer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

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
    public void runUploadTest() {
        newBandwidthAnalyzer.registerCallback(null);
        newBandwidthAnalyzer.startBandwidthTestSync(BandwithTestCodes.TestMode.UPLOAD);
    }
}
