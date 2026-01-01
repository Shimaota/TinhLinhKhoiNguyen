package com.tlnt.ui.window.market.panel;

import com.badlogic.gdx.utils.Pool;

public final class MarketBrowseRowPool extends Pool<MarketListingRow$MarketBrowseListingRow> {
   public com.tlnt.ui.window.market.panel.MarketListingRow.MarketBrowsePanel browsePanel;
   public MarketBrowseRowPool(MarketListingRow.MarketBrowsePanel browsePanel) {
      super();
      this.browsePanel = browsePanel;
   }

   @Override
   public MarketListingRow$MarketBrowseListingRow newObject() {
      return new MarketListingRow$MarketBrowseListingRow(this.browsePanel, this.browsePanel.renderContext);
   }
}
