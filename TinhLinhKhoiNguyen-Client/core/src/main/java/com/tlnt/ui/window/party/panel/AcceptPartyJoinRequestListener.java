package com.tlnt.ui.window.party.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class AcceptPartyJoinRequestListener extends ClickListener {
   public com.tlnt.ui.window.party.panel.PartyJoinRequestsPanel.JoinRequestRow joinRequestRow;
   public AcceptPartyJoinRequestListener(PartyJoinRequestsPanel.JoinRequestRow joinRequestRow) {
      super();
      this.joinRequestRow = joinRequestRow;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (this.joinRequestRow.joinRequest != null) {
         ServerPacketSender.getInstance().sendPartyInvite(this.joinRequestRow.joinRequest.playerId);
      }
   }
}
