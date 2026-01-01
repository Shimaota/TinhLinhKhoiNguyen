package com.tlnt.ui.window.market.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.model.item.Item;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.action.ReturnToCharacterSelectAction;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class MarketItemSelectClickListener extends ClickListener {
   public com.tlnt.ui.window.market.panel.MarketListingRow.MarketSellPanel sellPanel;
   public MarketItemSelectClickListener(MarketListingRow.MarketSellPanel sellPanel) {
      super();
      this.sellPanel = sellPanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);
      ItemSlotButton slotButton = this.sellPanel.itemIconSlot;
      Item item = this.sellPanel.itemIconSlot.getItem();
      ReturnToCharacterSelectAction defaultAction = ItemActionRegistry.RETURN_TO_CHAR_SELECT_ACTION;
      ItemInfoPopup.ActionConfig[] actions = new ItemInfoPopup.ActionConfig[1];
      actions[0] = ItemActionRegistry.RETURN_TO_CHAR_SELECT_ACTION;
      popup.showItemWithActions(slotButton, item, defaultAction, actions);
   }
}
