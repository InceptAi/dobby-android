package com.inceptai.wifiexpert.dagger;


import com.inceptai.wifiexpert.DobbyActivity;
import com.inceptai.wifiexpert.UserInteractionManager;
import com.inceptai.wifiexpert.expert.ExpertChatService;
import com.inceptai.wifiexpert.heartbeat.AlarmReceiver;
import com.inceptai.wifiexpert.heartbeat.DeviceBootReceiver;
import com.inceptai.wifiexpert.notifications.DobbyMessagingService;
import com.inceptai.wifiexpert.notifications.FirebaseIdService;
import com.inceptai.wifiexpert.ui.ChatFragment;
import com.inceptai.wifiexpert.ui.WifiExpertDialogFragment;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Production dagger component.
 */
@Singleton
@Component(modules={ProdModule.class})
public interface ProdComponent {

    void inject(DobbyActivity dobbyActivity);

    void inject(ChatFragment chatFragment);

    void inject(WifiExpertDialogFragment wifiExpertDialogFragment);

    void inject(AlarmReceiver alarmReceiver);

    void inject(DeviceBootReceiver deviceBootReceiver);

    void inject(DobbyMessagingService dobbyMessagingService);

    void inject(ExpertChatService expertChatService);

    void inject(FirebaseIdService firebaseIdService);

    void inject(UserInteractionManager userInteractionManager);

}
