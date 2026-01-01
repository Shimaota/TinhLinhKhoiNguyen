package com.tlnt.ui.window.party.tab;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class FriendInviteTabClickListener extends ClickListener {
   public com.tlnt.ui.window.party.tab.PartyFriendsTab friendsTab;
   public FriendInviteTabClickListener(PartyFriendsTab friendsTab) {
      super();
      this.friendsTab = friendsTab;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.friendsTab.friendListPanel.setVisible(false);
      this.friendsTab.friendInvitePanel.setVisible(true);
   }
}
