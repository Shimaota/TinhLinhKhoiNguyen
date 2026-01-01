package com.tlnt.ui.window.market.panel;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.BaseActor;

public final class MarketSearchRefreshIcon extends BaseActor {
   public com.tlnt.ui.window.market.panel.MarketShopPanel marketShopPanel;

   public MarketSearchRefreshIcon(MarketShopPanel shopPanel, RenderContext ctx) {
      super(ctx);
      this.marketShopPanel = shopPanel;
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext
         .drawTextureSized(
            TextureAsset.obtain("ui/122"), this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT
         );
   }

}
