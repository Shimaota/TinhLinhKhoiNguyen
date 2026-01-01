package com.tlnt.ui.window.market.panel;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.BaseTable;

public final class MarketSearchBackground extends BaseTable {
   public com.tlnt.ui.window.market.panel.MarketShopPanel marketShopPanel;
   public NinePatchAsset backgroundPatch;

   public MarketSearchBackground(MarketShopPanel shopPanel, RenderContext ctx) {
      super(ctx);
      this.marketShopPanel = shopPanel;
      this.backgroundPatch = NinePatchAsset.obtain("ui/121", 10);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawNinePatch(this.backgroundPatch, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.draw(batch, parentAlpha);
   }

}
