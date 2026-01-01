package com.tlnt.ui.window.action;

import com.tlnt.core.GameMain;
import com.tlnt.model.item.Item;
import com.tlnt.net.task.ServerPacketSender;
import com.tlnt.ui.widget.TextField;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class UsePvpItemAction extends ItemInfoPopup.ActionConfig {

   public UsePvpItemAction(String label) {
      super(label);
   }

   @Override
   public final void onItemAction(Item item) {
      if (item.quantity == 1) {
         ServerPacketSender.getInstance().sendPvpAction(item.slotIndex, item.quantity);
      } else {
         GameMain.instance
            .getLoginScreen()
            .debugScrollPane
            .findDialog(QuantityInputDialog.class)
            .openWithPrompt(
               "Nhập số lượng", new TextField.TextFieldFilter.DigitsOnlyFilter(), "", new UseItemQuantityCallback(this, item)
            );
      }
   }

}
