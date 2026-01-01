package com.tlnt.ui.window.action;

import com.badlogic.gdx.utils.Array;
import com.tlnt.core.GameMain;
import com.tlnt.core.GameState;
import com.tlnt.model.item.Item;
import com.tlnt.ui.widget.TextField;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class AddTradeItemAction extends ItemInfoPopup.ActionConfig {

   public AddTradeItemAction(String label) {
      super(label);
   }

   @Override
   public final void onItemAction(Item item) {
      if (GameState.tradeSession.myOfferItems.size >= 12) {
         GameMain.postFloatingMessage("Đã hết slot vật phẩm giao dịch!");
      } else if (item.locked) {
         GameMain.postFloatingMessage("Không thể thêm vật phẩm khóa khi giao dịch!");
      } else {
         Array.ArrayIterator iter = GameState.tradeSession.myOfferItems.iterator();

         while (iter.hasNext()) {
            if (((Item)iter.next()).slotIndex == item.slotIndex) {
               GameMain.postFloatingMessage("Đã có vật phẩm này!");
               return;
            }
         }

         if (item.quantity == 1) {
            GameState.tradeSession.myOfferItems.add(new Item(item));
         } else {
            GameMain.instance
               .getLoginScreen()
               .debugScrollPane
               .findDialog(QuantityInputDialog.class)
               .openWithPrompt(
                  "Nhập số lượng", new TextField.TextFieldFilter.DigitsOnlyFilter(), "", new TradeItemQuantityHandler(this, item)
               );
         }

         GameMain.instance.getLoginScreen().debugScrollPane.refreshHudState();
      }
   }

}
