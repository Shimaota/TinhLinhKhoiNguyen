package com.tlnt.ui.window.settings.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class LockAccountListener extends ClickListener {
   public com.tlnt.ui.window.settings.panel.AccountProtectionPanel protectionPanel;
   public LockAccountListener(AccountProtectionPanel panel) {
      super();
      this.protectionPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().sendForgotPassword();
   }
}
