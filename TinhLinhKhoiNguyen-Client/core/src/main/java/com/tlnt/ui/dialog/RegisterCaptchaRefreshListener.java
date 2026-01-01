package com.tlnt.ui.dialog;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class RegisterCaptchaRefreshListener extends ClickListener {
   public com.tlnt.ui.dialog.RegisterDialog registerDialog;
   public RegisterCaptchaRefreshListener(RegisterDialog registerDialog) {
      super();
      this.registerDialog = registerDialog;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.registerDialog.submitRegister();
   }
}
