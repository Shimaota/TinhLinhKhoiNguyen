package com.tlnt.ui.dialog;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class LoadSavedAccountClickListener extends ClickListener {
   public com.tlnt.ui.dialog.LoginDialog loginDialog;
   public com.tlnt.ui.dialog.LoginDialog.AccountNameButton accountNameButton;
   public LoadSavedAccountClickListener(LoginDialog.AccountNameButton accountNameButton, LoginDialog loginDialog) {
      super();
      this.accountNameButton = accountNameButton;
      this.loginDialog = loginDialog;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.accountNameButton.loginDialog.selectSavedAccount(this.accountNameButton.account);
   }
}
