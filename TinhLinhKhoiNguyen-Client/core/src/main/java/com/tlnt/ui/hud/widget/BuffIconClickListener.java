package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.ui.window.action.BuffTimerTooltip;

public final class BuffIconClickListener extends ClickListener {
   public com.tlnt.ui.hud.widget.BuffBar.BuffIcon buffIcon;
   public com.tlnt.ui.hud.widget.BuffBar buffBar;
   public BuffIconClickListener(BuffBar.BuffIcon buffIcon, BuffBar buffBar) {
      super();
      this.buffIcon = buffIcon;
      this.buffBar = buffBar;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (this.buffIcon.buffInfo != null) {
         GameMain.instance.getLoginScreen().debugScrollPane.findDialog(BuffTimerTooltip.class).showForBuff(this.buffIcon, this.buffIcon.buffInfo);
      }
   }
}
