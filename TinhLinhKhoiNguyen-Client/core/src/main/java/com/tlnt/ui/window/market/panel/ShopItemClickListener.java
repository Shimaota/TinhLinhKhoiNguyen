package com.tlnt.ui.window.market.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.model.item.Item;
import com.tlnt.ui.window.action.BuyItemAction;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class ShopItemClickListener extends ClickListener {
   public com.tlnt.ui.window.market.panel.MarketShopPanel marketShopPanel;
   public com.tlnt.ui.window.market.panel.MarketShopPanel.MarketBuyListingRow shopItemRow;
   public ShopItemClickListener(MarketShopPanel.MarketBuyListingRow row, MarketShopPanel shopPanel) {
      super();
      this.shopItemRow = row;
      this.marketShopPanel = shopPanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);
      MarketShopPanel.MarketBuyListingRow row = this.shopItemRow;
      Item item = this.shopItemRow.itemIcon.getItem();
      BuyItemAction defaultAction = ItemActionRegistry.BUY_ACTION;
      ItemInfoPopup.ActionConfig[] actions = new ItemInfoPopup.ActionConfig[2];
      actions[0] = ItemActionRegistry.BUY_ACTION;
      actions[1] = ItemActionRegistry.CLOSE_TOOLTIP_ACTION;
      popup.showItemWithActions(row, item, defaultAction, actions);
   }
}
