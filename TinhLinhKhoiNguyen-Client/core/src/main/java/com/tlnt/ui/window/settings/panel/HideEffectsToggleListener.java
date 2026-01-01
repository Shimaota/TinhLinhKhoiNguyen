package com.tlnt.ui.window.settings.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.Settings;

public final class HideEffectsToggleListener extends ClickListener {
   public com.tlnt.ui.window.settings.panel.SettingsMainPanel settingsPanel;
   public HideEffectsToggleListener(SettingsMainPanel panel) {
      super();
      this.settingsPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      Settings.saveHideEffects(Settings.hideEffects = this.settingsPanel.hideEffectsCheckbox.isChecked());
   }
}
