package com.inceptai.dobby.dagger;

import com.inceptai.dobby.MainActivity;
import com.inceptai.dobby.NetworkLayer;
import com.inceptai.dobby.UserInteractionManager;
import com.inceptai.dobby.actions.ActionTaker;
import com.inceptai.dobby.ai.DobbyAi;
import com.inceptai.dobby.expert.ExpertChatService;
import com.inceptai.dobby.heartbeat.AlarmReceiver;
import com.inceptai.dobby.heartbeat.DeviceBootReceiver;
import com.inceptai.dobby.notifications.FirebaseIdService;
import com.inceptai.dobby.notifications.MyFirebaseMessagingService;
import com.inceptai.dobby.ui.ChatFragment;
import com.inceptai.dobby.ui.DebugFragment;
import com.inceptai.dobby.ui.ExpertChatActivity;
import com.inceptai.dobby.ui.FakeDataFragment;
import com.inceptai.dobby.ui.WifiDocActivity;
import com.inceptai.dobby.ui.WifiDocDialogFragment;
import com.inceptai.dobby.ui.WifiDocMainFragment;
import com.inceptai.dobby.ui.WifiDocOnboardingActivity;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Production dagger component.
 */
@Singleton
@Component(modules={ProdModule.class})
public interface ProdComponent {

    void inject(MainActivity mainActivity);

    void inject(NetworkLayer networkLayer);

    void inject(ChatFragment chatFragment);

    void inject(DebugFragment debugFragment);

    void inject(FakeDataFragment fakeDataFragment);

    void inject(WifiDocDialogFragment wifiDocDialogFragment);

    void inject(WifiDocActivity wifiDocActivity);

    void inject(WifiDocMainFragment wifiDocMainFragment);

    void inject(ObjectRegistry objectRegistry);

    void inject(AlarmReceiver alarmReceiver);

    void inject(DeviceBootReceiver deviceBootReceiver);

    void inject(ExpertChatActivity expertChatActivity);

    void inject(WifiDocOnboardingActivity wifiDocOnboardingActivity);

    void inject(MyFirebaseMessagingService expertChatService);

    void inject(ExpertChatService expertChatService);

    void inject(FirebaseIdService firebaseIdService);

    void inject(UserInteractionManager userInteractionManager);

    void inject(ActionTaker actionTaker);

    void inject(DobbyAi dobbyAi);
}
