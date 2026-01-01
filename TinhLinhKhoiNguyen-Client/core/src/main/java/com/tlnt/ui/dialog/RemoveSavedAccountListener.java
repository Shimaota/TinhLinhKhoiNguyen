package com.tlnt.ui.dialog;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.Settings;

public final class RemoveSavedAccountListener extends ClickListener {
   public com.tlnt.ui.dialog.LoginDialog loginDialog;
   public com.tlnt.ui.dialog.LoginDialog.DeleteSavedAccountButton savedAccountButton;
   public RemoveSavedAccountListener(LoginDialog.DeleteSavedAccountButton savedAccountButton, LoginDialog loginDialog) {
      super();
      this.savedAccountButton = savedAccountButton;
      this.loginDialog = loginDialog;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (this.savedAccountButton.accountInfo != null) {
         Settings.removeSavedAccount(this.savedAccountButton.accountInfo);
         this.savedAccountButton.loginDialog.onClose2();
      }
   }
}
