package com.inceptai.dobby.ping;

import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.fake.FakePingAnalyzer;
import com.inceptai.dobby.model.IPLayerInfo;
import com.inceptai.dobby.utils.DobbyLog;

import static com.inceptai.dobby.DobbyApplication.USE_FAKES;

/**
 * Created by arunesh on 4/10/17.
 */

public class PingAnalyzerFactory {

    private static FakePingAnalyzer FAKE_PING_ANALYZER_INSTANCE = null;
    private static PingAnalyzer PING_ANALYZER_INSTANCE = null;
    private PingAnalyzerFactory() {}

    private static PingAnalyzer create(IPLayerInfo ipLayerInfo,
                                      DobbyThreadpool dobbyThreadpool,
                                      DobbyEventBus eventBus) {
        if (USE_FAKES.get()) {
            return FakePingAnalyzer.create(ipLayerInfo, dobbyThreadpool, eventBus);
        } else {
            return PingAnalyzer.create(ipLayerInfo, dobbyThreadpool, eventBus);
        }
    }

    public static PingAnalyzer getPingAnalyzer(IPLayerInfo ipLayerInfo,
                                               DobbyThreadpool dobbyThreadpool,
                                               DobbyEventBus eventBus) {
        if (USE_FAKES.get()) {
            DobbyLog.v("Returning fake ping instance");
            return getFakeInstance(ipLayerInfo, dobbyThreadpool, eventBus);
        } else {
            // return real instance.
            DobbyLog.v("Returning real ping instance");
            return getRealInstance(ipLayerInfo, dobbyThreadpool, eventBus);
        }
    }

    private static FakePingAnalyzer getFakeInstance(IPLayerInfo ipLayerInfo,
                                                    DobbyThreadpool dobbyThreadpool,
                                                    DobbyEventBus eventBus) {
        if (FAKE_PING_ANALYZER_INSTANCE ==  null) {
            // create new instance
            FAKE_PING_ANALYZER_INSTANCE = FakePingAnalyzer.create(ipLayerInfo, dobbyThreadpool, eventBus);
        }
        return FAKE_PING_ANALYZER_INSTANCE;
    }

    private static PingAnalyzer getRealInstance(IPLayerInfo ipLayerInfo,
                                                    DobbyThreadpool dobbyThreadpool,
                                                    DobbyEventBus eventBus) {
        if (PING_ANALYZER_INSTANCE ==  null) {
            // create new instance
            PING_ANALYZER_INSTANCE = PingAnalyzer.create(ipLayerInfo, dobbyThreadpool, eventBus);
        }
        return PING_ANALYZER_INSTANCE;
    }
}
