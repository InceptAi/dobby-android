package com.inceptai.wifiexpert.expertSystem;

import com.inceptai.wifiexpert.expert.ExpertChat;
import com.inceptai.wifiexpert.expert.ExpertChatService;
import com.inceptai.wifiexpert.expertSystem.inferencing.DobbyAi;
import com.inceptai.wifiexpert.expertSystem.inferencing.InferenceEngine;
import com.inceptai.wifimonitoringservice.ExpertAction;

import java.util.List;

/**
 * Created by vivek on 8/5/17.
 */

public class ExpertSystemService implements ExpertChatService.ChatCallback {
    private DobbyAi dobbyAi;
    private InferenceEngine inferenceEngine;

    public interface Callback {
        void onActionRequested(ExpertAction.ActionRequest actionRequest);
        void onExpertMessage(ExpertMessage expertMessage);
    }

    //Dobby Ai, InferenceEngine.
    ExpertSystemService(ExpertSystemService.Callback callback) {

    }

    public void configure(List<ExpertAction> expertActionList,) {

    }

    public void registerAction(ExpertAction expertAction) {

    }

    //Expert chat service callback
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
