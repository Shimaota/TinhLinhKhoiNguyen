package com.tlnt.ui.window.shop.panel;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.core.audio.FontManager;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.ShapeButton;

public final class ComboPackBuyButton extends ShapeButton {
   public com.tlnt.ui.window.shop.panel.ComboPackPanel.ComboPackRow comboPackRow;
   public TextureAsset buttonTexture;

   public ComboPackBuyButton(ComboPackPanel.ComboPackRow row, RenderContext renderContext) {
      super(renderContext);
      this.comboPackRow = row;
      this.buttonTexture = TextureAsset.obtain("ui/102");
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      if (this.isPressed() || this.comboPackRow.packData == null || this.comboPackRow.packData.purchased || this.comboPackRow.packData.purchasePending) {
         super.renderContext.setBatchColor(Color.GRAY);
      }

      super.renderContext.drawTextureSized(this.buttonTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext
         .drawText(
            FontManager.DEFAULT_FONT,
            "Mua",
            this.getX() + this.getWidth() / 2.0F,
            this.getY() + this.getHeight() / 1.8F,
            Anchor.CENTER,
            22.0F,
            Color.WHITE
         );
      super.renderContext.resetBatchColor();
   }

}
