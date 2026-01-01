package com.tlnt.ui.window.reward;

import com.tlnt.ui.hud.itemslot.ItemSlotButton;

public final class AddRewardItemTask implements Runnable {
   public int itemIndex;
   public com.tlnt.ui.window.reward.RewardPopup rewardPopup;
   public AddRewardItemTask(RewardPopup popup, int itemIndex) {
      super();
      this.rewardPopup = popup;
      this.itemIndex = itemIndex;
   }

   @Override
   public final void run() {
      ItemSlotButton slotButton;
      (slotButton = this.rewardPopup.rewardItemPool.obtain()).setItem(this.rewardPopup.rewardItems[this.itemIndex]);
      slotButton.enchantEffect.playSweep();
      this.rewardPopup.rewardItemTable.add(slotButton).size(104.0F).space(10.0F);
      this.rewardPopup.rewardScrollPane.setScrollX(this.rewardPopup.rewardScrollPane.getMaxX());
   }
}
