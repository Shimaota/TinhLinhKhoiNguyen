package com.tlnt.ui.window.settings.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class CreatePasswordSubmitListener extends ClickListener {
   public com.tlnt.ui.window.settings.panel.CreateSecondaryPasswordPanel createPasswordPanel;
   public CreatePasswordSubmitListener(CreateSecondaryPasswordPanel panel) {
      super();
      this.createPasswordPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.createPasswordPanel.submitCreateSecondaryPassword();
   }
}
