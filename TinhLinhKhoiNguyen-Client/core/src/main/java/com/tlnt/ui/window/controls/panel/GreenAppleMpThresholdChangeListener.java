package com.tlnt.ui.window.controls.panel;

import com.tlnt.core.Settings;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.window.charinfo.AmountStepper;

public final class GreenAppleMpThresholdChangeListener implements AmountStepper.ValueChangeListener {
   public com.tlnt.ui.window.controls.panel.AutoPlaySettingsPanel panel;
   public GreenAppleMpThresholdChangeListener(AutoPlaySettingsPanel panel) {
      super();
      this.panel = panel;
   }

   @Override
   public final void onValueChanged(float value) {
      Settings.autoMpThresholdPercent = (int)value;
      ServerPacketSender.getInstance().sendGameSettings();
   }
}
