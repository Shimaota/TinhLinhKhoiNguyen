package com.tlnt.ui.window.action;

import com.tlnt.model.item.Item;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class DonateToTribeActionHandler extends ItemInfoPopup.ActionConfig {
   public DonateToTribeActionHandler(String label) {
      super(label);
   }

   @Override
   public final void onItemAction(Item item) {
      ServerPacketSender.getInstance().sendPvpChallenge(item.slotIndex);
   }
}
