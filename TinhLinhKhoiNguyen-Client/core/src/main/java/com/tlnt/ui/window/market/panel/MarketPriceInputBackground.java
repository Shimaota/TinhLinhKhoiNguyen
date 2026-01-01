package com.tlnt.ui.window.market.panel;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.BaseTable;

public final class MarketPriceInputBackground extends BaseTable {
   public com.tlnt.ui.window.market.panel.MarketListingRow.MarketSellPanel sellPanel;
   public TextureAsset backgroundTexture;

   public MarketPriceInputBackground(MarketListingRow.MarketSellPanel sellPanel, RenderContext ctx) {
      super(ctx);
      this.sellPanel = sellPanel;
      this.backgroundTexture = TextureAsset.obtain("ui/116");
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawTextureSized(this.backgroundTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.draw(batch, parentAlpha);
   }

}
