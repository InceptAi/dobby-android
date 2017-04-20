package com.inceptai.dobby.ai;

import android.util.Log;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.NetworkLayer;
import com.inceptai.dobby.speedtest.BandwidthObserver;
import com.inceptai.dobby.speedtest.BandwithTestCodes;

import java.util.List;

import static com.inceptai.dobby.DobbyApplication.TAG;

/**
 * Created by arunesh on 4/19/17.
 */

public class BandwidthOperation extends ComposableOperation {
    private static final String BANDWIDTH_OP_NAME = "Bandwidth test operation";
    private DobbyThreadpool threadpool;
    private NetworkLayer networkLayer;
    private InferenceEngine inferenceEngine;
    private DobbyAi.ResponseCallback responseCallback;
    private ListenableFuture<?> future;

    public BandwidthOperation(DobbyThreadpool threadpool, NetworkLayer networkLayer,
                              InferenceEngine inferenceEngine, DobbyAi.ResponseCallback responseCallback) {
        super(threadpool);
        this.threadpool = threadpool;
        this.networkLayer = networkLayer;
        this.inferenceEngine = inferenceEngine;
        this.responseCallback = responseCallback;
    }

    @Override
    protected String getName() {
        return BANDWIDTH_OP_NAME;
    }

    @Override
    public void post() {
        Log.i(TAG, "Going to start bandwidth test.");
        @BandwithTestCodes.TestMode
        int testMode = BandwithTestCodes.TestMode.DOWNLOAD_AND_UPLOAD;
        BandwidthObserver observer = networkLayer.startBandwidthTest(testMode);
        if (!observer.startedNormally()) {
            Log.v(TAG, "Bandwidth tests did not start normally: " + observer.exceptionCode());
            return;
        }
        Log.v(TAG, "Started bw test successfully");
        observer.setInferenceEngine(inferenceEngine);
        responseCallback.showRtGraph(observer);
        future = observer.asFuture();
        setFuture(future);
    }
}
