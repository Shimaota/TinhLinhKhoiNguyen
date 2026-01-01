package com.tlnt.ui.window.action;

import com.tlnt.core.GameMain;
import com.tlnt.model.item.Item;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.widget.TextField;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class CraftItemAction extends ItemInfoPopup.ActionConfig {

   public CraftItemAction(String label) {
      super(label);
   }

   @Override
   public final void onItemAction(Item item) {
      if (item != null) {
         if (item.quantity == 1) {
            ServerPacketSender.getInstance().sendCraftItem(item.slotIndex, 1);
         } else {
            QuantityInputDialog dialog = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(QuantityInputDialog.class);
            dialog.openWithPrompt(
               "Số lượng", new TextField.TextFieldFilter.DigitsOnlyFilter(), "", new CraftItemQuantityCallback(this, item)
            );
         }
      }
   }

}
