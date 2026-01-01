package com.tlnt.ui.window.action;

import com.tlnt.model.item.Item;
import com.tlnt.net.task.ServerPacketSender;

public final class UseItemQuantityCallback implements QuantityInputDialog.InputCallback {
   public com.tlnt.model.item.Item itemInfo;
   public com.tlnt.ui.window.action.UsePvpItemAction itemActionHandler;
   public UseItemQuantityCallback(UsePvpItemAction action, Item item) {
      super();
      this.itemActionHandler = action;
      this.itemInfo = item;
   }

   @Override
   public final void onInputConfirmed(String text) {
      int quantity = 0;

      try {
         quantity = Integer.parseInt(text);
      } catch (Exception e) {
      }

      if (quantity <= 0) {
         quantity = 1;
      } else if (quantity > this.itemInfo.quantity) {
         quantity = this.itemInfo.quantity;
      }

      ServerPacketSender.getInstance().sendPvpAction(this.itemInfo.slotIndex, quantity);
   }
}
