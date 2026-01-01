package com.tlnt.ui.window.party.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class AutoApproveJoinRequestsListener extends ClickListener {
   public com.tlnt.ui.window.party.panel.PartyJoinRequestsPanel partyJoinRequestsPanel;
   public AutoApproveJoinRequestsListener(PartyJoinRequestsPanel partyJoinRequestsPanel) {
      super();
      this.partyJoinRequestsPanel = partyJoinRequestsPanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().sendRequestFriends();
   }
}
