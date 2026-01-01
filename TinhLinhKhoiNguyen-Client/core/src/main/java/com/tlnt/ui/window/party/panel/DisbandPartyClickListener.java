package com.tlnt.ui.window.party.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class DisbandPartyClickListener extends ClickListener {
   public com.tlnt.ui.window.party.panel.PartyMembersPanel partyMembersPanel;
   public DisbandPartyClickListener(PartyMembersPanel partyMembersPanel) {
      super();
      this.partyMembersPanel = partyMembersPanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().sendRequestLeaderboard();
   }
}
