package com.inceptai.actionlibrary.actions;

import android.content.Context;
import android.support.annotation.Nullable;

import com.google.common.util.concurrent.ListenableFuture;
import com.inceptai.actionlibrary.ActionResult;
import com.inceptai.actionlibrary.ActionThreadPool;
import com.inceptai.actionlibrary.NetworkLayer.NetworkActionLayer;
import com.inceptai.actionlibrary.R;

/**
 * Created by vivek on 7/5/17.
 */

public class GetNearbyWifiNetworks extends FutureAction {

    public GetNearbyWifiNetworks(Context context, ActionThreadPool threadpool, NetworkActionLayer networkActionLayer, long timeOut) {
        super(context, threadpool, networkActionLayer, timeOut);
    }

    @Override
    public void post() {
        setFuture(networkActionLayer.getNearbyWifiNetworks());
    }

    @Override
    public String getName() {
        return context.getString(R.string.get_nearby_wifi_networks);
    }

    @Override
    public void uponCompletion(FutureAction operation) {
        super.uponCompletion(operation);
    }

    @Override
    protected void setFuture(@Nullable ListenableFuture<?> future) {
        super.setFuture(future);
    }

    @Override
    protected void setResult(ActionResult result) {
        super.setResult(result);
    }

    @Override
    public ListenableFuture<ActionResult> getFuture() {
        return super.getFuture();
    }
}
