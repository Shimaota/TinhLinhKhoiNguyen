package com.tlnt.ui.window.market.panel;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.CompositeSprite;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.BaseTable;

public final class ListingTabBackground extends BaseTable {
   public com.tlnt.ui.window.market.panel.MarketListingRow marketListingRow;
   public CompositeSprite backgroundSprite;

   public ListingTabBackground(MarketListingRow listingRow, RenderContext ctx) {
      super(ctx);
      this.marketListingRow = listingRow;
      String leftPath = "ui/104";
      String middlePath = "ui/105";
      String rightPath = "ui/106";
      this.backgroundSprite = CompositeSprite.obtain(leftPath, middlePath, rightPath, false);
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      super.renderContext.drawCompositeSprite(this.backgroundSprite, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.draw(batch, parentAlpha);
   }

}
