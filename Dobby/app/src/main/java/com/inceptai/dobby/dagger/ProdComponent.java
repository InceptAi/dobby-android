package com.inceptai.dobby.dagger;

import com.inceptai.dobby.MainActivity;
import com.inceptai.dobby.NetworkLayer;
import com.inceptai.dobby.ai.DobbyAi;
import com.inceptai.dobby.ui.ChatFragment;
import com.inceptai.dobby.ui.DebugFragment;
import com.inceptai.dobby.ui.FakeDataFragment;
import com.inceptai.dobby.ui.WifiDocActivity;
import com.inceptai.dobby.ui.WifiDocDialogFragment;
import com.inceptai.dobby.ui.WifiDocMainFragment;

import javax.inject.Singleton;

import dagger.Component;

/**
 * Production dagger component.
 */
@Singleton
@Component(modules={ProdModule.class})
public interface ProdComponent {

    void inject(MainActivity mainActivity);

    void inject(DobbyAi dobbyAi);

    void inject(NetworkLayer networkLayer);

    void inject(ChatFragment chatFragment);

    void inject(DebugFragment debugFragment);

    void inject(FakeDataFragment fakeDataFragment);

    void inject(WifiDocDialogFragment wifiDocDialogFragment);

    void inject(WifiDocActivity wifiDocActivity);

    void inject(WifiDocMainFragment wifiDocMainFragment);

    void inject(ObjectRegistry objectRegistry);
}
