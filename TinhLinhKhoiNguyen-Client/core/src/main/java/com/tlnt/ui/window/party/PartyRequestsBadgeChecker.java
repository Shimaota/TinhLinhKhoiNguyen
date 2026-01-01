package com.tlnt.ui.window.party;

import com.tlnt.core.GameState;
import com.tlnt.ui.hud.widget.TargetMarker;

public final class PartyRequestsBadgeChecker implements TargetMarker.BadgeCondition {
   public com.tlnt.ui.window.party.PartyWindow partyWindow;
   public PartyRequestsBadgeChecker(PartyWindow partyWindow) {
      super();
      this.partyWindow = partyWindow;
   }

   @Override
   public final boolean hasBadge() {
      if (!GameState.currentParty.joinRequests.isEmpty()) {
         return true;
      } else {
         return false;
      }
   }
}
