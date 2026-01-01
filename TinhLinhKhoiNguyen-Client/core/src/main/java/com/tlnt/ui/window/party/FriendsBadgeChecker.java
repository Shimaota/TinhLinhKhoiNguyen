package com.tlnt.ui.window.party;

import com.tlnt.core.GameState;
import com.tlnt.ui.hud.widget.TargetMarker;

public final class FriendsBadgeChecker implements TargetMarker.BadgeCondition {
   public com.tlnt.ui.window.party.PartyWindow partyWindow;
   public FriendsBadgeChecker(PartyWindow partyWindow) {
      super();
      this.partyWindow = partyWindow;
   }

   @Override
   public final boolean hasBadge() {
      if (!GameState.friendInvites.isEmpty()) {
         return true;
      } else {
         return false;
      }
   }
}
