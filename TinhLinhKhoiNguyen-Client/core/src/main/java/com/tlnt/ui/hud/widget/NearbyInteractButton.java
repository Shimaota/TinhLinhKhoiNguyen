package com.tlnt.ui.hud.widget;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.tlnt.engine.WorldConfig;
import com.tlnt.engine.render.Anchor;
import com.tlnt.engine.render.RenderContext;
import com.tlnt.engine.render.TextureAsset;
import com.tlnt.ui.widget.ShapeButton;

public final class NearbyInteractButton extends ShapeButton {

   public NearbyInteractButton(RenderContext renderContext) {
      super(renderContext);
      this.setSize(75.0F, 75.0F);
      if (Gdx.app.getType() != Application.ApplicationType.iOS) {
         this.setPosition((float)(WorldConfig.VIEWPORT_WIDTH - 475), 313.0F, 20);
      } else {
         this.setPosition((float)(WorldConfig.VIEWPORT_WIDTH - 585), 313.0F, 20);
      }
      this.addListener(new LogoutClickListener(this));
   }

   @Override
   public final void draw(Batch batch, float parentAlpha) {
      if (this.isPressed()) {
         super.renderContext.setBatchColor(Color.LIGHT_GRAY);
      }

      super.renderContext
         .drawTextureSized(
            TextureAsset.obtain("ui/39"), this.getX(), this.getY(), this.getWidth(), this.getHeight(), Anchor.BOTTOM_LEFT
         );
      super.renderContext.resetBatchColor();
   }

}
