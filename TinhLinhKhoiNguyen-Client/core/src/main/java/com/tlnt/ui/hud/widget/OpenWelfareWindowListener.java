package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.ui.window.WindowType;

public final class OpenWelfareWindowListener extends ClickListener {
   public com.tlnt.ui.hud.widget.TargetPortrait targetPortrait;
   public OpenWelfareWindowListener(TargetPortrait targetPortrait) {
      super();
      this.targetPortrait = targetPortrait;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      GameMain.instance.getLoginScreen().debugScrollPane.showWindow(WindowType.GENERIC_POPUP);
   }
}
