package com.tlnt.ui.window.action;

import com.tlnt.model.item.Item;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class EquipItemAction extends ItemInfoPopup.ActionConfig {
   public EquipItemAction(String label) {
      super(label);
   }

   @Override
   public final void onItemAction(Item item) {
      if (item != null) {
         ServerPacketSender.getInstance().sendConfirmTrade(item.slotIndex);
      }
   }
}
