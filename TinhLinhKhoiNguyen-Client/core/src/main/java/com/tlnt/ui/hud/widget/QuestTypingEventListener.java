package com.tlnt.ui.hud.widget;

import com.github.tommyettinger.textra.TypingAdapter;
import com.tlnt.world.entity.player.PlayerEntity;

public final class QuestTypingEventListener extends TypingAdapter {
   public com.tlnt.ui.hud.widget.QuestPartyPanel.QuestDetailPanel questDetailPanel;

   public QuestTypingEventListener(QuestPartyPanel.QuestDetailPanel questDetailPanel) {
      super();
      this.questDetailPanel = questDetailPanel;
   }

   @Override
   public final void event(String eventName) {
      if (eventName.equals("finding")) {
         PlayerEntity.getInstance().offscreenArrow.setAutoPathing(true); // clicking the quest text: auto-pathing
      }
   }

}
