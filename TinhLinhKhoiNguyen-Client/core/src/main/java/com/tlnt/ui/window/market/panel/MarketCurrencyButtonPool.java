package com.tlnt.ui.window.market.panel;

import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.utils.Pool;

public final class MarketCurrencyButtonPool extends Pool<MarketListingRow$MarketCurrencyButton> {
   public com.tlnt.ui.window.market.panel.MarketListingRow.MarketSellPanel sellPanel;
   public ButtonGroup<Button> buttonGroup;

   public MarketCurrencyButtonPool(MarketListingRow.MarketSellPanel sellPanel) {
      super();
      this.sellPanel = sellPanel;
      this.buttonGroup = new ButtonGroup<>();
   }

   @Override
   public final MarketListingRow$MarketCurrencyButton newObject() {
      MarketListingRow$MarketCurrencyButton button = new MarketListingRow$MarketCurrencyButton(
         this.sellPanel, this.sellPanel.renderContext
      );
      this.buttonGroup.add(button);
      return button;
   }
}
