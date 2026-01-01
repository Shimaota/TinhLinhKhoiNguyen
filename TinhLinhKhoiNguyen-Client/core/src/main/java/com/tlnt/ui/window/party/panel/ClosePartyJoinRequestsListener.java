package com.tlnt.ui.window.party.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class ClosePartyJoinRequestsListener extends ClickListener {
   public com.tlnt.ui.window.party.panel.PartyJoinRequestsPanel panel;
   public ClosePartyJoinRequestsListener(PartyJoinRequestsPanel panel) {
      super();
      this.panel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().sendRequestLeaderboard();
   }
}
