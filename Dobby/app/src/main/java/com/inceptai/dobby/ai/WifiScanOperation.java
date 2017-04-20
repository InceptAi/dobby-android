package com.inceptai.dobby.ai;

import android.net.wifi.ScanResult;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.inceptai.dobby.DobbyThreadpool;
import com.inceptai.dobby.NetworkLayer;

import java.util.List;

/**
 * Created by arunesh on 4/19/17.
 */

public class WifiScanOperation extends ComposableOperation {
    private NetworkLayer networkLayer;
    private ListenableFuture<List<ScanResult>> wifiScanFuture;

    WifiScanOperation(DobbyThreadpool threadpool, NetworkLayer networkLayer) {
        super(threadpool);
        this.networkLayer = networkLayer;
    }

    @Override
    public void post() {
        wifiScanFuture = networkLayer.wifiScan();
    }

    @Override
    protected void performOperation() {
        // Perform operation is redundant here, since the wifi scan is async.
    }

    @Override
    protected ListenableFuture<?> getFuture() {
        return wifiScanFuture;
    }

    @Override
    protected String getName() {
        return null;
    }

    @Override
    public void postAfter(ComposableOperation operation) {
        super.postAfter(operation);
    }
}
