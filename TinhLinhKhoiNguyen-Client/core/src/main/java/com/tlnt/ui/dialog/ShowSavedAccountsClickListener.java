package com.tlnt.ui.dialog;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class ShowSavedAccountsClickListener extends ClickListener {
   public com.tlnt.ui.dialog.LoginDialog loginDialog;
   public ShowSavedAccountsClickListener(LoginDialog loginDialog) {
      super();
      this.loginDialog = loginDialog;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.loginDialog.savedAccountsTable.setVisible(true);
      this.loginDialog.loginFormTable.setVisible(false);
   }
}
