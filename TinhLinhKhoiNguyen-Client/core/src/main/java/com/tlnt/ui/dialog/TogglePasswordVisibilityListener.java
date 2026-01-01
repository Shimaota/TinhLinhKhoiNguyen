package com.tlnt.ui.dialog;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class TogglePasswordVisibilityListener extends ClickListener {
   public com.tlnt.ui.dialog.LoginDialog loginDialog;
   public TogglePasswordVisibilityListener(LoginDialog loginDialog) {
      super();
      this.loginDialog = loginDialog;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.loginDialog.loginFormTable.setVisible(true);
      this.loginDialog.savedAccountsTable.setVisible(false);
   }
}
