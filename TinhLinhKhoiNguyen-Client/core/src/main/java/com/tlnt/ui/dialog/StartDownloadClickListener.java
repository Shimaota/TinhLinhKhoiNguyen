package com.tlnt.ui.dialog;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class StartDownloadClickListener extends ClickListener {
   public com.tlnt.ui.dialog.LoadingDialog loadingDialog;
   public StartDownloadClickListener(LoadingDialog loadingDialog) {
      super();
      this.loadingDialog = loadingDialog;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.loadingDialog.processNextTask();
   }
}
