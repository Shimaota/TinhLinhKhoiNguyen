package com.tlnt.ui.dialog;

import com.badlogic.gdx.graphics.Color;
import com.tlnt.core.GameState;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.NinePatchAsset;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.ui.widget.ShapeButton;

public final class CaptchaButton extends ShapeButton {
   public com.tlnt.ui.dialog.RegisterDialog registerDialog;
   public NinePatchAsset backgroundPatch;

   public CaptchaButton(RegisterDialog registerDialog, RenderContext renderContext) {
      super(renderContext);
      this.registerDialog = registerDialog;
      this.backgroundPatch = NinePatchAsset.obtain("ui/163", 15);
   }

   @Override
   public final void drawBackground() {
      if (this.isPressed()) {
         super.renderContext.setBatchColor(Color.GRAY);
      }

      super.renderContext.drawNinePatch(this.backgroundPatch, this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext
         .drawTextureSized(
            GameState.captchaTexture,
            this.getX() + this.getWidth() / 2.0F,
            this.getY() + this.getHeight() / 2.0F,
            this.getWidth() - 10.0F,
            this.getHeight() - 10.0F,
            Anchor.CENTER
         );
      super.renderContext.resetBatchColor();
   }

}
