package com.tlnt.ui.window.action;

import com.tlnt.core.GameMain;
import com.tlnt.core.GameState;
import com.tlnt.model.item.Item;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class ReturnToCharacterSelectAction extends ItemInfoPopup.ActionConfig {
   public ReturnToCharacterSelectAction(String label) {
      super(label);
   }

   @Override
   public final void onItemAction(Item item) {
      GameState.marketSellItem = null;
      GameMain.instance.getLoginScreen().debugScrollPane.refreshHudState();
   }
}
