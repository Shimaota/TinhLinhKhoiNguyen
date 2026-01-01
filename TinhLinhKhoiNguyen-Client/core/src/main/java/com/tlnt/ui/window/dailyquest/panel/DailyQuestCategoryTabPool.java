package com.tlnt.ui.window.dailyquest.panel;

import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.utils.Pool;

public final class DailyQuestCategoryTabPool extends Pool<DailyQuestPanel.QuestCategoryTab> {
   public final com.tlnt.ui.window.dailyquest.panel.DailyQuestPanel.DailyQuestListPanel questDetailPanel;
   public ButtonGroup<DailyQuestPanel.QuestCategoryTab> categoryButtonGroup;

   public DailyQuestCategoryTabPool(DailyQuestPanel.DailyQuestListPanel listPanel) {
      this.questDetailPanel = listPanel;
      this.categoryButtonGroup = new ButtonGroup<>();
   }

   @Override
   public final DailyQuestPanel.QuestCategoryTab newObject() {
      DailyQuestPanel.QuestCategoryTab tab = this.questDetailPanel.dailyQuestPanel.new QuestCategoryTab(
         this.questDetailPanel.renderContext
      );
      this.categoryButtonGroup.add(tab);
      tab.addListener(new DailyQuestCategoryChangeListener(this, tab));
      return tab;
   }
}
