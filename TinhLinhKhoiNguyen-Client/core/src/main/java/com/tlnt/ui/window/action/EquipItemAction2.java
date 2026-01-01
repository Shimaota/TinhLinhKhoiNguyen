package com.tlnt.ui.window.action;

import com.tlnt.model.item.Item;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class EquipItemAction2 extends ItemInfoPopup.ActionConfig {
   public EquipItemAction2(String label) {
      super(label);
   }

   @Override
   public final void onItemAction(Item item) {
      if (item != null) {
         ServerPacketSender.getInstance().sendUseItem(item.slotIndex);
      }
   }
}
