package com.tlnt.ui.dialog.widget;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class InputVisibilityClickListener extends ClickListener {
   public com.tlnt.ui.dialog.widget.TextInputRow ownerInputRow;
   public InputVisibilityClickListener(TextInputRow ownerInputRow) {
      super();
      this.ownerInputRow = ownerInputRow;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      boolean visible = this.ownerInputRow.visibilityToggle.isChecked();
      int passwordMode;
      if (!visible) {
         passwordMode = 1;
      } else {
         passwordMode = 0;
      }

      this.ownerInputRow.textField.setPasswordMode((passwordMode != 0));
   }
}
