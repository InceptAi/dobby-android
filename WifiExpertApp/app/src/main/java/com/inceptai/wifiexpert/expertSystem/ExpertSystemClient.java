package com.inceptai.wifiexpert.expertSystem;

import com.inceptai.wifiexpert.expert.ExpertChat;
import com.inceptai.wifiexpert.expert.ExpertChatService;
import com.inceptai.wifimonitoringservice.ExpertAction;

import io.reactivex.Observable;

/**
 * Created by vivek on 8/4/17.
 */

public class ExpertSystemClient implements
        ExpertSystemService.Callback,
        ExpertChatService.ChatCallback {

    ExpertSystemService expertSystemService;

    public ExpertSystemClient() {

    }

    // Connect to the expert system service.
    public void connect() {
        expertSystemService = new ExpertSystemService(this);
    }

    public onUserMessage(UserMessage userMessage) {
        expertSystemService.onUserMessage();
    }

    public Observable<ExpertAction> getActionObservable() {

    }

    //Expert system service callbacks

    @Override
    public void onActionRequested(ExpertAction.ActionRequest actionRequest) {

    }

    @Override
    public void onExpertMessage(ExpertMessage expertMessage) {

    }

    //Expert chat service callbacks
    @Override
    public void onMessageAvailable(ExpertChat expertChat) {

    }

    @Override
    public void onNoHistoryAvailable() {

    }

    @Override
    public void onEtaUpdated(long newEtaSeconds, boolean isPresent) {

    }

    @Override
    public void onEtaAvailable(long newEtaSeconds, boolean isPresent) {

    }
}
