package com.tlnt.ui.window.party.tab;

import com.tlnt.core.GameState;
import com.tlnt.ui.hud.widget.TargetMarker;

public final class FriendInvitePendingChecker implements TargetMarker.BadgeCondition {
   public com.tlnt.ui.window.party.tab.PartyFriendsTab friendsTab;
   public FriendInvitePendingChecker(PartyFriendsTab friendsTab) {
      super();
      this.friendsTab = friendsTab;
   }

   @Override
   public final boolean hasBadge() {
      return ((!GameState.friendInvites.isEmpty() ? 1 : 0) != 0);
   }
}
