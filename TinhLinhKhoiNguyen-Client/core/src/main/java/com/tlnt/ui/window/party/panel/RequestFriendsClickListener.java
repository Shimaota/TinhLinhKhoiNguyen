package com.tlnt.ui.window.party.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class RequestFriendsClickListener extends ClickListener {
   public com.tlnt.ui.window.party.panel.PartyMembersPanel membersPanel;
   public RequestFriendsClickListener(PartyMembersPanel membersPanel) {
      super();
      this.membersPanel = membersPanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ServerPacketSender.getInstance().sendRequestFriends();
   }
}
