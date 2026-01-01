package com.tlnt.ui.window.market.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameMain;
import com.tlnt.model.item.Item;
import com.tlnt.ui.window.action.ItemActionRegistry;
import com.tlnt.ui.window.iteminfo.ItemInfoPopup;

public final class MyListingItemClickListener extends ClickListener {
   public com.tlnt.ui.window.market.panel.MarketListingRow$MarketBrowseListingRow myListingRow;
   public com.tlnt.ui.window.market.panel.MarketListingRow.MarketBrowsePanel myListingsPanel;
   public MyListingItemClickListener(MarketListingRow$MarketBrowseListingRow row, MarketListingRow.MarketBrowsePanel browsePanel) {
      super();
      this.myListingRow = row;
      this.myListingsPanel = browsePanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      ItemInfoPopup popup = GameMain.instance.getLoginScreen().debugScrollPane.findDialog(ItemInfoPopup.class);
      MarketListingRow$MarketBrowseListingRow row = this.myListingRow;
      Item item = this.myListingRow.itemIcon.getItem();
      ItemInfoPopup.ActionConfig[] actions = new ItemInfoPopup.ActionConfig[3];
      actions[0] = ItemActionRegistry.LIST_FOR_SALE_ACTION;
      actions[1] = ItemActionRegistry.BUY_MULTIPLE_ACTION;
      actions[2] = ItemActionRegistry.CLOSE_TOOLTIP_ACTION;
      popup.showItemWithActions(row, item, null, actions);
   }
}
