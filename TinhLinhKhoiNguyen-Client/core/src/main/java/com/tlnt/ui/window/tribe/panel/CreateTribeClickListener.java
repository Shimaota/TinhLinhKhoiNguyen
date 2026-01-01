package com.tlnt.ui.window.tribe.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.ui.window.action.QuantityInputDialog;

public final class CreateTribeClickListener extends ClickListener {
   public com.tlnt.ui.window.tribe.panel.TribeListEntry tribeListEntry;

   public CreateTribeClickListener(TribeListEntry tribeListEntry) {
      super();
      this.tribeListEntry = tribeListEntry;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      QuantityInputDialog dialog = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(QuantityInputDialog.class);
      dialog.openWithPrompt("Tạo bộ lạc\n[%80](cần 1 cờ lệnh bộ lạc)", null, "", new CreateTribeInputCallback(this));
   }

}
