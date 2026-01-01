package com.tlnt.ui.window.charinfo;

import com.badlogic.gdx.graphics.Color;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.ShapeButton;

public final class DecrementButton extends ShapeButton {
   public com.tlnt.ui.window.charinfo.AmountStepper amountStepper;
   public TextureAsset bgTexture;
   public TextureAsset iconTexture;

   public DecrementButton(AmountStepper stepper, RenderContext renderContext) {
      super(renderContext);
      this.amountStepper = stepper;
      this.bgTexture = TextureAsset.obtain("ui/63");
      this.iconTexture = TextureAsset.obtain("ui/139");
   }

   @Override
   public final void drawBackground() {
      if (this.isPressed()) {
         super.renderContext.setBatchColor(Color.GRAY);
      }

      super.renderContext.drawTextureSized(this.bgTexture, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext.drawTexture(this.iconTexture, this.getX() + this.getWidth() / 2.0F, this.getY() + this.getHeight() / 2.0F, Anchor.CENTER);
      super.renderContext.resetBatchColor();
   }

}
