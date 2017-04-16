package com.inceptai.dobby.connectivity;

import android.content.Context;

import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.eventbus.DobbyEventBus;
import com.inceptai.dobby.fake.FakeConnectivityAnalyzer;

import static com.inceptai.dobby.DobbyApplication.USE_FAKES;

/**
 * Created by arunesh on 4/10/17.
 */

public class ConnectivityAnalyzerFactory {

    private static FakeConnectivityAnalyzer FAKE_CONNECTIVITY_ANALYZER_INSTANCE = null;
    private static ConnectivityAnalyzer CONNECTIIVITY_ANALYZER_INSTANCE = null;
    private ConnectivityAnalyzerFactory() {}

    private static ConnectivityAnalyzer create(Context context,
                                               DobbyThreadpool threadpool,
                                               DobbyEventBus eventBus) {
        if (USE_FAKES.get()) {
            return FakeConnectivityAnalyzer.create(context, threadpool, eventBus);
        } else {
            return ConnectivityAnalyzer.create(context, threadpool, eventBus);
        }
    }

    public static ConnectivityAnalyzer getConnecitivityAnalyzer(Context context,
                                                                DobbyThreadpool threadpool,
                                                                DobbyEventBus eventBus) {
        if (USE_FAKES.get()) {
            return getFakeInstance(context, threadpool, eventBus);
        } else {
            // return real instance.
            return getRealInstance(context, threadpool, eventBus);
        }
    }

    private static FakeConnectivityAnalyzer getFakeInstance(Context context,
                                                            DobbyThreadpool threadpool,
                                                            DobbyEventBus eventBus) {
        if (FAKE_CONNECTIVITY_ANALYZER_INSTANCE ==  null) {
            FAKE_CONNECTIVITY_ANALYZER_INSTANCE = FakeConnectivityAnalyzer.create(context, threadpool, eventBus);
        }
        return FAKE_CONNECTIVITY_ANALYZER_INSTANCE;
    }

    private static ConnectivityAnalyzer getRealInstance(Context context,
                                                        DobbyThreadpool threadpool,
                                                        DobbyEventBus eventBus) {
        if (CONNECTIIVITY_ANALYZER_INSTANCE ==  null) {
            CONNECTIIVITY_ANALYZER_INSTANCE = ConnectivityAnalyzer.create(context, threadpool, eventBus);
        }
        return CONNECTIIVITY_ANALYZER_INSTANCE;
    }
}
