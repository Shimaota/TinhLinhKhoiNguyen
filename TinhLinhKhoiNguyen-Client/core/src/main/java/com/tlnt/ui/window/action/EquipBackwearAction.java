package com.tlnt.ui.window.action;

import com.tlnt.model.item.Item;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

/** "Equip" a backwear item (type 48) from the bag -> backwear collection (decompiled C3655.f13726, op 27 sub 20). */
public final class EquipBackwearAction extends ItemInfoPopup.ActionConfig {
   public EquipBackwearAction(String label) {
      super(label);
   }

   @Override
   public final void onItemAction(Item item) {
      if (item != null) {
         ServerPacketSender.getInstance().sendPutBackwear(item.slotIndex);
      }
   }
}
