package com.tlnt.ui.hud.widget;

import com.tlnt.core.GameState;

public final class GiftcodeBadgeChecker implements TargetMarker.BadgeCondition {
   public com.tlnt.ui.hud.widget.MenuButtonPanel menuButtonPanel;
   public GiftcodeBadgeChecker(MenuButtonPanel menuButtonPanel) {
      super();
      this.menuButtonPanel = menuButtonPanel;
   }

   @Override
   public final boolean hasBadge() {
      return GameState.giftcodeData.hasClaimableReward;
   }
}
