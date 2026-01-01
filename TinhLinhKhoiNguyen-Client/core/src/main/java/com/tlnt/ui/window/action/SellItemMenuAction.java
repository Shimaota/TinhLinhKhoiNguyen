package com.tlnt.ui.window.action;

import com.tlnt.net.task.ServerPacketSender;

public final class SellItemMenuAction extends ItemActionMenuPopup.ActionCallback {
   @Override
   public final void execute() {
      if (super.itemId != null) {
         ServerPacketSender.getInstance().sendPetFeed((Integer)super.itemId);
      }
   }
}
