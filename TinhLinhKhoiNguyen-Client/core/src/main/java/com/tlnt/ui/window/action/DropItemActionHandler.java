package com.tlnt.ui.window.action;

import com.tlnt.core.GameMain;
import com.tlnt.core.GameState;
import com.tlnt.model.item.Item;
import com.tlnt.ui.widget.TextField;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class DropItemActionHandler extends ItemInfoPopup.ActionConfig {

   public DropItemActionHandler(String label) {
      super(label);
   }

   @Override
   public final void onItemAction(Item item) {
      if (item != null) {
         if (item.locked) {
            GameMain.postFloatingMessage("Không thể chọn vật phẩm khóa!");
         } else {
            if (item.quantity == 1) {
               GameState.marketSellItem = new Item(item);
               GameMain.instance.getLoginScreen().debugScrollPane.refreshHudState();
            } else {
               GameMain.instance
                  .getLoginScreen()
                  .debugScrollPane
                  .findDialog(QuantityInputDialog.class)
                  .openWithPrompt(
                     "Nhập số lượng",
                     new TextField.TextFieldFilter.DigitsOnlyFilter(),
                     "",
                     new ItemQuantityConfirmCallback(this, item)
                  );
            }
         }
      }
   }

}
