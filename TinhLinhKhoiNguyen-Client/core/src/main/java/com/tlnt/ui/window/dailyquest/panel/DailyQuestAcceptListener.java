package com.tlnt.ui.window.dailyquest.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class DailyQuestAcceptListener extends ClickListener {
   public com.tlnt.ui.window.dailyquest.panel.DailyQuestPanel.QuestCategoryTab questButton;
   public DailyQuestAcceptListener(DailyQuestPanel.QuestCategoryTab tab) {
      super();
      this.questButton = tab;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (this.questButton.questCategoryData != null) {
         if (this.questButton.questCategoryData.currentProgress < this.questButton.questCategoryData.targetProgress) {
            ServerPacketSender.getInstance().sendPetDismiss(this.questButton.questCategoryData.questIndex);
         } else {
            if (!this.questButton.questCategoryData.rewardClaimed) {
               ServerPacketSender.getInstance().sendPetInteract(this.questButton.questCategoryData.questIndex);
            }
         }
      }
   }
}
