package com.tlnt.ui.window.market.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public final class MarketConfirmSellClickListener extends ClickListener {
   public com.tlnt.ui.window.market.panel.MarketListingRow.MarketSellPanel sellPanel;
   public MarketConfirmSellClickListener(MarketListingRow.MarketSellPanel sellPanel) {
      super();
      this.sellPanel = sellPanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      this.sellPanel.submitSellListing();
   }
}
