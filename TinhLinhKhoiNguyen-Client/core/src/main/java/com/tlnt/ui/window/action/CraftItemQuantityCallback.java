package com.tlnt.ui.window.action;

import com.tlnt.model.item.Item;
import com.tlnt.net.task.ServerPacketSender;

public final class CraftItemQuantityCallback implements QuantityInputDialog.InputCallback {
   public com.tlnt.ui.window.action.CraftItemAction craftItemAction;
   public com.tlnt.model.item.Item item;
   public CraftItemQuantityCallback(CraftItemAction action, Item item) {
      super();
      this.craftItemAction = action;
      this.item = item;
   }

   @Override
   public final void onInputConfirmed(String text) {
      int quantity = 1;

      try {
         quantity = Integer.parseInt(text);
      } catch (Exception e) {
      }

      if (quantity > 0 && quantity <= this.item.quantity) {
         ServerPacketSender.getInstance().sendCraftItem(this.item.slotIndex, quantity);
      }
   }
}
