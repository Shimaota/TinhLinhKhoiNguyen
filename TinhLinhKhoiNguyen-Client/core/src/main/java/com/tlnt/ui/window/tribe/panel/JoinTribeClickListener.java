package com.tlnt.ui.window.tribe.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class JoinTribeClickListener extends ClickListener {
   public com.tlnt.ui.window.tribe.panel.TribeListEntry.TribeInfoRow tribeInfoRow;
   public JoinTribeClickListener(TribeListEntry.TribeInfoRow tribeInfoRow) {
      super();
      this.tribeInfoRow = tribeInfoRow;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (this.tribeInfoRow.tribeData != null) {
         ServerPacketSender.getInstance().sendMarketSell(this.tribeInfoRow.tribeData.id);
      }
   }
}
