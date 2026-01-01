package com.tlnt.ui.window.market.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class MarketBrowseTabClickListener extends ClickListener {
   public com.tlnt.ui.window.market.panel.MarketListingRow marketListingRow;
   public MarketBrowseTabClickListener(MarketListingRow listingRow) {
      super();
      this.marketListingRow = listingRow;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.marketListingRow.sellPanel.setVisible(false);
      this.marketListingRow.myListingsPanel.setVisible(true);
   }
}
