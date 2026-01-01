package com.tlnt.ui.window.action;

import com.tlnt.model.item.Item;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class EquipToSecondSlotAction extends ItemInfoPopup.ActionConfig {
   public EquipToSecondSlotAction(String label) {
      super(label);
   }

   @Override
   public final void onItemAction(Item item) {
      ServerPacketSender.getInstance().sendWithdrawItem(item.slotIndex);
   }
}
