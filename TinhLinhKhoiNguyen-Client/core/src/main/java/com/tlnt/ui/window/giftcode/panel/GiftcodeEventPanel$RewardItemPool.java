package com.tlnt.ui.window.giftcode.panel;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;

public final class GiftcodeEventPanel$RewardItemPool extends Pool<ItemSlotButton> {
   public GiftcodeEventPanel.EventRowPanel eventDetailPanel;
   public ButtonGroup<Button> buttonGroup;

   public GiftcodeEventPanel$RewardItemPool(GiftcodeEventPanel.EventRowPanel rowPanel) {
      super();
      this.eventDetailPanel = rowPanel;
      this.buttonGroup = new ButtonGroup<>();
   }

   @Override
   public ItemSlotButton newObject() {
      ItemSlotButton slotButton = new ItemSlotButton(this.eventDetailPanel.renderContext);
      slotButton.addListener(new GiftcodeRewardItemClickListener(this, slotButton));
      this.buttonGroup.add(slotButton);
      return slotButton;
   }
}
