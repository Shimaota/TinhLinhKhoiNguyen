package com.tlnt.ui.window.action;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;

public final class QuantityInputEnterListener extends InputListener {
   public com.tlnt.ui.window.action.QuantityInputDialog dialog;
   public QuantityInputEnterListener(QuantityInputDialog dialog) {
      super();
      this.dialog = dialog;
   }

   @Override
   public final boolean keyDown(InputEvent event, int keycode) {
      if (keycode != 66) {
         if (keycode != 160) {
            return false;
         }
      }

      this.dialog.confirmInput();
      return true;
   }
}
