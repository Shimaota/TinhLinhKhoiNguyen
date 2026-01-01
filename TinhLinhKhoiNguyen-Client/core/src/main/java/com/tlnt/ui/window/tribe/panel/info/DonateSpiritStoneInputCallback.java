package com.tlnt.ui.window.tribe.panel.info;

import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.window.action.QuantityInputDialog;

public final class DonateSpiritStoneInputCallback implements QuantityInputDialog.InputCallback {
   public com.tlnt.ui.window.tribe.panel.info.DonateSpiritStoneButtonListener donateSpiritStoneListener;
   public DonateSpiritStoneInputCallback(DonateSpiritStoneButtonListener donateSpiritStoneListener) {
      super();
      this.donateSpiritStoneListener = donateSpiritStoneListener;
   }

   @Override
   public final void onInputConfirmed(String input) {
      int amount = 0;

      try {
         amount = Integer.parseInt(input);
      } catch (Exception e) {
      }

      ServerPacketSender.getInstance().sendAuctionAction(amount);
   }
}
