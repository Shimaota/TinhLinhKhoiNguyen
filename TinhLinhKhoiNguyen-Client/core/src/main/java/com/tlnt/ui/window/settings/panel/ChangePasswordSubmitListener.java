package com.tlnt.ui.window.settings.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class ChangePasswordSubmitListener extends ClickListener {
   public com.tlnt.ui.window.settings.panel.ChangeSecondaryPasswordPanel changePasswordPanel;
   public ChangePasswordSubmitListener(ChangeSecondaryPasswordPanel panel) {
      super();
      this.changePasswordPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.changePasswordPanel.submitChangePassword();
   }
}
