package com.tlnt.ui.dialog;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.ui.window.WindowType;

public final class LoginResetDataListener extends ClickListener {
   public com.tlnt.ui.dialog.LoginDialog loginDialog;
   public LoginResetDataListener(LoginDialog loginDialog) {
      super();
      this.loginDialog = loginDialog;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      GameMain.instance.getGameScreen().active.showDialog(WindowType.CLEAR_DATA_PANEL);
   }
}
