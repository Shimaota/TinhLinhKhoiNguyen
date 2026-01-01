package com.tlnt.ui.hud.widget;

import com.tlnt.core.GameState;

public final class MenuButtonNotificationChecker implements TargetMarker.BadgeCondition {
   public com.tlnt.ui.hud.widget.MenuButtonPanel menuButtonPanel;
   public MenuButtonNotificationChecker(MenuButtonPanel menuButtonPanel) {
      super();
      this.menuButtonPanel = menuButtonPanel;
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
