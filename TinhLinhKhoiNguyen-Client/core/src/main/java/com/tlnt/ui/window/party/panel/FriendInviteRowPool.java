package com.tlnt.ui.window.party.panel;

import com.badlogic.gdx.utils.Pool;

public final class FriendInviteRowPool extends Pool<FriendInvitePanel.InviteRow> {
   public com.tlnt.ui.window.party.panel.FriendInvitePanel panel;
   public FriendInviteRowPool(FriendInvitePanel panel) {
      super();
      this.panel = panel;
   }

   @Override
   public FriendInvitePanel.InviteRow newObject() {
      return (this.panel).new InviteRow(this.panel.renderContext);
   }
}
