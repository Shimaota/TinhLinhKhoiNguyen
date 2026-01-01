package com.tlnt.ui.window.action;

import com.tlnt.model.item.Item;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class BuyItemAction extends ItemInfoPopup.ActionConfig {
   public BuyItemAction(String label) {
      super(label);
   }

   @Override
   public final void onItemAction(Item item) {
      if (item != null) {
         ServerPacketSender.getInstance().sendRequestTrade(item.slotIndex);
      }
   }
}
