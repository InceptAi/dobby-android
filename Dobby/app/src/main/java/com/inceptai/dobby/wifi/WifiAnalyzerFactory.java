package com.inceptai.dobby.wifi;

import android.content.Context;

import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.fake.FakeWifiAnalyzer;

import static com.inceptai.dobby.DobbyApplication.USE_FAKES;

/**
 * Created by arunesh on 4/10/17.
 */

public class WifiAnalyzerFactory {

    private static FakeWifiAnalyzer FAKE_WIFI_ANALYZER_INSTANCE = null;
    private static WifiAnalyzer WIFI_ANALYZER_INSTANCE = null;
    private WifiAnalyzerFactory() {}

    private static WifiAnalyzer create(Context context,
                                      DobbyThreadpool dobbyThreadpool,
                                      DobbyEventBus eventBus) {
        if (USE_FAKES.get()) {
            return FakeWifiAnalyzer.create(context, dobbyThreadpool, eventBus);
        } else {
            return WifiAnalyzer.create(context, dobbyThreadpool, eventBus);
        }
    }

    public static void cleanupFakeWifiAnalyzer() {
        if (FAKE_WIFI_ANALYZER_INSTANCE != null) {
            FAKE_WIFI_ANALYZER_INSTANCE.cleanup();
            FAKE_WIFI_ANALYZER_INSTANCE = null;
        }
    }

    public static void cleanupRealWifiAnalyzer() {
        if (WIFI_ANALYZER_INSTANCE != null) {
            WIFI_ANALYZER_INSTANCE.cleanup();
            WIFI_ANALYZER_INSTANCE = null;
        }
    }

    public static WifiAnalyzer getWifiAnalyzer(Context context,
                                               DobbyThreadpool dobbyThreadpool,
                                               DobbyEventBus eventBus) {
        if (USE_FAKES.get()) {
            return getFakeInstance(context, dobbyThreadpool, eventBus);
        } else {
            // return real instance.
            return getRealInstance(context, dobbyThreadpool, eventBus);
        }
    }

    private static FakeWifiAnalyzer getFakeInstance(Context context,
                                                    DobbyThreadpool dobbyThreadpool,
                                                    DobbyEventBus eventBus) {
        cleanupRealWifiAnalyzer();
        if (FAKE_WIFI_ANALYZER_INSTANCE ==  null) {
            // create new instance
            FAKE_WIFI_ANALYZER_INSTANCE = FakeWifiAnalyzer.create(context, dobbyThreadpool, eventBus);
        }
        return FAKE_WIFI_ANALYZER_INSTANCE;
    }

    private static WifiAnalyzer getRealInstance(Context context,
                                                    DobbyThreadpool dobbyThreadpool,
                                                    DobbyEventBus eventBus) {
        cleanupFakeWifiAnalyzer();
        if (WIFI_ANALYZER_INSTANCE ==  null) {
            // create new instance
            WIFI_ANALYZER_INSTANCE = WifiAnalyzer.create(context, dobbyThreadpool, eventBus);
        }
        return WIFI_ANALYZER_INSTANCE;
    }
}
