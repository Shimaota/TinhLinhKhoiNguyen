package com.tlnt.ui.dialog;

import com.badlogic.gdx.utils.Pool;

public final class SavedAccountRowPool extends Pool<LoginDialog.SavedAccountRow> {
   public com.tlnt.ui.dialog.LoginDialog loginDialog;
   public SavedAccountRowPool(LoginDialog loginDialog) {
      super();
      this.loginDialog = loginDialog;
   }

   @Override
   public LoginDialog.SavedAccountRow newObject() {
      return (this.loginDialog).new SavedAccountRow(this.loginDialog.renderContext);
   }
}
