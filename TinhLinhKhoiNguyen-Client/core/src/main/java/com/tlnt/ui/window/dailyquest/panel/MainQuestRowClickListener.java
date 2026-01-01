package com.tlnt.ui.window.dailyquest.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class MainQuestRowClickListener extends ClickListener {
   public com.tlnt.ui.window.dailyquest.panel.MainQuestPanel questPanel;
   public com.tlnt.ui.window.dailyquest.panel.MainQuestPanel.QuestCategoryButton questRow;
   public MainQuestRowClickListener(MainQuestPanel.QuestCategoryButton row, MainQuestPanel panel) {
      super();
      this.questRow = row;
      this.questPanel = panel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.questRow.parentPanel.selectQuest(this.questRow.questChapter);
   }
}
