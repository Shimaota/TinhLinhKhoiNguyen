package com.tlnt.ui.window.giftcode;

import com.tlnt.ui.hud.widget.TargetMarker;
import com.tlnt.ui.window.giftcode.panel.GiftcodeEventPanel;

public final class GiftcodeEventTabChecker implements TargetMarker.BadgeCondition {
   public com.tlnt.ui.window.giftcode.GiftcodeWindow giftcodeWindow;
   public com.tlnt.ui.window.giftcode.panel.GiftcodeEventPanel eventPanel;
   public GiftcodeEventTabChecker(GiftcodeWindow window, GiftcodeEventPanel eventPanel) {
      super();
      this.giftcodeWindow = window;
      this.eventPanel = eventPanel;
   }

   @Override
   public final boolean hasBadge() {
      return this.eventPanel.getEventList().hasClaimableReward;
   }
}
