package com.tlnt.ui.window.action;

import com.tlnt.core.GameMain;
import com.tlnt.model.item.Item;
import com.tlnt.ui.window.WindowType;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class OpenItemTooltipAction extends ItemInfoPopup.ActionConfig {
   public OpenItemTooltipAction(String label) {
      super(label);
   }

   @Override
   public final void onItemAction(Item item) {
      GameMain.instance.getLoginScreen().debugScrollPane.closeWindowByType(WindowType.INFO_ITEM_POPUP);
   }
}
