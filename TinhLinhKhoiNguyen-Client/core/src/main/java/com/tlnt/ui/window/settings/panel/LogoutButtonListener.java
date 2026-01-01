package com.tlnt.ui.window.settings.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class LogoutButtonListener extends ClickListener {
   public com.tlnt.ui.window.settings.panel.SettingsMainPanel settingsPanel;
   public LogoutButtonListener(SettingsMainPanel panel) {
      super();
      this.settingsPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().disconnect();
   }
}
