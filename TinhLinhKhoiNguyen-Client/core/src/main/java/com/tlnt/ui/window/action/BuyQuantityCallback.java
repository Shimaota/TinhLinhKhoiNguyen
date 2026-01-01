package com.tlnt.ui.window.action;

import com.tlnt.model.consignment.ConsignmentItem;
import com.tlnt.net.task.ServerPacketSender;

public final class BuyQuantityCallback implements QuantityInputDialog.InputCallback {
   public com.tlnt.model.consignment.ConsignmentItem item;
   public com.tlnt.ui.window.action.BuyManyAction action;
   public BuyQuantityCallback(BuyManyAction action, ConsignmentItem item) {
      super();
      this.action = action;
      this.item = item;
   }

   @Override
   public final void onInputConfirmed(String text) {
      int quantity = 0;

      try {
         quantity = Integer.parseInt(text);
      } catch (Exception e) {
      }

      if (quantity > 0) {
         ServerPacketSender.getInstance().sendEnhanceEquipment(this.item.category.shopData.shopId, this.item.category.index, this.item.slot, quantity);
      }
   }
}
