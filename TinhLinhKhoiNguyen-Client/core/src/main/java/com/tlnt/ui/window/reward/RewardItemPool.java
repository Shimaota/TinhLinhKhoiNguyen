package com.tlnt.ui.window.reward;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;

public final class RewardItemPool extends Pool<ItemSlotButton> {
   public com.tlnt.ui.window.reward.RewardPopup rewardPopup;
   public ButtonGroup<Button> buttonGroup;

   public RewardItemPool(RewardPopup popup) {
      super();
      this.rewardPopup = popup;
      this.buttonGroup = new ButtonGroup<>();
   }

   @Override
   public final ItemSlotButton newObject() {
      ItemSlotButton slotButton;
      (slotButton = new ItemSlotButton(this.rewardPopup.renderContext)).addListener(new RewardItemClickListener(this, slotButton));
      this.buttonGroup.add(slotButton);
      return slotButton;
   }
}
