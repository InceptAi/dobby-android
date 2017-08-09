package com.inceptai.wifiexpertsystem.dagger;


import com.inceptai.wifiexpertsystem.DobbyActivity;
import com.inceptai.wifiexpertsystem.UserInteractionManager;
import com.inceptai.wifiexpertsystem.expert.ExpertChatService;
import com.inceptai.wifiexpertsystem.heartbeat.AlarmReceiver;
import com.inceptai.wifiexpertsystem.heartbeat.DeviceBootReceiver;
import com.inceptai.wifiexpertsystem.notifications.DobbyMessagingService;
import com.inceptai.wifiexpertsystem.notifications.FirebaseIdService;
import com.inceptai.wifiexpertsystem.ui.ChatFragment;
import com.inceptai.wifiexpertsystem.ui.WifiExpertDialogFragment;

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
