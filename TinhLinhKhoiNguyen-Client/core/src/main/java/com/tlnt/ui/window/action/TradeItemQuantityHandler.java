package com.tlnt.ui.window.action;

import com.tlnt.core.GameState;
import com.tlnt.model.item.Item;

public final class TradeItemQuantityHandler implements QuantityInputDialog.InputCallback {
   public com.tlnt.model.item.Item item;
   public com.tlnt.ui.window.action.AddTradeItemAction tradeHandler;
   public TradeItemQuantityHandler(AddTradeItemAction action, Item item) {
      super();
      this.tradeHandler = action;
      this.item = item;
   }

   @Override
   public final void onInputConfirmed(String text) {
      int quantity = 0;

      try {
         quantity = Integer.parseInt(text);
      } catch (Exception e) {
      }

      if (quantity <= 0) {
         quantity = 1;
      } else if (quantity > this.item.quantity) {
         quantity = this.item.quantity;
      }

      GameState.tradeSession.myOfferItems.add(new Item(this.item, quantity));
   }
}
