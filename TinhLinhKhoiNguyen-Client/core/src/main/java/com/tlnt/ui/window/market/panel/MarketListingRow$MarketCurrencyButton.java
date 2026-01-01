package com.tlnt.ui.window.market.panel;

import com.badlogic.gdx.graphics.Color;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.model.market.MarketFeeOption;
import com.tlnt.ui.widget.TextButton;

public final class MarketListingRow$MarketCurrencyButton extends TextButton {
   public com.tlnt.ui.window.market.panel.MarketListingRow.MarketSellPanel buttonSprite;
   public TextureAsset checkedTexture;
   public TextureAsset uncheckedTexture;
   public Color textColor;
   public MarketFeeOption currencyType;

   public MarketListingRow$MarketCurrencyButton(MarketListingRow.MarketSellPanel sellPanel, RenderContext ctx) {
      super("", ctx);
      this.buttonSprite = sellPanel;
      this.checkedTexture = TextureAsset.obtain("ui/118");
      this.uncheckedTexture = TextureAsset.obtain("ui/119");
      this.textColor = new Color(0.54509807F, 0.30588236F, 0.13725491F, 1.0F);
      this.addListener(new FeeTierSelectListener(this, sellPanel));
   }

   public final MarketListingRow$MarketCurrencyButton bindCurrencyType(MarketFeeOption option) {
      this.currencyType = option;
      this.setText(this.currencyType.name);
      return this;
   }

   @Override
   public final void drawBackground() {
      if (this.isPressed()) {
         super.renderContext.setBatchColor(Color.GRAY);
      }

      super.renderContext
         .drawTextureSized(
            this.isChecked() ? this.checkedTexture : this.uncheckedTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT
         );
      super.renderContext.resetBatchColor();
   }

   @Override
   public final void drawLabel() {
      super.renderContext
         .drawText(
            FontManager.PLAIN_FONT,
            super.text,
            this.getX() + this.getWidth() / 2.0F,
            this.getY() + this.getHeight() / 2.0F,
            Anchor.CENTER,
            22.0F,
            this.textColor
         );
   }

}
