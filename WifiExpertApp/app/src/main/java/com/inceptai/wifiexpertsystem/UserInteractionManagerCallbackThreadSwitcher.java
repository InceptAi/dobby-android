package com.inceptai.wifiexpertsystem;

import com.inceptai.wifiexpertsystem.expertSystem.inferencing.DataInterpreter;
import com.inceptai.wifiexpertsystem.expertSystem.inferencing.SuggestionCreator;
import com.inceptai.wifiexpertsystem.expertSystem.messages.StructuredUserResponse;

import java.util.List;
import java.util.concurrent.ExecutorService;

import io.reactivex.Observable;

/**
 * Created by vivek on 4/26/17.
 */

public class UserInteractionManagerCallbackThreadSwitcher implements UserInteractionManager.InteractionCallback {

    private ExecutorService executorService;
    private UserInteractionManager.InteractionCallback resultsCallback;

    private UserInteractionManagerCallbackThreadSwitcher(ExecutorService service, UserInteractionManager.InteractionCallback delegate) {
        this.executorService = service;
        this.resultsCallback = delegate;
    }

    public static UserInteractionManagerCallbackThreadSwitcher wrap(ExecutorService service, UserInteractionManager.InteractionCallback delegate) {
        return new UserInteractionManagerCallbackThreadSwitcher(service, delegate);
    }

    @Override
    public void showBotResponse(final String text) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                resultsCallback.showBotResponse(text);
            }
        });
    }

    @Override
    public void showUserResponse(final String text) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                resultsCallback.showUserResponse(text);
            }
        });
    }

    @Override
    public void showExpertResponse(final String text) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                resultsCallback.showExpertResponse(text);
            }
        });
    }

    @Override
    public void showStatusUpdate(final String text) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                resultsCallback.showExpertResponse(text);
            }
        });
    }

    @Override
    public void showFillerTypingMessage(final String text) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                resultsCallback.showExpertResponse(text);
            }
        });
    }

    @Override
    public void showUserActionOptions(final List<StructuredUserResponse> structuredUserResponses) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                resultsCallback.showUserActionOptions(structuredUserResponses);
            }
        });
    }

    @Override
    public void updateExpertIndicator(String text) {

    }

    @Override
    public void hideExpertIndicator() {

    }

    @Override
    public void observeBandwidth(final Observable bandwidthObservable) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                resultsCallback.observeBandwidth(bandwidthObservable);
            }
        });
    }

    @Override
    public void cancelTestsResponse() {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                resultsCallback.cancelTestsResponse();
            }
        });
    }

    @Override
    public void showBandwidthViewCard(final DataInterpreter.BandwidthGrade bandwidthGrade) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                resultsCallback.showBandwidthViewCard(bandwidthGrade);
            }
        });
    }

    @Override
    public void showNetworkInfoViewCard(final DataInterpreter.WifiGrade wifiGrade, final String isp, final String ip) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                resultsCallback.showNetworkInfoViewCard(wifiGrade, isp, isp);
            }
        });
    }

    @Override
    public void showDetailedSuggestions(final SuggestionCreator.Suggestion suggestion) {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                resultsCallback.showDetailedSuggestions(suggestion);
            }
        });
    }

    @Override
    public void requestAccessibilityPermission() {
        //Don't wrap this one
        requestAccessibilityPermission();
    }
}