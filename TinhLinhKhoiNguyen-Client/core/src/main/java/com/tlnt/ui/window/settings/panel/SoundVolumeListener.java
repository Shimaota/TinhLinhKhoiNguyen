package com.tlnt.ui.window.settings.panel;

import com.tlnt.core.audio.AudioManager;
import com.tlnt.ui.window.charinfo.Slider;

public final class SoundVolumeListener implements Slider.SliderChangeListener {
   public com.tlnt.ui.window.settings.panel.SettingsMainPanel settingsPanel;
   public SoundVolumeListener(SettingsMainPanel panel) {
      super();
      this.settingsPanel = panel;
   }

   @Override
   public final void onValueChanged(float value) {
      AudioManager.setSoundVolume(value);
   }
}
