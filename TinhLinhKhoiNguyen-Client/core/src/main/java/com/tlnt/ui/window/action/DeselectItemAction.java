package com.tlnt.ui.window.action;

import com.tlnt.core.GameMain;
import com.tlnt.core.GameState;
import com.tlnt.model.item.Item;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class DeselectItemAction extends ItemInfoPopup.ActionConfig {
   public DeselectItemAction(String label) {
      super(label);
   }

   @Override
   public final void onItemAction(Item item) {
      GameState.mailbox.composeAttachments.removeValue(item, true);
      GameMain.instance.getLoginScreen().debugScrollPane.refreshHudState();
   }
}
