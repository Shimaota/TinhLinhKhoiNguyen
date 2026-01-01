package com.tlnt.android;

import android.app.Activity;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.PopupWindow;

import com.tlnt.ui.widget.KeyboardInsets;

/**
 * Measures the soft keyboard height -> KeyboardInsets (core shifts the UI up when the focused field is covered).
 * The Activity uses adjustNothing + fullscreen, so it cannot measure this itself; we use a PopupWindow 0 px wide and full
 * screen height in ADJUST_RESIZE mode: when the keyboard shows the popup shrinks, and the shrink = keyboard height (the libGDX 1.13 approach).
 */
final class KeyboardHeightProvider extends PopupWindow implements ViewTreeObserver.OnGlobalLayoutListener {
    private final Activity activity;
    private final View popupView;
    private final Rect visible = new Rect();

    KeyboardHeightProvider(Activity activity) {
        super(activity);
        this.activity = activity;
        popupView = new View(activity);
        setContentView(popupView);
        setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE | WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
        setWidth(0);
        setHeight(ViewGroup.LayoutParams.MATCH_PARENT);
        setBackgroundDrawable(new ColorDrawable(0));
        setFocusable(false);
        setTouchable(false);
        popupView.getViewTreeObserver().addOnGlobalLayoutListener(this);
    }

    /** Call from onResume: the Activity window must be attached before the popup can be shown. */
    void start() {
        final View parent = activity.getWindow().getDecorView();
        parent.post(new Runnable() {
            @Override
            public void run() {
                if (!isShowing() && parent.getWindowToken() != null && !activity.isFinishing()) {
                    showAtLocation(parent, Gravity.NO_GRAVITY, 0, 0);
                }
            }
        });
    }

    void stop() {
        KeyboardInsets.setHeightFraction(0f);
        if (isShowing()) dismiss();
    }

    @Override
    public void onGlobalLayout() {
        int screenHeight = activity.getWindow().getDecorView().getHeight();
        if (screenHeight <= 0) return;
        popupView.getWindowVisibleDisplayFrame(visible);
        int keyboard = screenHeight - visible.bottom;
        // Navigation bar / display cutout also take away a few %: a real keyboard is always > 15% of the height (~40-50% in landscape).
        float fraction = keyboard / (float) screenHeight;
        KeyboardInsets.setHeightFraction(fraction > 0.15f ? fraction : 0f);
    }
}
