package com.tlnt.ui.window.action;

import com.tlnt.core.GameMain;
import com.tlnt.core.GameState;
import com.tlnt.model.item.Item;

public final class MailAttachmentQuantityCallback implements QuantityInputDialog.InputCallback {
   public com.tlnt.ui.window.action.AddMailAttachmentAction itemActionHandler;
   public com.tlnt.model.item.Item itemInfo;
   public MailAttachmentQuantityCallback(AddMailAttachmentAction action, Item item) {
      super();
      this.itemActionHandler = action;
      this.itemInfo = item;
   }

   @Override
   public final void onInputConfirmed(String text) {
      int quantity = 1;

      try {
         quantity = Integer.parseInt(text);
      } catch (Exception e) {
      }

      if (quantity > 0 && quantity <= this.itemInfo.quantity) {
         GameState.mailbox.addComposeAttachment(new Item(this.itemInfo, quantity));
         GameMain.instance.getLoginScreen().debugScrollPane.refreshHudState();
      }
   }
}
