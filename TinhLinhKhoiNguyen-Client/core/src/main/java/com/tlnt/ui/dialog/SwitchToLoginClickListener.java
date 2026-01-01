package com.tlnt.ui.dialog;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.ui.window.WindowType;

public final class SwitchToLoginClickListener extends ClickListener {
   public com.tlnt.ui.dialog.RegisterDialog registerDialog;
   public SwitchToLoginClickListener(RegisterDialog registerDialog) {
      super();
      this.registerDialog = registerDialog;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      GameMain.instance.getGameScreen().active.showDialog(WindowType.LOGIN_PANEL);
   }
}
