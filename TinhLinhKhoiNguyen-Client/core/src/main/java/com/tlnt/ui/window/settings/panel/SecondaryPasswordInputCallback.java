package com.tlnt.ui.window.settings.panel;

import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.window.action.QuantityInputDialog;

public final class SecondaryPasswordInputCallback implements QuantityInputDialog.InputCallback {
   public com.tlnt.ui.window.settings.panel.UnlockAccountListener unlockAccountListener;
   public SecondaryPasswordInputCallback(UnlockAccountListener listener) {
      super();
      this.unlockAccountListener = listener;
   }

   @Override
   public final void onInputConfirmed(String input) {
      ServerPacketSender.getInstance().sendVerifyCode(input);
   }
}
