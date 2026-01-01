package com.tlnt.ui.window.marketsell.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.model.item.Item;
import com.tlnt.ui.hud.itemslot.ItemSlotButton;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class MarketSellItemSlotClickListener extends ClickListener {
   public com.tlnt.ui.window.marketsell.panel.MarketSellItemRow.ItemSlotButton itemSlotButton;
   public com.tlnt.ui.window.marketsell.panel.MarketSellItemRow marketSellItemRow;
   public MarketSellItemSlotClickListener(MarketSellItemRow.ItemSlotButton slotButton, MarketSellItemRow row) {
      super();
      this.itemSlotButton = slotButton;
      this.marketSellItemRow = row;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);

      // The detail popup is modal: a click underneath only closes the popup, it does not open another item.
      // After choosing an action in the popup, ignore the next click that falls through to the shop.
      if (popup.shouldBlockShopClick()) {
         event.stop();
         event.handle();
         return;
      }

      ItemSlotButton iconWidget = this.itemSlotButton.itemIconWidget;
      Item item = this.itemSlotButton.item.itemData;
      ItemInfoPopup.ActionConfig[] actions = new ItemInfoPopup.ActionConfig[2];
      actions[0] = ItemActionRegistry.FORGE_EQUIP_HANDLER.withCallbackData(this.itemSlotButton.item);
      actions[1] = ItemActionRegistry.BUY_MANY_ACTION.withCallbackData(this.itemSlotButton.item);
      // The decompiled shop passes null for double-click; purchase is only sent from the popup action button.
      popup.showItemWithActions(iconWidget, item, null, actions);
      event.stop();
      event.handle();
   }
}
