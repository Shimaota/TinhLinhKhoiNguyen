package com.tlnt.ui.window.market.panel;

import com.badlogic.gdx.utils.Pool;

public final class MarketBuyListingRowPool extends Pool<MarketShopPanel.MarketBuyListingRow> {
   public com.tlnt.ui.window.market.panel.MarketShopPanel marketShopPanel;
   public MarketBuyListingRowPool(MarketShopPanel shopPanel) {
      super();
      this.marketShopPanel = shopPanel;
   }

   @Override
   public MarketShopPanel.MarketBuyListingRow newObject() {
      return (this.marketShopPanel).new MarketBuyListingRow(this.marketShopPanel.renderContext);
   }
}
