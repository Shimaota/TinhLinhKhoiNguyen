package com.tlnt.ui.window.action;

import com.tlnt.core.GameMain;
import com.tlnt.model.consignment.ConsignmentItem;
import com.tlnt.model.item.Item;
import com.tlnt.ui.widget.TextField;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class BuyManyAction extends ItemInfoPopup.ActionConfig {

   public BuyManyAction(String label) {
      super(label);
   }

   @Override
   public final void onItemAction(Item item) {
      if (super.callbackData instanceof ConsignmentItem) {
         ConsignmentItem consignmentItem = (ConsignmentItem)super.callbackData;
         QuantityInputDialog dialog = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(QuantityInputDialog.class);
         dialog.openWithPrompt("Nhập số lượng", new TextField.TextFieldFilter.DigitsOnlyFilter(), "", new BuyQuantityCallback(this, consignmentItem));
      }
   }

}
