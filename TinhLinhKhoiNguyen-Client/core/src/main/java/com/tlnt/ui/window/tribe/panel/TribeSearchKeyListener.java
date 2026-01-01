package com.tlnt.ui.window.tribe.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.tlnt.net.task.ServerPacketSender;

public final class TribeSearchKeyListener extends InputListener {
   public com.tlnt.ui.window.tribe.panel.TribeListEntry tribeListEntry;
   public TribeSearchKeyListener(TribeListEntry tribeListEntry) {
      super();
      this.tribeListEntry = tribeListEntry;
   }

   @Override
   public final boolean keyUp(InputEvent event, int keycode) {
      ServerPacketSender.getInstance().sendMarketSearch(this.tribeListEntry.searchTextField.getText());
      return super.keyUp(event, keycode);
   }
}
