package com.tlnt.ui.window.market.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.tlnt.core.GameState;

public final class FeeTierSelectListener extends ClickListener {
   public com.tlnt.ui.window.market.panel.MarketListingRow$MarketCurrencyButton feeTierButton;
   public com.tlnt.ui.window.market.panel.MarketListingRow.MarketSellPanel sellPanel;
   public FeeTierSelectListener(MarketListingRow$MarketCurrencyButton button, MarketListingRow.MarketSellPanel sellPanel) {
      super();
      this.feeTierButton = button;
      this.sellPanel = sellPanel;
   }

   @Override
   public final void clicked(InputEvent event, float x, float y) {
      GameState.selectedMarketCategory = this.feeTierButton.currencyType;
      this.feeTierButton.buttonSprite.feeValueLabel.setText("Phí giao dịch: " + GameState.selectedMarketCategory.feePercent + "%");
   }
}
