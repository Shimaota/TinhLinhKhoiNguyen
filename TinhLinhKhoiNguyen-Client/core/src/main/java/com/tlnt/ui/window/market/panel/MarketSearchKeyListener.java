package com.tlnt.ui.window.market.panel;

import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;

public final class MarketSearchKeyListener extends InputListener {
   public com.tlnt.ui.window.market.panel.MarketShopPanel marketShopPanel;
   public MarketSearchKeyListener(MarketShopPanel shopPanel) {
      super();
      this.marketShopPanel = shopPanel;
   }

   @Override
   public final boolean keyUp(InputEvent event, int keycode) {
      this.marketShopPanel.refresh();
      return super.keyUp(event, keycode);
   }
}
