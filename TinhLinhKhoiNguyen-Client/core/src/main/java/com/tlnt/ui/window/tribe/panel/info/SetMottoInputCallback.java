package com.tlnt.ui.window.tribe.panel.info;

import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.window.action.QuantityInputDialog;

public final class SetMottoInputCallback implements QuantityInputDialog.InputCallback {
   public com.tlnt.ui.window.tribe.panel.info.SetMottoButtonListener setMottoListener;
   public SetMottoInputCallback(SetMottoButtonListener setMottoListener) {
      super();
      this.setMottoListener = setMottoListener;
   }

   @Override
   public final void onInputConfirmed(String input) {
      ServerPacketSender.getInstance().sendSetTitle(input);
   }
}
