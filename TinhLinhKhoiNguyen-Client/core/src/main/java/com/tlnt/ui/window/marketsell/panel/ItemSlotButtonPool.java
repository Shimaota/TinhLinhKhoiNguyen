package com.tlnt.ui.window.marketsell.panel;

import com.badlogic.gdx.utils.Pool;

public final class ItemSlotButtonPool extends Pool<MarketSellItemRow.ItemSlotButton> {
   public com.tlnt.ui.window.marketsell.panel.MarketSellItemRow marketSellItemRow;
   public ItemSlotButtonPool(MarketSellItemRow row) {
      super();
      this.marketSellItemRow = row;
   }

   @Override
   public MarketSellItemRow.ItemSlotButton newObject() {
      return (this.marketSellItemRow).new ItemSlotButton(this.marketSellItemRow.renderContext);
   }
}
