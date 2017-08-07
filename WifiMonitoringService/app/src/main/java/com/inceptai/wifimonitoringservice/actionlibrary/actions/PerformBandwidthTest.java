package com.inceptai.wifimonitoringservice.actionlibrary.actions;

import android.content.Context;

import com.inceptai.wifimonitoringservice.R;
import com.inceptai.wifimonitoringservice.actionlibrary.NetworkLayer.NetworkActionLayer;
import com.inceptai.wifimonitoringservice.actionlibrary.utils.ActionLibraryCodes;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import io.reactivex.Observable;

/**
 * Created by vivek on 7/5/17.
 */

public class PerformBandwidthTest extends ObservableAction {
    @ActionLibraryCodes.BandwidthTestMode int mode;
    public PerformBandwidthTest(Context context,
                                Executor executor,
                                ScheduledExecutorService scheduledExecutorService,
                                NetworkActionLayer networkActionLayer,
                                long actionTimeOutMs,
                                @ActionLibraryCodes.BandwidthTestMode int mode) {
        super(ActionType.PERFORM_BANDWIDTH_TEST, context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        this.mode = mode;
    }

    public PerformBandwidthTest(Context context,
                                Executor executor,
                                ScheduledExecutorService scheduledExecutorService,
                                NetworkActionLayer networkActionLayer,
                                long actionTimeOutMs,
                                int maxTests,
                                long gapBetweenChecksMs,
                                @ActionLibraryCodes.BandwidthTestMode int mode) {
        super(ActionType.PERFORM_BANDWIDTH_TEST, context, executor, scheduledExecutorService, networkActionLayer, actionTimeOutMs);
        this.mode = mode;
    }

    @Override
    public void start() {
        setObservable(networkActionLayer.startBandwidthTest(mode));
    }

    @Override
    public String getName() {
        return context.getString(R.string.perform_bandwidth_test);
    }

}
