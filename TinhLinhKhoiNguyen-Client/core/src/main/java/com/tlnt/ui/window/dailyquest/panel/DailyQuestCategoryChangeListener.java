package com.tlnt.ui.window.dailyquest.panel;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

public final class DailyQuestCategoryChangeListener extends ChangeListener {
   public com.tlnt.ui.window.dailyquest.panel.DailyQuestCategoryTabPool questButtonPool;
   public com.tlnt.ui.window.dailyquest.panel.DailyQuestPanel.QuestCategoryTab questButton;
   public DailyQuestCategoryChangeListener(DailyQuestCategoryTabPool pool, DailyQuestPanel.QuestCategoryTab tab) {
      super();
      this.questButtonPool = pool;
      this.questButton = tab;
   }

   @Override
   public final void changed(ChangeListener.ChangeEvent event, Actor actor) {
      this.questButtonPool.questDetailPanel.showQuestRewards(this.questButton.questCategoryData);
   }
}
