package com.tlnt.ui.window.action;

import com.tlnt.net.task.ServerPacketSender;

public final class SplitItemContextAction extends ItemActionMenuPopup.ActionCallback {
   @Override
   public final void execute() {
      if (super.itemId != null) {
         ServerPacketSender.getInstance().sendFriendAction((Integer)super.itemId);
      }
   }
}
