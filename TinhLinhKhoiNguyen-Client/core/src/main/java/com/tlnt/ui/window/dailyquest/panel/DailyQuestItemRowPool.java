package com.tlnt.ui.window.dailyquest.panel;

import com.badlogic.gdx.utils.Pool;

public final class DailyQuestItemRowPool extends Pool<DailyQuestPanel.QuestProgressNode> {
   public com.tlnt.ui.window.dailyquest.panel.DailyQuestPanel.DiligenceProgressPanel parentPanel;
   public DailyQuestItemRowPool(DailyQuestPanel.DiligenceProgressPanel progressPanel) {
      super();
      this.parentPanel = progressPanel;
   }

   @Override
   public DailyQuestPanel.QuestProgressNode newObject() {
      return (this.parentPanel.dailyQuestPanel).new QuestProgressNode(this.parentPanel.renderContext);
   }
}
