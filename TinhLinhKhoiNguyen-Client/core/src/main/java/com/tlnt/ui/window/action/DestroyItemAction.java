package com.tlnt.ui.window.action;

import com.tlnt.model.item.Item;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class DestroyItemAction extends ItemInfoPopup.ActionConfig {
   public DestroyItemAction(String label) {
      super(label);
   }

   @Override
   public final void onItemAction(Item item) {
      ServerPacketSender sender = ServerPacketSender.getInstance();
      sender.sendClaimReward(-1);
   }
}
