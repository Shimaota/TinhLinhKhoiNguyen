package com.tlnt.ui.window.marketsell;

import com.badlogic.gdx.utils.Pool;
import com.tlnt.ui.window.marketsell.panel.MarketSellItemRow;

public final class MarketSellItemRowPool extends Pool<MarketSellItemRow> {
   public com.tlnt.ui.window.marketsell.MarketSellWindow window;
   public MarketSellItemRowPool(MarketSellWindow window) {
      super();
      this.window = window;
   }

   @Override
   public MarketSellItemRow newObject() {
      return new com.tlnt.ui.window.marketsell.panel.MarketSellItemRow(this.window.renderContext);
   }
}
