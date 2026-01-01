package com.tlnt.ios;

import org.robovm.apple.uikit.UIApplication;
import org.robovm.apple.uikit.UIScene;
import org.robovm.apple.uikit.UISceneConnectionOptions;
import org.robovm.apple.uikit.UISceneSession;
import org.robovm.apple.uikit.UIWindow;
import org.robovm.apple.uikit.UIWindowScene;
import org.robovm.apple.uikit.UIWindowSceneDelegateAdapter;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.iosrobovm.IOSApplication;

/**
 * iOS 27 requires the UIScene lifecycle (apps that do not declare scenes are closed right after launch). libGDX 1.12.1 does not support scenes yet:
 * start libGDX here (IOSLauncher.start, when the landscape orientation is already known) and attach the UIWindow it creates to the UIWindowScene.
 * When scenes are used, UIKit no longer sends didBecomeActive / willResignActive... to the app delegate -> forward them to
 * IOSApplication.Delegate so libGDX still pauses / resumes correctly.
 */
public class IOSSceneDelegate extends UIWindowSceneDelegateAdapter {
    @Override
    public void willConnect(UIScene scene, UISceneSession session, UISceneConnectionOptions connectionOptions) {
        if (!(scene instanceof UIWindowScene)) return;
        UIApplication application = UIApplication.getSharedApplication();
        if (application.getDelegate() instanceof IOSLauncher) ((IOSLauncher) application.getDelegate()).start(application, (UIWindowScene) scene);
        if (!(Gdx.app instanceof IOSApplication)) return;
        UIWindow window = ((IOSApplication) Gdx.app).getUIWindow();
        window.setWindowScene((UIWindowScene) scene);
        setWindow(window);
        window.makeKeyAndVisible();
    }

    @Override
    public void sceneDidBecomeActive(UIScene scene) {
        IOSApplication.Delegate d = appDelegate();
        if (d != null) d.didBecomeActive(UIApplication.getSharedApplication());
    }

    @Override
    public void sceneWillResignActive(UIScene scene) {
        IOSApplication.Delegate d = appDelegate();
        if (d != null) d.willResignActive(UIApplication.getSharedApplication());
    }

    @Override
    public void sceneWillEnterForeground(UIScene scene) {
        IOSApplication.Delegate d = appDelegate();
        if (d != null) d.willEnterForeground(UIApplication.getSharedApplication());
    }

    private static IOSApplication.Delegate appDelegate() {
        Object d = UIApplication.getSharedApplication().getDelegate();
        // Before start, IOSApplication.Delegate has no app yet -> calling into it throws a NullPointerException.
        return d instanceof IOSLauncher && ((IOSLauncher) d).isStarted() ? (IOSApplication.Delegate) d : null;
    }
}
