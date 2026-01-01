package com.tlnt.ui.window.action;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class CloseInputDialogClickListener extends ClickListener {
   public com.tlnt.ui.window.action.QuantityInputDialog inputDialog;
   public CloseInputDialogClickListener(QuantityInputDialog dialog) {
      super();
      this.inputDialog = dialog;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.inputDialog.hide();
   }
}
