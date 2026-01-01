package com.tlnt.ui.window.dailyquest.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class DailyQuestSelectListener extends ClickListener {
   public com.tlnt.ui.window.dailyquest.panel.DailyQuestPanel dailyQuestPanel;
   public com.tlnt.ui.window.dailyquest.panel.DailyQuestPanel.QuestCategoryTab questButton;
   public DailyQuestSelectListener(DailyQuestPanel.QuestCategoryTab tab, DailyQuestPanel panel) {
      super();
      this.questButton = tab;
      this.dailyQuestPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.questButton.dailyQuestPanel.dailyQuestListPanel.showQuestRewards(this.questButton.questCategoryData);
   }
}
