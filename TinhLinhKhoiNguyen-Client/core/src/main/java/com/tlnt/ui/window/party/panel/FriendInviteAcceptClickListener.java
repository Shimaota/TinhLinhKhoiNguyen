package com.tlnt.ui.window.party.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class FriendInviteAcceptClickListener extends ClickListener {
   public com.tlnt.ui.window.party.panel.FriendInvitePanel.InviteRow inviteRow;
   public FriendInviteAcceptClickListener(FriendInvitePanel.InviteRow inviteRow) {
      super();
      this.inviteRow = inviteRow;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().sendHarvestItem(this.inviteRow.inviteEntry.playerId);
   }
}
