package com.tlnt.ui.window.settings.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.Settings;
import com.tlnt.net.task.ServerPacketSender;

public final class HideOtherPlayersCheckboxListener extends ClickListener {
   public com.tlnt.ui.window.settings.panel.SettingsMainPanel settingsPanel;
   public HideOtherPlayersCheckboxListener(SettingsMainPanel panel) {
      super();
      this.settingsPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      Settings.hideOtherPlayers = this.settingsPanel.hideOtherPlayersCheckbox.isChecked();
      ServerPacketSender.getInstance().sendGameSettings();
   }
}
