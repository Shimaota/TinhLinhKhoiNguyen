package com.tlnt.ui.dialog;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.util.FileStorage;

public final class DeleteLocalDataClickListener extends ClickListener {
   public com.tlnt.ui.dialog.ConfirmDialog confirmDialog;
   public DeleteLocalDataClickListener(ConfirmDialog confirmDialog) {
      super();
      this.confirmDialog = confirmDialog;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (this.confirmDialog != null) {
         this.confirmDialog.hide();
      }
      FileStorage.wipeDataAndExit();
   }
}
