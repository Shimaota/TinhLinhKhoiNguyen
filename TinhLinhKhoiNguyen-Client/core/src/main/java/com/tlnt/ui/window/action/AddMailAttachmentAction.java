package com.tlnt.ui.window.action;

import com.badlogic.gdx.utils.Array;
import com.tlnt.core.GameMain;
import com.tlnt.core.GameState;
import com.tlnt.model.item.Item;
import com.tlnt.ui.widget.TextField;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class AddMailAttachmentAction extends ItemInfoPopup.ActionConfig {

   public AddMailAttachmentAction(String label) {
      super(label);
   }

   @Override
   public final void onItemAction(Item item) {
      if (item != null) {
         if (item.locked) {
            GameMain.postFloatingMessage("Không thể thêm vật phẩm khóa!");
         } else {
            Array.ArrayIterator iter = GameState.mailbox.composeAttachments.iterator();

            while (iter.hasNext()) {
               if (((Item)iter.next()).slotIndex == item.slotIndex) {
                  GameMain.postFloatingMessage("Vật phẩm này đã có!");
                  return;
               }
            }

            if (item.quantity == 1) {
               GameState.mailbox.addComposeAttachment(new Item(item, 1));
               GameMain.instance.getLoginScreen().debugScrollPane.refreshHudState();
            } else {
               GameMain.instance
                  .getLoginScreen()
                  .debugScrollPane
                  .findDialog(QuantityInputDialog.class)
                  .openWithPrompt(
                     "Số lượng",
                     new TextField.TextFieldFilter.DigitsOnlyFilter(),
                     "",
                     new MailAttachmentQuantityCallback(this, item)
                  );
            }
         }
      }
   }

}
