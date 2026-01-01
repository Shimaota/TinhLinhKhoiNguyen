package com.tlnt.ui.window.controls.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.Settings;
import com.tlnt.net.task.ServerPacketSender;

public final class AutoReviveClickListener extends ClickListener {
   public com.tlnt.ui.window.controls.panel.AutoPlaySettingsPanel panel;
   public AutoReviveClickListener(AutoPlaySettingsPanel panel) {
      super();
      this.panel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      Settings.autoRevive = this.panel.autoReviveCheckbox.isChecked();
      ServerPacketSender.getInstance().sendGameSettings();
   }
}
