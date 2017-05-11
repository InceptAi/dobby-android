package com.inceptai.dobby;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.inceptai.dobby.speedtest.NewBandwidthAnalyzer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;

/**
 * Runs upload / download tests.
 */
@RunWith(AndroidJUnit4.class)
public class BandwidthAnalyzerTest {

    NewBandwidthAnalyzer newBandwidthAnalyzer;

    @Before
    public void setupInstance() {

    }

    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("com.inceptai.dobby.wifidoc", appContext.getPackageName());
    }
}
