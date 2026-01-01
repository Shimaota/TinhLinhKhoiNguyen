package com.tlnt.ui.window.reward;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;

public final class ShowRewardItemsTask implements Runnable {
   public com.tlnt.ui.window.reward.RewardPopup rewardPopup;
   public ShowRewardItemsTask(RewardPopup popup) {
      super();
      this.rewardPopup = popup;
   }

   @Override
   public final void run() {
      this.rewardPopup.rewardScrollPane.setScrollX(this.rewardPopup.rewardScrollPane.getMaxX());
      Array.ArrayIterator iter = this.rewardPopup.rewardItemTable.getChildren().iterator();

      while (iter.hasNext()) {
         Actor child;
         if ((child = (Actor)iter.next()) instanceof ItemSlotButton) {
            ItemSlotButton slotButton;
            (slotButton = (ItemSlotButton)child).enchantEffect.playSweep();
         }
      }

      this.rewardPopup.headerLabel.setVisible(true);
      this.rewardPopup.confirmButton.setVisible(true);
   }
}
