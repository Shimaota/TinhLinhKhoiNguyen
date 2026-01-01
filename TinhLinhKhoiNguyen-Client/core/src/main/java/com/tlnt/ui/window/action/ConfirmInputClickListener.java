package com.tlnt.ui.window.action;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class ConfirmInputClickListener extends ClickListener {
   public com.tlnt.ui.window.action.QuantityInputDialog dialog;
   public ConfirmInputClickListener(QuantityInputDialog dialog) {
      super();
      this.dialog = dialog;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.dialog.confirmInput();
   }
}
