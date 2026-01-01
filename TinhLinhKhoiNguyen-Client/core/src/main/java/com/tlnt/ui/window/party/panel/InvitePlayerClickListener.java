package com.tlnt.ui.window.party.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class InvitePlayerClickListener extends ClickListener {
   public com.tlnt.ui.window.party.panel.PartyNearbyPlayersPanel.NearbyPlayerRow playerRow;
   public InvitePlayerClickListener(PartyNearbyPlayersPanel.NearbyPlayerRow playerRow) {
      super();
      this.playerRow = playerRow;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (this.playerRow.playerEntity != null) {
         ServerPacketSender.getInstance().sendLeaderboardAction(this.playerRow.playerEntity.getId());
      }
   }
}
