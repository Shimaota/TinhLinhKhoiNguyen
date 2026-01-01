package com.tlnt.ui.dialog;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class SaveAccountClickListener extends ClickListener {
   public com.tlnt.ui.dialog.LoginDialog loginDialog;
   public SaveAccountClickListener(LoginDialog loginDialog) {
      super();
      this.loginDialog = loginDialog;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.loginDialog.saveAccountAndRefresh();
   }
}
