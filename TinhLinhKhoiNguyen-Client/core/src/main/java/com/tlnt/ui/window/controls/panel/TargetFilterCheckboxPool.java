package com.tlnt.ui.window.controls.panel;

import com.badlogic.gdx.utils.Pool;

public final class TargetFilterCheckboxPool extends Pool<AutoPlaySettingsPanel.TargetFilterCheckbox> {
   public com.tlnt.ui.window.controls.panel.AutoPlaySettingsPanel panel;
   public TargetFilterCheckboxPool(AutoPlaySettingsPanel panel) {
      super();
      this.panel = panel;
   }

   @Override
   public AutoPlaySettingsPanel.TargetFilterCheckbox newObject() {
      return (this.panel).new TargetFilterCheckbox("", this.panel.renderContext);
   }
}
