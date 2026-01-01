package com.tlnt.ui.dialog;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;

public final class LoginUsernameKeyListener extends InputListener {
   public com.tlnt.ui.dialog.LoginDialog loginDialog;
   public LoginUsernameKeyListener(LoginDialog loginDialog) {
      super();
      this.loginDialog = loginDialog;
   }

   @Override
   public final boolean keyUp(InputEvent event, int keycode) {
      if (keycode != 66) {
         if (keycode != 160) {
            return super.keyUp(event, keycode);
         }
      }

      this.loginDialog.submitLogin();
      return super.keyUp(event, keycode);
   }
}
