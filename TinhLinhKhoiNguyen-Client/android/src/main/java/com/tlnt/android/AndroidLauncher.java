package com.tlnt.android;

import android.os.Bundle;
import android.view.WindowManager;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.tlnt.core.GameMain;

/** Android entry point. Mirrors the desktop launcher's back-buffer config (8888, 16-bit depth, 8-bit stencil). */
public class AndroidLauncher extends AndroidApplication {
    private KeyboardHeightProvider keyboardHeight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.r = 8;
        config.g = 8;
        config.b = 8;
        config.a = 8;
        config.depth = 16;
        config.stencil = 8;
        config.useImmersiveMode = true;
        config.useAccelerometer = false;
        config.useCompass = false;
        config.useGyroscope = false;
        config.useWakelock = true;
        initialize(new GameMain(), config);
        keyboardHeight = new KeyboardHeightProvider(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        keyboardHeight.start();
    }

    @Override
    protected void onPause() {
        keyboardHeight.stop();
        super.onPause();
    }
}
