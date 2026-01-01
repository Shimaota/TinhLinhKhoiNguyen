package com.tlnt.ui.window.party.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class JoinPartyClickListener extends ClickListener {
   public com.tlnt.ui.window.party.panel.PartyNearbyListPanel.NearbyPartyRow nearbyPartyRow;
   public JoinPartyClickListener(PartyNearbyListPanel.NearbyPartyRow nearbyPartyRow) {
      super();
      this.nearbyPartyRow = nearbyPartyRow;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (this.nearbyPartyRow.partyData != null) {
         ServerPacketSender.getInstance().sendPartyAction(this.nearbyPartyRow.partyData.id);
      }
   }
}
