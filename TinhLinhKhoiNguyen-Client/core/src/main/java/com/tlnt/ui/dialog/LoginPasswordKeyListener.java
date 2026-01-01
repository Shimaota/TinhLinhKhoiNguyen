package com.tlnt.ui.dialog;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;

public final class LoginPasswordKeyListener extends InputListener {
   public com.tlnt.ui.dialog.LoginDialog loginDialog;
   public LoginPasswordKeyListener(LoginDialog loginDialog) {
      super();
      this.loginDialog = loginDialog;
   }

   @Override
   public final boolean keyUp(InputEvent event, int keycode) {
      if (keycode == 66 || keycode == 160) {
         this.loginDialog.submitLogin();
      }

      return super.keyUp(event, keycode);
   }
}
