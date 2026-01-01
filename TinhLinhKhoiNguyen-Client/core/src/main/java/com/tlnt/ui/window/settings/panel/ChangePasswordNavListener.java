package com.tlnt.ui.window.settings.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class ChangePasswordNavListener extends ClickListener {
   public com.tlnt.ui.window.settings.panel.AccountProtectionPanel protectionPanel;
   public ChangePasswordNavListener(AccountProtectionPanel panel) {
      super();
      this.protectionPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.protectionPanel.settingsWindow.showPanel(ChangeSecondaryPasswordPanel.class);
   }
}
