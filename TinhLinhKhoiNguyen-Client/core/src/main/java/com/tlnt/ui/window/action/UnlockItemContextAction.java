package com.tlnt.ui.window.action;

import com.tlnt.net.task.ServerPacketSender;

public final class UnlockItemContextAction extends ItemActionMenuPopup.ActionCallback {
   @Override
   public final void execute() {
      if (super.itemId != null) {
         ServerPacketSender.getInstance().sendPartySkill((Integer)super.itemId);
      }
   }
}
