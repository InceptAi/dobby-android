package com.inceptai.dobby.ping;

import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.fake.FakePingAnalyzer;
import com.inceptai.dobby.model.IPLayerInfo;

import static com.inceptai.dobby.DobbyApplication.USE_FAKES;

/**
 * Created by arunesh on 4/10/17.
 */

public class PingAnalyzerFactory {

    private static FakePingAnalyzer INSTANCE = null;
    private PingAnalyzerFactory() {}

    public static PingAnalyzer create(IPLayerInfo ipLayerInfo,
                                      DobbyThreadpool dobbyThreadpool,
                                      DobbyEventBus eventBus) {
        if (USE_FAKES.get()) {
            return FakePingAnalyzer.create(ipLayerInfo, dobbyThreadpool, eventBus);
        } else {
            return PingAnalyzer.create(ipLayerInfo, dobbyThreadpool, eventBus);
        }
    }

    public static PingAnalyzer getPingAnalyzer() {
        if (USE_FAKES.get()) {
            return getFakeInstance();
        } else {
            // return real instance.
            return null;
        }
    }

    private static FakePingAnalyzer getFakeInstance() {
        if (INSTANCE ==  null) {
            // create new instance
        }
        return INSTANCE;
    }
}
