package com.tlnt.ui.dialog;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class RefreshCaptchaClickListener extends ClickListener {
   public com.tlnt.ui.dialog.RegisterDialog registerDialog;
   public RefreshCaptchaClickListener(RegisterDialog registerDialog) {
      super();
      this.registerDialog = registerDialog;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().sendCreateCharacter();
   }
}
