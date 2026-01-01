package com.tlnt.ui.window.settings.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameState;

public final class SecPasswordButtonListener extends ClickListener {
   public com.tlnt.ui.window.settings.panel.SettingsMainPanel settingsPanel;
   public SecPasswordButtonListener(SettingsMainPanel panel) {
      super();
      this.settingsPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.settingsPanel.settingsWindow.showPanel(GameState.hasSecondaryPassword ? AccountProtectionPanel.class : CreateSecondaryPasswordPanel.class);
   }
}
