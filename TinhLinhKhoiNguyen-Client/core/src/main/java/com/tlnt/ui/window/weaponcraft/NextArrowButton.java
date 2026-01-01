package com.tlnt.ui.window.weaponcraft;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.ShapeButton;

public final class NextArrowButton extends ShapeButton {
   public com.tlnt.ui.window.weaponcraft.WeaponCraftWindow weaponCraftWindow;

   public NextArrowButton(WeaponCraftWindow window, RenderContext ctx) {
      super(ctx);
      this.weaponCraftWindow = window;
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      if (this.isPressed()) {
         super.renderContext.setBatchColor(Color.GRAY);
      }

      RenderContext ctx = super.renderContext;
      TextureAsset arrowTexture = TextureAsset.obtain("ui/162");
      float x = this.getX();
      float y = this.getY();
      float width = this.getWidth();
      float height = this.getHeight();
      ctx.drawTextureRotatedFlipped(
         arrowTexture, x, y, width, height, 0.0F, true, false, Anchor.BOTTOM_LEFT
      );
      super.renderContext.resetBatchColor();
   }

}
