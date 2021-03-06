package com.inceptai.expertchat;

import android.app.Application;
import android.content.res.Configuration;

import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowManager;

/**
 * Created by arunesh on 6/9/17.
 */

public class ExpertChatApplication extends Application {
    ExpertChatService service;
    @Override
    public void onCreate() {
        super.onCreate();
        service = ExpertChatService.fetchInstance(getApplicationContext());
        FlowManager.init(new FlowConfig.Builder(this).build());
        ExpertChatThreadpool.get();
    }
}
