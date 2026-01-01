package com.tlnt.ui.hud.widget;

import com.tlnt.core.GameState;

public final class MenuButtonVisibilityChecker implements TargetMarker.BadgeCondition {
   public com.tlnt.ui.hud.widget.MenuButtonPanel menuButtonPanel;
   public MenuButtonVisibilityChecker(MenuButtonPanel menuButtonPanel) {
      super();
      this.menuButtonPanel = menuButtonPanel;
   }

   @Override
   public final boolean hasBadge() {
      if (!GameState.achievementData.hasClaimableReward && !GameState.dailyQuestData.hasNewlyCompletedTasks) {
         return false;
      } else {
         return true;
      }
   }
}
