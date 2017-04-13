package com.inceptai.dobby.ping;

import com.inceptai.dobby.DobbyApplication;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.fake.FakePingAnalyzer;
import com.inceptai.dobby.model.IPLayerInfo;

/**
 * Created by arunesh on 4/10/17.
 */

public class PingAnalyzerFactory {
    private PingAnalyzerFactory() {}

    public static PingAnalyzer create(IPLayerInfo ipLayerInfo,
                                      DobbyThreadpool dobbyThreadpool,
                                      DobbyEventBus eventBus) {
        if (DobbyApplication.USE_FAKES.get()) {
            return FakePingAnalyzer.create(ipLayerInfo, dobbyThreadpool, eventBus);
        } else {
            return PingAnalyzer.create(ipLayerInfo, dobbyThreadpool, eventBus);
        }
    }
}
