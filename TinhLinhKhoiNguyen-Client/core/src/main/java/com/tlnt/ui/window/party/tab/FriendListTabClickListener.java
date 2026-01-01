package com.tlnt.ui.window.party.tab;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class FriendListTabClickListener extends ClickListener {
   public com.tlnt.ui.window.party.tab.PartyFriendsTab friendsTab;
   public FriendListTabClickListener(PartyFriendsTab friendsTab) {
      super();
      this.friendsTab = friendsTab;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.friendsTab.friendListPanel.setVisible(true);
      this.friendsTab.friendInvitePanel.setVisible(false);
   }
}
