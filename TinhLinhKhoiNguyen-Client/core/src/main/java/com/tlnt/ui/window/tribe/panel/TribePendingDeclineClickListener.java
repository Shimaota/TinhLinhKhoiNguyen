package com.tlnt.ui.window.tribe.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class TribePendingDeclineClickListener extends ClickListener {
   public com.tlnt.ui.window.tribe.panel.TribePendingPanel.PendingMemberRow pendingMemberRow;
   public TribePendingDeclineClickListener(TribePendingPanel.PendingMemberRow pendingMemberRow) {
      super();
      this.pendingMemberRow = pendingMemberRow;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (this.pendingMemberRow.pendingMemberData != null) {
         ServerPacketSender.getInstance().sendMarketAction(this.pendingMemberRow.pendingMemberData.playerId);
      }
   }
}
