package com.tlnt.ui.window.weaponcraft;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.ShapeButton;

public final class PrevArrowButton extends ShapeButton {
   public com.tlnt.ui.window.weaponcraft.WeaponCraftWindow weaponCraftWindow;

   public PrevArrowButton(WeaponCraftWindow window, RenderContext ctx) {
      super(ctx);
      this.weaponCraftWindow = window;
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      if (this.isPressed()) {
         super.renderContext.setBatchColor(Color.GRAY);
      }

      super.renderContext
         .drawTextureSized(TextureAsset.obtain("ui/162"), this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT);
      super.renderContext.resetBatchColor();
   }

}
