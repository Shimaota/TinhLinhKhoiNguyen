package com.tlnt.ui.window.trade.panel;

import com.tlnt.core.GameMain;
import com.tlnt.core.GameState;
import com.tlnt.ui.window.action.QuantityInputDialog;

public final class TradeGoldAmountInputCallback implements QuantityInputDialog.InputCallback {
   public com.tlnt.ui.window.trade.panel.TradeEnterCoinsListener coinInputListener;
   public TradeGoldAmountInputCallback(TradeEnterCoinsListener listener) {
      super();
      this.coinInputListener = listener;
   }

   @Override
   public final void onInputConfirmed(String text) {
      int amount = 0;

      try {
         amount = Integer.parseInt(text);
      } catch (Exception e) {
      }

      if (amount > 50000000) {
         amount = 50000000;
      }

      if (amount >= 0 && (long)amount <= GameState.inventory.inventoryTimestamp1) {
         GameState.tradeSession.myOfferCoins = amount;
         GameMain.instance.getLoginScreen().debugScrollPane.refreshHudState();
      }
   }
}
