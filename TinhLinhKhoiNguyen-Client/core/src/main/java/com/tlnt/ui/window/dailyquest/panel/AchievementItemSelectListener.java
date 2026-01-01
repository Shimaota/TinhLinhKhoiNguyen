package com.tlnt.ui.window.dailyquest.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class AchievementItemSelectListener extends ClickListener {
   public com.tlnt.ui.window.dailyquest.panel.AchievementPanel.ListPanel.AchievementEntryButton achievementEntry;
   public com.tlnt.ui.window.dailyquest.panel.AchievementPanel.ListPanel listPanel;
   public AchievementItemSelectListener(AchievementPanel.ListPanel.AchievementEntryButton entry, AchievementPanel.ListPanel listPanel) {
      super();
      this.achievementEntry = entry;
      this.listPanel = listPanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.achievementEntry.outerListPanel.parentPanel.detailPanel.showAchievement(this.achievementEntry.achievement);
   }
}
