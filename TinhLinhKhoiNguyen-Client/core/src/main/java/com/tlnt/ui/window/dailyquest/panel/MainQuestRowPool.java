package com.tlnt.ui.window.dailyquest.panel;

import com.badlogic.gdx.utils.Pool;

public final class MainQuestRowPool extends Pool<MainQuestPanel.QuestCategoryButton> {
   public com.tlnt.ui.window.dailyquest.panel.MainQuestPanel questPanel;
   public MainQuestRowPool(MainQuestPanel panel) {
      super();
      this.questPanel = panel;
   }

   @Override
   public MainQuestPanel.QuestCategoryButton newObject() {
      return (this.questPanel).new QuestCategoryButton(this.questPanel.renderContext);
   }
}
