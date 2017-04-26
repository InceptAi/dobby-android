package com.inceptai.dobby.ping;

import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.fake.FakePingAnalyzer;
import com.inceptai.dobby.model.IPLayerInfo;

import java.util.concurrent.ExecutorService;

import static com.inceptai.dobby.DobbyApplication.USE_FAKES;

/**
 * Created by arunesh on 4/10/17.
 */

public class PingAnalyzerFactory {

    private static FakePingAnalyzer FAKE_PING_ANALYZER_INSTANCE = null;
    private static PingAnalyzer PING_ANALYZER_INSTANCE = null;
    private PingAnalyzerFactory() {}

    private static PingAnalyzer create(IPLayerInfo ipLayerInfo,
                                      ListeningScheduledExecutorService executorService,
                                      DobbyEventBus eventBus) {
        if (USE_FAKES.get()) {
            return FakePingAnalyzer.create(ipLayerInfo, executorService, eventBus);
        } else {
            return PingAnalyzer.create(ipLayerInfo, executorService, eventBus);
        }
    }

    public static PingAnalyzer getPingAnalyzer(IPLayerInfo ipLayerInfo,
                                               ListeningScheduledExecutorService executorService,
                                               DobbyEventBus eventBus) {
        if (USE_FAKES.get()) {
            return getFakeInstance(ipLayerInfo, executorService, eventBus);
        } else {
            // return real instance.
            return getRealInstance(ipLayerInfo, executorService, eventBus);
        }
    }

    private static FakePingAnalyzer getFakeInstance(IPLayerInfo ipLayerInfo,
                                                    ListeningScheduledExecutorService executorService,
                                                    DobbyEventBus eventBus) {
        if (FAKE_PING_ANALYZER_INSTANCE ==  null) {
            // create new instance
            FAKE_PING_ANALYZER_INSTANCE = FakePingAnalyzer.create(ipLayerInfo, executorService, eventBus);
        }
        return FAKE_PING_ANALYZER_INSTANCE;
    }

    private static PingAnalyzer getRealInstance(IPLayerInfo ipLayerInfo,
                                                    ExecutorService executorService,
                                                    DobbyEventBus eventBus) {
        if (PING_ANALYZER_INSTANCE ==  null) {
            // create new instance
            PING_ANALYZER_INSTANCE = PingAnalyzer.create(ipLayerInfo, executorService, eventBus);
        }
        return PING_ANALYZER_INSTANCE;
    }
}
