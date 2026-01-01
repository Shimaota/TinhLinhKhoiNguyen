package com.tlnt.ui.window.action;

import com.tlnt.core.GameMain;
import com.tlnt.core.GameState;
import com.tlnt.model.item.Item;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class RemoveTradeItemAction extends ItemInfoPopup.ActionConfig {
   public RemoveTradeItemAction(String label) {
      super(label);
   }

   @Override
   public final void onItemAction(Item item) {
      if (!GameState.tradeSession.myOfferLocked) {
         GameState.tradeSession.myOfferItems.removeValue(item, true);
         GameMain.instance.getLoginScreen().debugScrollPane.refreshHudState();
      }
   }
}
