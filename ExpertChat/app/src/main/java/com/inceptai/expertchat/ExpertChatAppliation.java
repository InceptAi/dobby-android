package com.inceptai.expertchat;

import android.app.Application;
import android.content.res.Configuration;

/**
 * Created by arunesh on 6/9/17.
 */

public class ExpertChatAppliation extends Application {
    ExpertChatService service;
    @Override
    public void onCreate() {
        super.onCreate();
        service = ExpertChatService.fetchInstance(getApplicationContext());
    }
}
