package com.tlnt.ui.window.dailyquest;

import com.tlnt.core.GameState;
import com.tlnt.ui.hud.widget.TargetMarker;

public final class DailyQuestTabCondition implements TargetMarker.BadgeCondition {
   public com.tlnt.ui.window.dailyquest.DailyActivityWindow dailyActivityWindow;
   public DailyQuestTabCondition(DailyActivityWindow window) {
      super();
      this.dailyActivityWindow = window;
   }

   @Override
   public final boolean hasBadge() {
      return GameState.dailyQuestData.hasNewlyCompletedTasks;
   }
}
