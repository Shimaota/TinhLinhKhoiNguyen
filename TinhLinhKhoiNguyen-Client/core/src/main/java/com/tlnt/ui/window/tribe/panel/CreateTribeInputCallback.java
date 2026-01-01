package com.tlnt.ui.window.tribe.panel;

import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.window.action.QuantityInputDialog;

public final class CreateTribeInputCallback implements QuantityInputDialog.InputCallback {
   public com.tlnt.ui.window.tribe.panel.CreateTribeClickListener createTribeClickListener;
   public CreateTribeInputCallback(CreateTribeClickListener createTribeClickListener) {
      super();
      this.createTribeClickListener = createTribeClickListener;
   }

   @Override
   public final void onInputConfirmed(String input) {
      ServerPacketSender.getInstance().sendMarketBuy(input);
   }
}
