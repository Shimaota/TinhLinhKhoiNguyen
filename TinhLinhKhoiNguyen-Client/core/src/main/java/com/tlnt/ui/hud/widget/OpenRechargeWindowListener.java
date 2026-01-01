package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.ui.window.WindowType;

/** First menu button (icon ui/82) -> "Recharge" window (decompiled C3642: RECHARGE_POPUP). */
public final class OpenRechargeWindowListener extends ClickListener {
   @Override
   public final void clicked(InputEvent event, float x, float y) {
      GameMain.instance.getLoginScreen().debugScrollPane.showWindow(WindowType.RECHARGE_POPUP);
      GameMain.instance.getLoginScreen().debugScrollPane.refreshHudState();
   }
}
