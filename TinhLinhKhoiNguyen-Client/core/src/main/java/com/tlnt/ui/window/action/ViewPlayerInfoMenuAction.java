package com.tlnt.ui.window.action;

import com.tlnt.net.task.ServerPacketSender;

public final class ViewPlayerInfoMenuAction extends ItemActionMenuPopup.ActionCallback {
   @Override
   public final void execute() {
      if (super.itemId != null) {
         ServerPacketSender.getInstance().sendPetRelease((Integer)super.itemId);
      }
   }
}
