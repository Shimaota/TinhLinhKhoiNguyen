package com.tlnt.ui.window.tribe.panel.info;

import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.window.action.QuantityInputDialog;

public final class DonateGoldInputCallback implements QuantityInputDialog.InputCallback {
   public com.tlnt.ui.window.tribe.panel.info.DonateGoldButtonListener donateGoldListener;
   public DonateGoldInputCallback(DonateGoldButtonListener donateGoldListener) {
      super();
      this.donateGoldListener = donateGoldListener;
   }

   @Override
   public final void onInputConfirmed(String input) {
      int amount = 0;

      try {
         amount = Integer.parseInt(input);
      } catch (Exception e) {
      }

      ServerPacketSender.getInstance().sendAuctionBid(amount);
   }
}
