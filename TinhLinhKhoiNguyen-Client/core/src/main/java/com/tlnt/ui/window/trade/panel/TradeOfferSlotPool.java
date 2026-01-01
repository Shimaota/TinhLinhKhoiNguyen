package com.tlnt.ui.window.trade.panel;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.utils.Pool;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;

public final class TradeOfferSlotPool extends Pool<ItemSlotButton> {
   public final com.tlnt.ui.window.trade.panel.TradeOfferPanel.ItemGridPanel itemGridPanel;
   public ButtonGroup<Button> buttonGroup;

   public TradeOfferSlotPool(TradeOfferPanel.ItemGridPanel gridPanel) {
      this.itemGridPanel = gridPanel;
      this.buttonGroup = new ButtonGroup<>();
   }

   @Override
   public final ItemSlotButton newObject() {
      ItemSlotButton slotButton;
      (slotButton = new ItemSlotButton(this.itemGridPanel.renderContext)).addListener(new TradeItemSlotClickListener(this, slotButton));
      this.buttonGroup.add(slotButton);
      return slotButton;
   }
}
