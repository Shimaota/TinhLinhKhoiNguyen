package com.tlnt.ui.window.market.panel;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.font.FontFactory;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.GameTextField;
import com.tlnt.ui.widget.TextField;

public final class MarketListingRow$MarketPriceTextField extends GameTextField {
   public com.tlnt.ui.window.market.panel.MarketListingRow.MarketSellPanel backgroundPatch;
   public MarketListingRow$MarketPriceTextField(MarketListingRow.MarketSellPanel sellPanel, String text, RenderContext ctx) {
      super(text, ctx, FontFactory.scaleFont(FontManager.TEXT_FIELD_FONT, 22.0F));
      this.backgroundPatch = sellPanel;
      this.setAlignment(1);
      this.setTextFieldFilter(new TextField.TextFieldFilter.DigitsOnlyFilter());
   }

   @Override
   public final void drawBackground(float x, float y, float width, float height, boolean focused, boolean disabled, float parentAlpha) {
   }

   @Override
   public final void drawText(Batch batch, BitmapFont font, float x, float y) {
      super.drawText(batch, font, x, y - 5.0F);
   }
}
