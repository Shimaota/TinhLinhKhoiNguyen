package com.tlnt.ui.window.settings.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class SmallScreenCheckboxListener extends ClickListener {
   public com.tlnt.ui.window.settings.panel.SettingsMainPanel settingsPanel;
   public SmallScreenCheckboxListener(SettingsMainPanel panel) {
      super();
      this.settingsPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (this.settingsPanel.smallScreenCheckbox.isChecked()) {
         this.settingsPanel.fullscreenCheckbox.setChecked(false);
      }

      this.settingsPanel.applyDisplaySettings();
   }
}
