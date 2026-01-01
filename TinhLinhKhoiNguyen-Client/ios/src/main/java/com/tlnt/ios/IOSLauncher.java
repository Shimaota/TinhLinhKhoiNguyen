package com.tlnt.ios;

import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.glkit.GLKViewDrawableStencilFormat;
import org.robovm.apple.uikit.UIApplication;
import org.robovm.apple.uikit.UIApplicationLaunchOptions;
import org.robovm.apple.uikit.UIKeyboardAnimation;
import org.robovm.apple.uikit.UISceneConfiguration;
import org.robovm.apple.uikit.UISceneConnectionOptions;
import org.robovm.apple.uikit.UISceneSession;
import org.robovm.apple.uikit.UIScreen;
import org.robovm.apple.uikit.UIWindow;
import org.robovm.apple.uikit.UIWindowScene;
import org.robovm.objc.block.VoidBlock1;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.iosrobovm.IOSApplication;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.backends.iosrobovm.IOSApplicationConfiguration;
import com.badlogic.gdx.backends.iosrobovm.IOSAudio;
import com.badlogic.gdx.backends.iosrobovm.IOSInput;
import com.tlnt.core.GameMain;
import com.tlnt.ui.widget.KeyboardInsets;
import com.tlnt.util.AesDecryptor;

/** iOS entry point (MobiVM/RoboVM). Build requires macOS + Xcode. */
public class IOSLauncher extends IOSApplication.Delegate {
    @Override
    protected IOSApplication createApplication() {
        IOSApplicationConfiguration config = new IOSApplicationConfiguration();
        config.orientationLandscape = true;
        config.orientationPortrait = false;
        config.preventScreenDimming = true;
        config.useAccelerometer = false;
        config.useCompass = false;
        config.statusBarVisible = false;
        config.hideHomeIndicator = true;
        // By default libGDX mutes audio when the iPhone is in silent mode; the game plays sound like the Android/PC builds.
        config.overrideRingerSwitch = true;
        // 8-bit stencil like the Android / PC builds: skill slots clip icons to a circle with the stencil (SkillButton). By default
        // iOS has no stencil buffer -> skill icons spill outside the slot.
        config.stencilFormat = GLKViewDrawableStencilFormat._8;
        return new IOSApplication(new GameMain(), config) {
            // libGDX calls createInput right after creating the UIWindow, before GameMain.create: attach the window to the scene here,
            // otherwise a window without a scene is treated as portrait and the game locks in the wrong viewport (side bars).
            @Override
            protected IOSInput createInput() {
                if (windowScene != null) getUIWindow().setWindowScene(windowScene);
                return super.createInput();
            }

            @Override
            protected IOSAudio createAudio(IOSApplicationConfiguration config) {
                return new OggToM4aAudio(super.createAudio(config));
            }
        };
    }

    private UIApplicationLaunchOptions launchOptions;
    private UIWindowScene windowScene;
    private boolean started;

    // With UIScene, a UIWindow not attached to a scene is treated as portrait (430x932): GameMain.create reads that size
    // (WorldConfig locks the viewport once) -> the game gets side bars. Defer starting libGDX until
    // IOSSceneDelegate.willConnect -> start(scene); createInput attaches the window to the scene before GameMain.create.
    @Override
    public boolean didFinishLaunching(UIApplication application, UIApplicationLaunchOptions launchOptions) {
        this.launchOptions = launchOptions;
        return true;
    }

    boolean isStarted() {
        return started;
    }

    void start(UIApplication application, UIWindowScene scene) {
        if (started) return;
        started = true;
        windowScene = scene;
        AesDecryptor.backend = new JavaxAesBackend();
        super.didFinishLaunching(application, launchOptions);
        // Soft keyboard height -> KeyboardInsets (core shifts the UI up when the focused field is covered). Hiding the keyboard also fires
        // KeyboardWillChangeFrame (frame moves off screen -> 0).
        UIWindow.Notifications.observeKeyboardWillChangeFrame(new VoidBlock1<UIKeyboardAnimation>() {
            @Override
            public void invoke(UIKeyboardAnimation animation) {
                double screenHeight = UIScreen.getMainScreen().getBounds().getHeight();
                CGRect end = animation.getEndFrame();
                if (screenHeight <= 0 || end == null) return;
                KeyboardInsets.setHeightFraction((float) ((screenHeight - end.getMinY()) / screenHeight));
            }
        });
        // Older builds stored downloaded data under Documents/private/var/.../<old container id>/... (see FileStorage.getStorageRoot, iOS case):
        // no longer reachable -> delete it to free device storage.
        new Thread(() -> {
            FileHandle stale = Gdx.files.external("private");
            if (stale.exists()) stale.deleteDirectory();
        }, "cleanup-stale-data").start();
    }

    // UIScene lifecycle (mandatory since iOS 27): every scene uses IOSSceneDelegate. Info.plist declares
    // UIApplicationSceneManifest without a class name (the ObjC class name is generated by RoboVM) -> return the configuration here.
    @Override
    public UISceneConfiguration getConfigurationForConnectingSceneSession(UIApplication application,
            UISceneSession connectingSceneSession, UISceneConnectionOptions options) {
        UISceneConfiguration config = new UISceneConfiguration("Default", connectingSceneSession.getRole());
        config.setDelegateClass(IOSSceneDelegate.class);
        return config;
    }

    public static void main(String[] argv) {
        NSAutoreleasePool pool = new NSAutoreleasePool();
        UIApplication.main(argv, null, IOSLauncher.class);
        pool.close();
    }
}
