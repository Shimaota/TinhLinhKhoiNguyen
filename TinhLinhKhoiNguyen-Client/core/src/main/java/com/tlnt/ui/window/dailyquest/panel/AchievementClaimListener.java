package com.tlnt.ui.window.dailyquest.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.net.task.ServerPacketSender;

public final class AchievementClaimListener extends ClickListener {
   public com.tlnt.ui.window.dailyquest.panel.AchievementPanel.ListPanel.AchievementEntryButton achievementEntry;
   public AchievementClaimListener(AchievementPanel.ListPanel.AchievementEntryButton entry) {
      super();
      this.achievementEntry = entry;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      if (this.achievementEntry.claimButton.status == 1) {
         ServerPacketSender.getInstance().sendMountEquip(this.achievementEntry.achievement.id);
      }
   }
}
