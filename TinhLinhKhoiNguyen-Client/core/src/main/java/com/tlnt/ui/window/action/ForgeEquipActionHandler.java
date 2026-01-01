package com.tlnt.ui.window.action;

import com.tlnt.model.consignment.ConsignmentItem;
import com.tlnt.model.item.Item;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class ForgeEquipActionHandler extends ItemInfoPopup.ActionConfig {
   public ForgeEquipActionHandler(String label) {
      super(label);
   }

   @Override
   public final void onItemAction(Item item) {
      if (super.callbackData instanceof ConsignmentItem) {
         ConsignmentItem consignmentItem = (ConsignmentItem)super.callbackData;
         ServerPacketSender sender = ServerPacketSender.getInstance();
         sender.sendEnhanceEquipment(consignmentItem.category.shopData.shopId, consignmentItem.category.index, consignmentItem.slot, 1);
      }
   }
}
