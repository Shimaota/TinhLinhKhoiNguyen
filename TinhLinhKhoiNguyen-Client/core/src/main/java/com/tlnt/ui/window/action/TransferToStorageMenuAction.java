package com.tlnt.ui.window.action;

import com.tlnt.net.task.ServerPacketSender;

public final class TransferToStorageMenuAction extends ItemActionMenuPopup.ActionCallback {
   @Override
   public final void execute() {
      if (super.itemId != null) {
         ServerPacketSender.getInstance().sendHarvestItem((Integer)super.itemId);
      }
   }
}
