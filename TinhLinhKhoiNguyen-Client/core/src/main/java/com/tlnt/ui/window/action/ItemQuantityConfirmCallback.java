package com.tlnt.ui.window.action;

import com.tlnt.core.GameMain;
import com.tlnt.core.GameState;
import com.tlnt.model.item.Item;

public final class ItemQuantityConfirmCallback implements QuantityInputDialog.InputCallback {
   public com.tlnt.ui.window.action.DropItemActionHandler itemAction;
   public com.tlnt.model.item.Item selectedItem;
   public ItemQuantityConfirmCallback(DropItemActionHandler action, Item item) {
      super();
      this.itemAction = action;
      this.selectedItem = item;
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
      } else if (quantity > this.selectedItem.quantity) {
         quantity = this.selectedItem.quantity;
      }

      (GameState.marketSellItem = new Item(this.selectedItem)).quantity = quantity;
      GameMain.instance.getLoginScreen().debugScrollPane.refreshHudState();
   }
}
