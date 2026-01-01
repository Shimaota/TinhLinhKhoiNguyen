package com.tlnt.ui.window.dailyquest.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameState;
import com.tlnt.net.task.ServerPacketSender;

public final class ClaimDailyQuestRewardClickListener extends ClickListener {
   public com.tlnt.ui.window.dailyquest.panel.DailyQuestPanel.QuestProgressNode rewardEntry;
   public ClaimDailyQuestRewardClickListener(DailyQuestPanel.QuestProgressNode node) {
      super();
      this.rewardEntry = node;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (!this.rewardEntry.questData.claimed && GameState.dailyQuestData.currentDiligencePoints >= this.rewardEntry.questData.requiredPoints) {
         ServerPacketSender.getInstance().sendPetSummon(this.rewardEntry.questData.milestoneId);
      }
   }
}
