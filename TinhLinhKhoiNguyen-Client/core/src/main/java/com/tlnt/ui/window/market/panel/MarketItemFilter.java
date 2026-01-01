package com.tlnt.ui.window.market.panel;

import com.tlnt.model.item.Item;
import com.tlnt.ui.window.charinfo.InventoryGridPanel;

public final class MarketItemFilter implements InventoryGridPanel.ItemFilter {
   public com.tlnt.ui.window.market.panel.MarketListingRow marketListingRow;
   public MarketItemFilter(MarketListingRow listingRow) {
      super();
      this.marketListingRow = listingRow;
   }

   @Override
   public final boolean isItemAccepted(Item item) {
      if (!item.locked) {
         return true;
      } else {
         return false;
      }
   }
}
